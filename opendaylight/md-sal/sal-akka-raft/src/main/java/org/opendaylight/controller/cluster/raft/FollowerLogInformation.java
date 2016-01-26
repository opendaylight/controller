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
     *
     * @return the new value of nextIndex
     */
    long incrNextIndex();

    /**
     * Decrement the value of the nextIndex
     *
     * @return the new value of nextIndex
     */
    long decrNextIndex();

    /**
     * Sets the index of the next log entry for this follower.
     *
     * @param nextIndex
     * @return true if the new index differed from the current index and the current index was updated, false
     *              otherwise.
     */
    boolean setNextIndex(long nextIndex);

    /**
     * Increment the value of the matchIndex
     *
     * @return the new value of matchIndex
     */
    long incrMatchIndex();

    /**
     * Sets the index of the highest log entry for this follower.
     *
     * @param matchIndex
     * @return true if the new index differed from the current index and the current index was updated, false
     *              otherwise.
     */
    boolean setMatchIndex(long matchIndex);

    /**
     *
     * @return the identifier of the follower. This could simply be the url of the remote actor.
     */
    String getId();

    /**
     * @return index of the next log entry to send to that server (initialized to leader last log index + 1)
     */
    long getNextIndex();

    /**
     * @return index of highest log entry known to be replicated on server (initialized to 0, increases monotonically)
     */
    long getMatchIndex();

    /**
     * Checks if the follower is active by comparing the last updated with the duration
     *
     * @return true if follower is active, false otherwise
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


    /**
     * This will return the active time of follower, since it was last reset
     *
     * @return time in milliseconds since the last activity from the follower
     */
    long timeSinceLastActivity();

    /**
     * This method checks if it is ok to replicate
     *
     * @return true if it is ok to replicate, false otherwise
     */
    boolean okToReplicate();

    /**
     * @return the payload data version of the follower.
     */
    short getPayloadVersion();

    /**
     * Sets the payload data version of the follower.
     */
    void setPayloadVersion(short payloadVersion);

    /**
     * @return the raft version of the follower.
     */
    short getRaftVersion();

    /**
     * Sets the raft version of the follower.
     */
    void setRaftVersion(short payloadVersion);
}
