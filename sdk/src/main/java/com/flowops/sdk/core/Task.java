package com.flowops.sdk.core;

/**
 * A Task is a unit of work that can be started, stopped, paused and resumed.
 */
public interface Task {
    /**
     * Start the task.
     */
    public void start();

    /**
     * Stop the task.
     */
    public void stop();

    /**
     * Pause the task.
     */
    public void pause();

    /**
     * Resume the task.
     */
    public void resume();
}
