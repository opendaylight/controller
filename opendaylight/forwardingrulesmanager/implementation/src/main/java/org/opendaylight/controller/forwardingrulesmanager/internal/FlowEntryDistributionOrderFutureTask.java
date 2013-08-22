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
    }

    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        return false;
    }

    @Override
    public Status get() throws InterruptedException, ExecutionException {
        // If i'm done lets return the status as many times as caller wants
        if (this.waitingLatch.getCount() == 0L) {
            return retStatus;
        }

        // Wait till someone signal that we are done
        this.waitingLatch.await();

        // Return the known status
        return retStatus;
    }

    @Override
    public Status get(long timeout, TimeUnit unit) throws InterruptedException,
            ExecutionException, TimeoutException {
        // If i'm done lets return the status as many times as caller wants
        if (this.waitingLatch.getCount() == 0L) {
            return retStatus;
        }

        // Wait till someone signal that we are done
        this.waitingLatch.await(timeout, unit);

        // Return the known status, could also be null if didn't return
        return retStatus;
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
        if (order != this.order) {
            // Weird we got a call for an order we didn't make
            return;
        }
        this.retStatus = retStatus;
        // Now we are not waiting any longer
        this.waitingLatch.countDown();
    }
}
