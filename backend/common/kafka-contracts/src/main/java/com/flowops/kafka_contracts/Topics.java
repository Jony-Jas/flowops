package com.flowops.kafka_contracts;

/**
 * Centralized topic names, generated from topics.yml.
 */
public final class Topics {

    private Topics() {}

    // Execution lifecycle
    public static final String EXECUTION_COMMANDS = "execution.commands";
    public static final String EXECUTION_STATUS = "execution.status";
}
