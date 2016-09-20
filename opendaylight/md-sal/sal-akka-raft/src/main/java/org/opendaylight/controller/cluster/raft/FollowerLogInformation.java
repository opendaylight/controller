/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft;

import com.google.common.annotations.VisibleForTesting;

/**
 * The state of the followers log as known by the Leader.
 */
public interface FollowerLogInformation {

    /**
     * Increments the value of the follower's next index.
     *
     * @return the new value of nextIndex.
     */
    long incrNextIndex();

    /**
     * Decrements the value of the follower's next index.
     *
     * @return the new value of nextIndex,
     */
    long decrNextIndex();

    /**
     * Sets the index of the follower's next log entry.
     *
     * @param nextIndex the new index.
     * @return true if the new index differed from the current index and the current index was updated, false
     *              otherwise.
     */
    boolean setNextIndex(long nextIndex);

    /**
     * Increments the value of the follower's match index.
     *
     * @return the new value of matchIndex.
     */
    long incrMatchIndex();

    /**
     * Sets the index of the follower's highest log entry.
     *
     * @param matchIndex the new index.
     * @return true if the new index differed from the current index and the current index was updated, false
     *              otherwise.
     */
    boolean setMatchIndex(long matchIndex);

    /**
     * Returns the identifier of the follower.
     *
     * @return the identifier of the follower.
     */
    String getId();

    /**
     * Returns the index of the next log entry to send to the follower.
     *
     * @return index of the follower's next log entry.
     */
    long getNextIndex();

    /**
     * Returns the index of highest log entry known to be replicated on the follower.
     *
     * @return the index of highest log entry.
     */
    long getMatchIndex();

    /**
     * Checks if the follower is active by comparing the time of the last activity with the election time out. The
     * follower is active if some activity has occurred for the follower within the election time out interval.
     *
     * @return true if follower is active, false otherwise.
     */
    boolean isFollowerActive();

    /**
     * Marks the follower as active. This should be called when some activity has occurred for the follower.
     */
    void markFollowerActive();

    /**
     * Marks the follower as inactive. This should only be called from unit tests.
     */
    @VisibleForTesting
    void markFollowerInActive();


    /**
     * Returns the time since the last activity occurred for the follower.
     *
     * @return time in milliseconds since the last activity from the follower.
     */
    long timeSinceLastActivity();

    /**
     * This method checks if the next replicate message can be sent to the follower. This is an optimization to avoid
     * sending duplicate message too frequently if the last replicate message was sent and no reply has been received
     * yet within the current heart beat interval
     *
     * @return true if it is ok to replicate, false otherwise
     */
    boolean okToReplicate();

    /**
     * Returns the log entry payload data version of the follower.
     *
     * @return the payload data version.
     */
    short getPayloadVersion();

    /**
     * Sets the payload data version of the follower.
     *
     * @param payloadVersion the payload data version.
     */
    void setPayloadVersion(short payloadVersion);

    /**
     * Returns the the raft version of the follower.
     *
     * @return the raft version of the follower.
     */
    short getRaftVersion();

    /**
     * Sets the raft version of the follower.
     *
     * @param raftVersion the raft version.
     */
    void setRaftVersion(short raftVersion);
}
