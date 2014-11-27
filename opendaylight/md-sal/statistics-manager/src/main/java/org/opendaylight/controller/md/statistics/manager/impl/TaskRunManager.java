package org.opendaylight.controller.md.statistics.manager.impl;

/**
 * Created by Martin Bobak mbobak@cisco.com on 11/27/14.
 */
public interface TaskRunManager {

    /**
     * Method that puts its implementation thread to sleep.
     */
    void sleep();

    /**
     * Method that wakes up its implementation thread.
     */
    void wakeUp();


}
