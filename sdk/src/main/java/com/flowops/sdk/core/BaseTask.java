package com.flowops.sdk.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An abstract base class for tasks providing default logging functionality.
 */
public abstract class BaseTask implements Task {
    /**
     * The logger instance for this task.
     */
    protected final Logger logger;

    /**
     * Constructor for BaseTask.
     */
    public BaseTask() {
        this.logger = LoggerFactory.getLogger(this.getClass());
    }

    /**
     * Constructor for BaseTask with a custom logger.
     * @param logger the logger to use
     */
    public BaseTask(Logger logger) {
        this.logger = logger;
    }


    /**
     * Starts the task.
     */
    @Override
    public void start() {
        logger.info("Task started.");
    }


    /**
     * Stops the task.
     */
    @Override
    public void stop() {
        logger.info("Task stopped.");
    }


    /**
     * Pauses the task.
     */
    @Override
    public void pause() {
        logger.info("Task paused.");
    }


    /**
     * Resumes the task.
     */
    @Override
    public void resume() {
        logger.info("Task resumed.");
    }
}
