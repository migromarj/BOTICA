package com.botica.utils.bot;

import lombok.Getter;

/**
 * This class contains the RabbitMq configuration for a bot.
 */
@Getter
public class BotRabbitConfig {

    private String botType;         // The type of the bot.
    private String keyToPublish;    // The binding key for publishing messages.
    private String orderToPublish;  // The order to be sent in the message.

    /**
     * Constructor for the BotRabbitConfig class.
     *
     * @param botType        The type of the bot.
     * @param order          The order associated with the bot.
     * @param keyToPublish   The binding key for publishing messages.
     * @param orderToPublish The order to be sent in the message.
     */
    public BotRabbitConfig(String botType, String keyToPublish, String orderToPublish) {
        this.botType = botType;
        this.keyToPublish = keyToPublish;
        this.orderToPublish = orderToPublish;
    }
}

