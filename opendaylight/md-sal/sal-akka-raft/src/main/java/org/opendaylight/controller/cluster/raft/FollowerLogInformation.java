/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft;

/**
 * The state of the followers log as known by the Leader
 */
public interface FollowerLogInformation {

    /**
     * Increment the value of the nextIndex
     * @return
     */
    long incrNextIndex();

    /**
     * Decrement the value of the nextIndex
     * @return
     */
    long decrNextIndex();

    /**
     *
     * @param nextIndex
     */
    void setNextIndex(long nextIndex);

    /**
     * Increment the value of the matchIndex
     * @return
     */
    long incrMatchIndex();

    void setMatchIndex(long matchIndex);

    /**
     * The identifier of the follower
     * This could simply be the url of the remote actor
     */
    String getId();

    /**
     * for each server, index of the next log entry
     * to send to that server (initialized to leader
     *    last log index + 1)
     */
    long getNextIndex();

    /**
     * for each server, index of highest log entry
     * known to be replicated on server
     *    (initialized to 0, increases monotonically)
     */
    long getMatchIndex();

    /**
     * Checks if the follower is active by comparing the last updated with the duration
     * @return boolean
     */
    boolean isFollowerActive();

    /**
     * restarts the timeout clock of the follower
     */
    void markFollowerActive();

    /**
     * This will stop the timeout clock
     */
    void markFollowerInActive();
}
