/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

/**
 * Class which will monitor the completion of a FlowEntryDistributionOrder it
 * implements a Future interface so it can be inspected by who is waiting for
 * it.
 */
package org.opendaylight.controller.forwardingrulesmanager.internal;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.opendaylight.controller.forwardingrulesmanager.implementation.data.FlowEntryDistributionOrder;
import org.opendaylight.controller.sal.utils.Status;
import org.opendaylight.controller.sal.utils.StatusCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class which will monitor the completion of a FlowEntryDistributionOrder it
 * implements a Future interface so it can be inspected by who is waiting for
 * it.
 */
final class FlowEntryDistributionOrderFutureTask implements Future<Status> {
    private final FlowEntryDistributionOrder order;
    private boolean amICancelled;
    private CountDownLatch waitingLatch;
    private Status retStatus;
    private static final Logger logger = LoggerFactory.getLogger(FlowEntryDistributionOrderFutureTask.class);
    // Don't wait forever to program, rather timeout if there are issues, and
    // log an error
    private long timeout;
    private static final Long DEFAULTTIMEOUT = 30000L;

    /**
     * @param order
     *            for which we are monitoring the execution
     */
    FlowEntryDistributionOrderFutureTask(FlowEntryDistributionOrder order) {
        // Order being monitored
        this.order = order;
        this.amICancelled = false;
        // We need to wait for one completion to happen
        this.waitingLatch = new CountDownLatch(1);
        // No return status yet!
        this.retStatus = new Status(StatusCode.UNDEFINED);
        // Set the timeout
        String strTimeout = System.getProperty("FlowEntryDistributionOrderFutureTask.timeout",
                                               DEFAULTTIMEOUT.toString());
        try {
            timeout = Long.parseLong(strTimeout);
        } catch (Exception e) {
            timeout = DEFAULTTIMEOUT;
        }
    }

    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        if (this.waitingLatch.getCount() != 0L) {
            this.retStatus = new Status(StatusCode.GONE);
            this.waitingLatch.countDown();
            logger.trace("Cancelled the workOrder");
            return true;
        }
        return false;
    }

    @Override
    public Status get() throws InterruptedException, ExecutionException {
        boolean didFinish = false;
        logger.trace("Getting status for order {}", this.order);
        // If i'm done lets return the status as many times as caller wants
        if (this.waitingLatch.getCount() == 0L) {
            logger.trace("get returns the status without waiting");
            return retStatus;
        }

        logger.trace("Start waiting for status to come back");
        // Wait till someone signal that we are done
        didFinish = this.waitingLatch.await(this.timeout, TimeUnit.MILLISECONDS);

        if (didFinish) {
            logger.trace("Waiting for the status of order {} is over, returning it", this.order);
            // Return the known status
            return retStatus;
        } else {
            logger.error("Timing out, the workStatus for order {} has not come back in time!, it's hashcode is {}",
                    this.order, this.order.hashCode());
            return new Status(StatusCode.TIMEOUT);
        }
    }

    @Override
    public Status get(long timeout, TimeUnit unit) throws InterruptedException,
            ExecutionException, TimeoutException {
        boolean didFinish = false;
        logger.trace("Getting status for order {}", this.order);
        // If i'm done lets return the status as many times as caller wants
        if (this.waitingLatch.getCount() == 0L) {
            logger.trace("get returns the status without waiting");
            return retStatus;
        }

        logger.trace("Start waiting for status to come back");
        // Wait till someone signal that we are done
        didFinish = this.waitingLatch.await(timeout, unit);

        if (didFinish) {
            logger.trace("Waiting for the status is over, returning it");
            // Return the known status, could also be null if didn't return
            return retStatus;
        } else {
            // No need to bark here as long as this routine could indeed
            // timeout
            logger.trace("Timing out, the workStatus for order {} has not come back in time!", this.order);
            return new Status(StatusCode.TIMEOUT);
        }
    }

    @Override
    public boolean isCancelled() {
        return this.amICancelled;
    }

    @Override
    public boolean isDone() {
        return (this.waitingLatch.getCount() == 0L);
    }

    /**
     * Used by the thread that gets back the status for the order so can unblock
     * an eventual caller waiting on the result to comes back
     *
     * @param order
     * @param retStatus
     */
    void gotStatus(FlowEntryDistributionOrder order, Status retStatus) {
        logger.trace("Got status for order:{} \n Status:{}", order, retStatus);
        if (!order.equals(this.order)) {
            logger.error("Didn't get a result for an order we did issue order expected:{}, order received:{}",
                    this.order, order);
            // Weird we got a call for an order we didn't make
            return;
        }
        this.retStatus = retStatus;
        // Now we are not waiting any longer
        this.waitingLatch.countDown();
        logger.trace("Unlocked the Future");
    }

    /**
     * Getter for the workOrder for which the order is waiting for
     * @return the order
     */
    public FlowEntryDistributionOrder getOrder() {
        return order;
    }
}
