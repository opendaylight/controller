/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft;

import scala.concurrent.duration.FiniteDuration;

import java.util.concurrent.TimeUnit;

/**
 * Default implementation of the ConfigParams
 *
 * If no implementation is provided for ConfigParams, then this will be used.
 */
public class DefaultConfigParamsImpl implements ConfigParams {

    private static final int SNAPSHOT_BATCH_COUNT = 100000;

    /**
     * The maximum election time variance
     */
    private static final int ELECTION_TIME_MAX_VARIANCE = 100;


    /**
     * The interval at which a heart beat message will be sent to the remote
     * RaftActor
     * <p/>
     * Since this is set to 100 milliseconds the Election timeout should be
     * at least 200 milliseconds
     */
    public static final FiniteDuration HEART_BEAT_INTERVAL =
        new FiniteDuration(100, TimeUnit.MILLISECONDS);


    @Override
    public long getSnapshotBatchCount() {
        return SNAPSHOT_BATCH_COUNT;
    }

    @Override
    public FiniteDuration getHeartBeatInterval() {
        return HEART_BEAT_INTERVAL;
    }


    @Override
    public FiniteDuration getElectionTimeOutInterval() {
        // returns 2 times the heart beat interval
        return getHeartBeatInterval().$times(2);
    }

    @Override
    public int getElectionTimeVariance() {
        return ELECTION_TIME_MAX_VARIANCE;
    }
}
