/*
 * Copyright (c) 2017 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.sharding;

import static akka.actor.ActorRef.noSender;

import akka.actor.ActorRef;
import akka.actor.Status;
import org.eclipse.jdt.annotation.Nullable;

/**
 * Base class for lookup tasks. Lookup tasks are supposed to run repeatedly until successful lookup or maximum retries
 * are hit. This class is NOT thread-safe.
 */
@Deprecated(forRemoval = true)
abstract class LookupTask implements Runnable {
    private final int maxRetries;
    private final ActorRef replyTo;
    private int retried = 0;

    LookupTask(final ActorRef replyTo, final int maxRetries) {
        this.replyTo = replyTo;
        this.maxRetries = maxRetries;
    }

    abstract void reschedule(int retries);

    void tryReschedule(final @Nullable Throwable throwable) {
        if (retried <= maxRetries) {
            retried++;
            reschedule(retried);
        } else {
            fail(throwable);
        }
    }

    void fail(final @Nullable Throwable throwable) {
        if (throwable == null) {
            replyTo.tell(new Status.Failure(
                    new DOMDataTreeShardCreationFailedException("Unable to find the backend shard."
                            + "Failing..")), noSender());
        } else {
            replyTo.tell(new Status.Failure(
                    new DOMDataTreeShardCreationFailedException("Unable to find the backend shard."
                            + "Failing..", throwable)), noSender());
        }
    }
}
