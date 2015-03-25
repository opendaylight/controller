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

/**
 * TransactionRateLimitingCallback computes the new transaction rate limit on the successful completion of a
 * transaction
 */
public class TransactionRateLimitingCallback implements OperationCallback{

    private static final Logger LOG = LoggerFactory.getLogger(TransactionRateLimitingCallback.class);
    private static final String COMMIT = "commit";

    private final Timer commitTimer;
    private final ActorContext actorContext;
    private Timer.Context timerContext;

    TransactionRateLimitingCallback(ActorContext actorContext){
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

        double newRateLimit = calculateNewRateLimit(commitTimer, actorContext.getDatastoreContext());

        LOG.debug("Data Store {} commit rateLimit adjusted to {}", actorContext.getDataStoreType(), newRateLimit);

        actorContext.setTxCreationLimit(newRateLimit);
    }

    @Override
    public void failure() {
        // This would mean we couldn't get a transaction completed in 30 seconds which is
        // the default transaction commit timeout. Using the timeout information to figure out the rate limit is
        // not going to be useful - so we leave it as it is
    }

    private static double calculateNewRateLimit(Timer commitTimer, DatastoreContext context) {
        Snapshot timerSnapshot = commitTimer.getSnapshot();
        double newRateLimit = 0;

        long commitTimeoutInSeconds = context.getShardTransactionCommitTimeoutInSeconds();
        long commitTimeoutInNanos = TimeUnit.SECONDS.toNanos(commitTimeoutInSeconds);

        // Find the time that it takes for transactions to get executed in every 10th percentile
        // Compute the rate limit for that percentile and sum it up
        for(int i=1;i<=10;i++){
            // Get the amount of time transactions take in the i*10th percentile
            double percentileTimeInNanos = timerSnapshot.getValue(i * 0.1D);

            if(percentileTimeInNanos > 0) {
                // Figure out the rate limit for the i*10th percentile in nanos
                double percentileRateLimit = (commitTimeoutInNanos / percentileTimeInNanos);

                // Add the percentileRateLimit to the total rate limit
                newRateLimit += percentileRateLimit;
            }
        }

        // Compute the rate limit per second
        return newRateLimit/(commitTimeoutInSeconds*10);
    }

    public static void adjustNewRateLimitForUnusedTransaction(ActorContext actorContext) {
        // Unused transactions in one data store can artificially limit the rate for other data stores
        // if the first data store's rate is still at a lower initial rate since the front-end creates
        // transactions in each data store up-front even though the client may not actually submit changes.
        // So we may have to adjust the rate for data stores with unused transactions.

        // First calculate the current rate for the data store. If it's 0 then there have been no
        // actual transactions committed to the data store.

        double newRateLimit = calculateNewRateLimit(actorContext.getOperationTimer(COMMIT),
                actorContext.getDatastoreContext());
        if(newRateLimit == 0.0) {
            // Since we have no rate data for unused Tx's data store, adjust to the rate from another
            // data store that does have rate data.
            for(String datastoreType: DatastoreContext.getGlobalDatastoreTypes()) {
                newRateLimit = calculateNewRateLimit(actorContext.getOperationTimer(datastoreType, COMMIT),
                        actorContext.getDatastoreContext());
                if(newRateLimit > 0.0) {
                    LOG.debug("On unused Tx - data Store {} commit rateLimit adjusted to {}",
                            actorContext.getDataStoreType(), newRateLimit);

                    actorContext.setTxCreationLimit(newRateLimit);
                    break;
                }
            }
        }
    }
}