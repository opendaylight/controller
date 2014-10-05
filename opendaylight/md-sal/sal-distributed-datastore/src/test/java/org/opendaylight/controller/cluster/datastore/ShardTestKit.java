/*
 * Copyright (c) 2014 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore;

import org.junit.Assert;
import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.event.Logging;
import akka.testkit.JavaTestKit;

class ShardTestKit extends JavaTestKit {

    ShardTestKit(ActorSystem actorSystem) {
        super(actorSystem);
    }

    protected void waitForLogMessage(final Class logLevel, ActorRef subject, String logMessage){
        // Wait for a specific log message to show up
        final boolean result =
            new JavaTestKit.EventFilter<Boolean>(logLevel
            ) {
                @Override
                protected Boolean run() {
                    return true;
                }
            }.from(subject.path().toString())
                .message(logMessage)
                .occurrences(1).exec();

        Assert.assertEquals(true, result);

    }

    protected void waitUntilLeader(ActorRef subject) {
        waitForLogMessage(Logging.Info.class, subject,
                "Switching from state Candidate to Leader");
    }
}