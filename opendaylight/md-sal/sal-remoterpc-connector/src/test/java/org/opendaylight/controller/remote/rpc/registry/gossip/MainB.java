/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.remote.rpc.registry.gossip;

import akka.actor.ActorRef;
import akka.actor.ActorSelection;
import akka.actor.ActorSystem;
import akka.actor.Props;
import com.typesafe.config.ConfigFactory;
import scala.concurrent.Await;
import scala.concurrent.Future;
import scala.concurrent.duration.Duration;
import scala.concurrent.duration.FiniteDuration;

import java.util.concurrent.TimeUnit;

/**
 * Created by abhishk2 on 8/1/14.
 */
public class MainB {
    public static void main (String args[]) throws InterruptedException {
        ActorSystem memberB = ActorSystem.create("opendaylight-rpc",
                ConfigFactory.load().getConfig("memberB"));

        ActorRef store = memberB.actorOf(Props.create(BucketStore.class), "store");
        //ActorRef gossiper = memberB.actorOf(Props.create(Gossiper.class), "gossiper");

        ActorSelection remGos = memberB.actorSelection("akka.tcp://opendaylight-rpc@localhost:2552/user/store/gossiper");

        Thread.sleep(5000);
        Future<ActorRef> futRem = remGos.resolveOne(new FiniteDuration(1000, TimeUnit.MILLISECONDS));

        ActorRef remoteRefGos = null;
        try {
            remoteRefGos = Await.result(futRem, Duration.create(1000, TimeUnit.MILLISECONDS));
        } catch (Exception e) {
            e.printStackTrace();
        }

        System.out.println("Got remote ref" + remoteRefGos);
    }
}
