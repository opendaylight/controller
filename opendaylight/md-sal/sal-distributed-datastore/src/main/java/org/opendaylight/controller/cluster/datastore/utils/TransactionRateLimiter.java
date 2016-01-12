/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.datastore.utils;

import com.codahale.metrics.Snapshot;
import com.codahale.metrics.Timer;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.util.concurrent.RateLimiter;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import org.opendaylight.controller.cluster.datastore.DatastoreContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TransactionRateLimiter {
    private static final Logger LOG = LoggerFactory.getLogger(TransactionRateLimiter.class);

    private final ActorContext actorContext;
    private final long commitTimeoutInSeconds;
    private final String dataStoreName;
    private final RateLimiter txRateLimiter;
    private final AtomicLong acquireCount = new AtomicLong();

    private volatile long pollOnCount = 1;

    public TransactionRateLimiter(ActorContext actorContext){
        this.actorContext = actorContext;
        this.commitTimeoutInSeconds = actorContext.getDatastoreContext().getShardTransactionCommitTimeoutInSeconds();
        this.dataStoreName = actorContext.getDataStoreName();
        this.txRateLimiter = RateLimiter.create(actorContext.getDatastoreContext().getTransactionCreationInitialRateLimit());
    }

    public void acquire(){
        adjustRateLimit();
        txRateLimiter.acquire();
    }

    private void adjustRateLimit() {
        final long count = acquireCount.incrementAndGet();
        if(count >= pollOnCount) {
            final Timer commitTimer = actorContext.getOperationTimer(ActorContext.COMMIT);
            double newRateLimit = calculateNewRateLimit(commitTimer, commitTimeoutInSeconds);

            if (newRateLimit < 1.0) {
                newRateLimit = getRateLimitFromOtherDataStores();
            }

            if (newRateLimit >= 1.0) {
                txRateLimiter.setRate(newRateLimit);
                pollOnCount = count + ((long) newRateLimit/2);
            }
        }
    }

    public double getTxCreationLimit(){
        return txRateLimiter.getRate();
    }

    private double getRateLimitFromOtherDataStores(){
        // Since we have no rate data for unused Tx's data store, adjust to the rate from another
        // data store that does have rate data.
        for(String name: DatastoreContext.getGlobalDatastoreNames()) {
            if(name.equals(this.dataStoreName)) {
                continue;
            }

            double newRateLimit = calculateNewRateLimit(actorContext.getOperationTimer(name, ActorContext.COMMIT),
                    this.commitTimeoutInSeconds);
            if(newRateLimit > 0.0) {
                LOG.debug("On unused Tx - data Store {} commit rateLimit adjusted to {}",
                        this.dataStoreName, newRateLimit);

                return newRateLimit;
            }
        }

        return -1.0D;
    }

    private static double calculateNewRateLimit(Timer commitTimer, long commitTimeoutInSeconds) {
        if(commitTimer == null) {
            // This can happen in unit tests.
            return 0;
        }

        Snapshot timerSnapshot = commitTimer.getSnapshot();
        double newRateLimit = 0;

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

    @VisibleForTesting
    long getPollOnCount() {
        return pollOnCount;
    }

    @VisibleForTesting
    void setPollOnCount(long value){
        pollOnCount = value;
    }

    @VisibleForTesting
    void setAcquireCount(long value){
        acquireCount.set(value);
    }

}
