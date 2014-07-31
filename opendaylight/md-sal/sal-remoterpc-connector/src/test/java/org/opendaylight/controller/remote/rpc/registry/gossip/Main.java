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

public class Main {
    public static void main(String args[]) throws InterruptedException {
        ActorSystem memberA = ActorSystem.create("opendaylight-rpc",
                ConfigFactory.load().getConfig("memberA"));

        ActorRef store = memberA.actorOf(Props.create(BucketStore.class), "store");
        //ActorRef gossiper = memberA.actorOf(Props.create(Gossiper.class), "gossiper");

        Thread.sleep(10000);
        ActorSelection remStore = memberA.actorSelection("akka.tcp://opendaylight-rpc@localhost:2552/user/store");
        ActorSelection remGos = memberA.actorSelection("akka.tcp://opendaylight-rpc@localhost:2552/user/store/gossiper");

        Future<ActorRef> futSt = remStore.resolveOne(new FiniteDuration(1000, TimeUnit.MILLISECONDS));
        Future<ActorRef> futRem = remGos.resolveOne(new FiniteDuration(1000, TimeUnit.MILLISECONDS));

        ActorRef remoteRefSt = null;
        ActorRef remoteRefGos = null;
        try {
            remoteRefSt = Await.result(futSt, Duration.create(1000, TimeUnit.MILLISECONDS));
            remoteRefGos = Await.result(futRem, Duration.create(1000, TimeUnit.MILLISECONDS));
        } catch (Exception e) {
            e.printStackTrace();
        }

        System.out.println("Got remote store" + remoteRefSt);
        System.out.println("Got remote gossiper" + remoteRefGos);

        Thread.sleep(10000);
        ActorSelection localGossiper = memberA.actorSelection("akka.tcp://opendaylight-rpc@localhost:2551/user/store/gossiper");
        localGossiper.tell(new Messages.GossiperMessages.GossipTick(), ActorRef.noSender());
        Thread.sleep(1000);
        localGossiper.tell(new Messages.GossiperMessages.GossipTick(), ActorRef.noSender());
    }
}
