/*
 * Copyright (c) 2015 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft;

import akka.actor.Cancellable;
import com.google.common.base.Preconditions;
import scala.concurrent.duration.FiniteDuration;

/**
 * An abstract class that implements a Runnable operation with a timer such that if the run method isn't
 * invoked within a timeout period, the operation is cancelled via {@link #doCancel}.
 * <p>
 * <b>Note:</b> this class is not thread safe and is intended for use only within the context of the same
 * actor that's passed on construction. The run method must be called on this actor's thread dispatcher as it
 * modifies internal state.
 *
 * @author Thomas Pantelis
 */
abstract class TimedRunnable implements Runnable {
    private final Cancellable cancelTimer;
    private boolean canRun = true;

    TimedRunnable(FiniteDuration timeout, RaftActor actor) {
        Preconditions.checkNotNull(timeout);
        Preconditions.checkNotNull(actor);
        cancelTimer = actor.getContext().system().scheduler().scheduleOnce(timeout, actor.self(),
                (Runnable) () -> cancel(), actor.getContext().system().dispatcher(), actor.self());
    }

    @Override
    public void run() {
        if(canRun) {
            canRun = false;
            cancelTimer.cancel();
            doRun();
        }
    }

    private void cancel() {
        canRun = false;
        doCancel();
    }

    /**
     * Overridden to perform the operation if not previously cancelled or run.
     */
    protected abstract void doRun();

    /**
     * Overridden to cancel the operation on time out.
     */
    protected abstract void doCancel();
}
