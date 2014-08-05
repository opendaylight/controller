/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.remote.rpc.utils;


import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.japi.Pair;
import akka.testkit.JavaTestKit;
import com.typesafe.config.ConfigFactory;
import junit.framework.Assert;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;


public class LatestEntryRoutingLogicTest {

  static ActorSystem system;
  static ActorSystem node1;
  static ActorSystem node2;

  @BeforeClass
  public static void setup() throws InterruptedException {
    Thread.sleep(1000);
    system = ActorSystem.create("opendaylight-rpc", ConfigFactory.load().getConfig("odl-cluster"));
    node1 = ActorSystem.create("opendaylight-rpc", ConfigFactory.load().getConfig("memberA"));
    node2 = ActorSystem.create("opendaylight-rpc", ConfigFactory.load().getConfig("memberB"));
  }

  @AfterClass
  public static void teardown() {
    JavaTestKit.shutdownActorSystem(node1);
    JavaTestKit.shutdownActorSystem(node2);
    JavaTestKit.shutdownActorSystem(system);
    node1 = null;
    node2 = null;
    system = null;
  }

  @Test
  public void testRoutingLogic() {
    List<Pair<ActorRef, Long>> pairList = new ArrayList<>();
    JavaTestKit probe1 = new JavaTestKit(node1);
    JavaTestKit probe2 = new JavaTestKit(node2);
    JavaTestKit probe3 = new JavaTestKit(system);
    ActorRef actor1 = probe1.getRef();
    ActorRef actor2 = probe2.getRef();
    ActorRef actor3 = probe3.getRef();
    pairList.add(new Pair<ActorRef, Long>(actor1, 1000L));
    pairList.add(new Pair<ActorRef, Long>(actor2, 3000L));
    pairList.add(new Pair<ActorRef, Long>(actor3, 2000L));
    RoutingLogic logic = new LatestEntryRoutingLogic(pairList);
    Assert.assertTrue(logic.select().equals(actor2));
  }
}
