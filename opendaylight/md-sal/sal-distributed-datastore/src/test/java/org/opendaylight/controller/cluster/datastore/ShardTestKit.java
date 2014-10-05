/*
 * Copyright (c) 2014 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore;

import java.util.concurrent.TimeUnit;
import org.junit.Assert;
import org.opendaylight.controller.cluster.raft.client.messages.FindLeader;
import org.opendaylight.controller.cluster.raft.client.messages.FindLeaderReply;
import com.google.common.util.concurrent.Uninterruptibles;
import scala.concurrent.Await;
import scala.concurrent.Future;
import scala.concurrent.duration.Duration;
import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.pattern.Patterns;
import akka.testkit.JavaTestKit;
import akka.util.Timeout;

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

    protected void waitUntilLeader(ActorRef shard) {
        for(int i = 0; i < 20 * 5; i++) {
            Future<Object> future = Patterns.ask(shard, new FindLeader(), new Timeout(5, TimeUnit.SECONDS));
            try {
                FindLeaderReply resp = (FindLeaderReply)Await.result(future, Duration.create(5, TimeUnit.SECONDS));
                if(resp.getLeaderActor() != null) {
                    return;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            Uninterruptibles.sleepUninterruptibly(50, TimeUnit.MILLISECONDS);
        }

        Assert.fail("Leader not found for shard " + shard.path());
    }
}