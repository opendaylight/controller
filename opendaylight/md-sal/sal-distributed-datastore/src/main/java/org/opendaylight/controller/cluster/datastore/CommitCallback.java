/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.datastore;

import com.codahale.metrics.Snapshot;
import com.codahale.metrics.Timer;
import com.google.common.base.Preconditions;
import java.util.concurrent.TimeUnit;
import org.opendaylight.controller.cluster.datastore.utils.ActorContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CommitCallback implements OperationCallback{

    private static final Logger LOG = LoggerFactory.getLogger(CommitCallback.class);
    private static final String COMMIT = "commit";

    private final Timer commitTimer;
    private final ActorContext actorContext;
    private Timer.Context timerContext;

    CommitCallback(ActorContext actorContext){
        this.actorContext = actorContext;
        commitTimer = actorContext.getOperationTimer(COMMIT);
    }

    @Override
    public void run() {
        timerContext = commitTimer.time();
    }

    @Override
    public void success() {
        Preconditions.checkState(timerContext != null, "Call run before success");
        timerContext.stop();

        Snapshot timerSnapshot = commitTimer.getSnapshot();
        double newRateLimit = 0;

        long commitTimeoutInSeconds = actorContext.getDatastoreContext()
                .getShardTransactionCommitTimeoutInSeconds();
        long commitTimeoutInNanos = TimeUnit.SECONDS.toNanos(commitTimeoutInSeconds);

        for(int i=1;i<11;i++){
            // Get the amount of time transactions take in the i*10th percentile
            double percentileTimeInNanos = timerSnapshot.getValue(i * 0.1);

            // Figure out the rate limit for the i*10th percentile in nanos
            double percentileRateLimit = ((double) commitTimeoutInNanos / percentileTimeInNanos) / 10;

            // Add the percentileRateLimit to the
            newRateLimit += percentileRateLimit;
        }

        // get the new rate limit per second
        newRateLimit = newRateLimit/commitTimeoutInSeconds;

        LOG.debug("Data Store {} commit rateLimit adjusted to {}", actorContext.getDataStoreType(), newRateLimit);

        actorContext.setTxCreationLimit(newRateLimit);
    }

    @Override
    public void failure() {
        // This would mean we couldn't get a transaction completed in 30 seconds which is
        // the default transaction commit timeout. Using the timeout information to figure out the rate limit is
        // not going to be useful - so we leave it as it is
    }
}