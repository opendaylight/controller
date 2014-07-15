/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.raft;

import java.util.concurrent.atomic.AtomicLong;

/**
 * The state of the followers log as known by the Leader
 */
public interface FollowerLogInformation {

    /**
     * Increment the value of the nextIndex
     * @return
     */
    public long incrNextIndex();

    /**
     * Decrement the value of the nextIndex
     * @return
     */
    public long decrNextIndex();

    /**
     *
     * @param nextIndex
     */
    void setNextIndex(long nextIndex);

    /**
     * Increment the value of the matchIndex
     * @return
     */
    public long incrMatchIndex();

    public void setMatchIndex(long matchIndex);

    /**
     * The identifier of the follower
     * This could simply be the url of the remote actor
     */
    public String getId();

    /**
     * for each server, index of the next log entry
     * to send to that server (initialized to leader
     *    last log index + 1)
     */
    public AtomicLong getNextIndex();

    /**
     * for each server, index of highest log entry
     * known to be replicated on server
     *    (initialized to 0, increases monotonically)
     */
    public AtomicLong getMatchIndex();


}
