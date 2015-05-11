/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.datastore;

import com.codahale.metrics.Timer;
import com.google.common.base.Preconditions;
import org.opendaylight.controller.cluster.datastore.utils.ActorContext;

/**
 * TransactionRateLimitingCallback computes the new transaction rate limit on the successful completion of a
 * transaction
 */
public class TransactionRateLimitingCallback implements OperationCallback{
    private final Timer commitTimer;
    private Timer.Context timerContext;

    TransactionRateLimitingCallback(ActorContext actorContext){
        commitTimer = actorContext.getOperationTimer(ActorContext.COMMIT);
    }

    @Override
    public void run() {
        timerContext = commitTimer.time();
    }

    @Override
    public void success() {
        Preconditions.checkState(timerContext != null, "Call run before success");
        timerContext.stop();
    }

    @Override
    public void failure() {
        // This would mean we couldn't get a transaction completed in 30 seconds which is
        // the default transaction commit timeout. Using the timeout information to figure out the rate limit is
        // not going to be useful - so we leave it as it is
    }

}