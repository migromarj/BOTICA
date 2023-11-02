package com.botica.generators;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.Collection;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONObject;

import com.botica.interfaces.TestReportGeneratorInterface;
import com.botica.utils.RESTestUtil;
import com.botica.utils.RabbitCommunicator;

import es.us.isa.restest.coverage.CoverageGatherer;
import es.us.isa.restest.coverage.CoverageMeter;
import es.us.isa.restest.reporting.AllureReportManager;
import es.us.isa.restest.reporting.StatsReportManager;
import es.us.isa.restest.runners.RESTestLoader;
import es.us.isa.restest.specification.OpenAPISpecification;
import es.us.isa.restest.testcases.TestCase;
import es.us.isa.restest.util.RESTestException;

public class TestReportGenerator implements TestReportGeneratorInterface {

    private String propertyFilePath;
    private String testCasesPath;
    private String keyToPublish;
    private String orderToPublish;
    private RabbitCommunicator rabbitCommunicator;

    private static final Logger logger = LogManager.getLogger(TestReportGenerator.class);
    
    public TestReportGenerator(String propertyFilePath, String testCasesPath, String keyToPublish, String orderToPublish) {
        this.propertyFilePath = propertyFilePath;
        this.testCasesPath = testCasesPath;
        this.keyToPublish = keyToPublish;
        this.orderToPublish = orderToPublish;
        this.rabbitCommunicator = new RabbitCommunicator(this.keyToPublish, logger);
    }

    @Override
    public void generate() {
        
        Collection<TestCase> testCases = new ArrayList<>();

        try (FileInputStream fis = new FileInputStream(testCasesPath);
                ObjectInputStream ois = new ObjectInputStream(fis)) {
            readTestCasesFromObjectStream(ois, testCases);
        } catch (IOException e) {
            logger.error("Error writing test cases to file: {}", e.getMessage());
        }
        
        String experimentName = RESTestUtil.readProperty(propertyFilePath, "experiment.name");
        RESTestLoader loader = new RESTestLoader(propertyFilePath);
        try{
            loader.createGenerator(); //TODO: FIX (It is necessary to assign value to spec property in the Loader class)
        }catch(RESTestException e){
            logger.error("Error creating generator: {}", e.getMessage());
        }

        AllureReportManager allureReportManager = loader.createAllureReportManager();
        StatsReportManager statsReportManager = createStatsReportManager(propertyFilePath);
        
        allureReportManager.generateReport();
        statsReportManager.setTestCases(testCases);
        statsReportManager.generateReport(experimentName, true);


        JSONObject message = new JSONObject();
        message.put("order", orderToPublish);

        rabbitCommunicator.sendMessage(message.toString());
    }

    private Collection<TestCase> readTestCasesFromObjectStream(ObjectInputStream ois, Collection<TestCase> testCases) throws IOException {
        try {
            Object obj = ois.readObject();
            if (obj instanceof Collection<?>) {
                Collection<?> aux = (Collection<?>) obj;
                // TODO: Check how to improve the performance of this loop
                for (Object o : aux) {
                    if (o instanceof TestCase) {
                        testCases.add((TestCase) o);
                    }
                }
            }
            return testCases;
        } catch (ClassNotFoundException e) {
            logger.error("Error reading test cases from file: {}", e.getMessage());
            return testCases;
        }
    }

    //TODO: Change (Own definition of createStatsReportManager)
    private static StatsReportManager createStatsReportManager(String propertyFilePath) {

        String experimentName = RESTestUtil.readProperty(propertyFilePath, "experiment.name");
        String testDataDir = RESTestUtil.readProperty(propertyFilePath, "data.tests.dir") + "/" + experimentName;
        String coverageDataDir = RESTestUtil.readProperty(propertyFilePath, "data.coverage.dir") + "/" + experimentName;
        boolean enableCSVStats = Boolean.parseBoolean(RESTestUtil.readProperty(propertyFilePath, "stats.csv"));
        boolean enableInputCoverage = Boolean.parseBoolean(RESTestUtil.readProperty(propertyFilePath, "coverage.input"));
        boolean enableOutputCoverage = Boolean.parseBoolean(RESTestUtil.readProperty(propertyFilePath, "coverage.output"));
        String OAISpecPath = RESTestUtil.readProperty(propertyFilePath, "oas.path");

        OpenAPISpecification spec = new OpenAPISpecification(OAISpecPath);

		CoverageMeter coverageMeter = new CoverageMeter(new CoverageGatherer(spec));

		return new StatsReportManager(testDataDir, coverageDataDir, enableCSVStats, enableInputCoverage,
					enableOutputCoverage, coverageMeter);
	}

}