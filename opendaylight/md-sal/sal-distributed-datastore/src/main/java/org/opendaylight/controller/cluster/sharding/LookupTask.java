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
import javax.annotation.Nullable;

abstract class LookupTask implements Runnable {

    static final int LOOKUP_TASK_MAX_RETRIES = 100;

    private final ActorRef replyTo;
    private int retries = 0;

    LookupTask(final ActorRef replyTo) {
        this.replyTo = replyTo;
    }

    abstract void reschedule(int retries);

    void tryReschedule(@Nullable final Throwable throwable) {
        if (retries <= LOOKUP_TASK_MAX_RETRIES) {
            retries++;
            reschedule(retries);
        } else {
            fail(throwable);
        }
    }

    void fail(@Nullable final Throwable throwable) {
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
