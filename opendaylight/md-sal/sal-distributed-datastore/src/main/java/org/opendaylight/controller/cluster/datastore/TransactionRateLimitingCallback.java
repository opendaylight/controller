/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.datastore;

import com.codahale.metrics.Timer;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.common.base.Ticker;
import java.util.concurrent.TimeUnit;
import org.opendaylight.controller.cluster.datastore.utils.ActorContext;

/**
 * TransactionRateLimitingCallback computes the new transaction rate limit on the successful completion of a
 * transaction
 */
public class TransactionRateLimitingCallback implements OperationCallback{
    private static Ticker TICKER = Ticker.systemTicker();

    private enum State {
        STOPPED,
        RUNNING,
        PAUSED
    }

    private final Timer commitTimer;
    private long startTime;
    private long elapsedTime;
    private volatile State state = State.STOPPED;

    TransactionRateLimitingCallback(ActorContext actorContext){
        commitTimer = actorContext.getOperationTimer(ActorContext.COMMIT);
    }

    @Override
    public void run() {
        Preconditions.checkState(state == State.STOPPED, "state is not STOPPED");
        resume();
    }

    @Override
    public void pause() {
        if(state == State.RUNNING) {
            elapsedTime += TICKER.read() - startTime;
            state = State.PAUSED;
        }
    }

    @Override
    public void resume() {
        if(state != State.RUNNING) {
            startTime = TICKER.read();
            state = State.RUNNING;
        }
    }

    @Override
    public void success() {
        Preconditions.checkState(state != State.STOPPED, "state is STOPPED");
        pause();
        commitTimer.update(elapsedTime, TimeUnit.NANOSECONDS);
        state = State.STOPPED;
    }

    @Override
    public void failure() {
        // This would mean we couldn't get a transaction completed in 30 seconds which is
        // the default transaction commit timeout. Using the timeout information to figure out the rate limit is
        // not going to be useful - so we leave it as it is
    }

    @VisibleForTesting
    static void setTicker(Ticker ticker) {
        TICKER = ticker;
    }
}