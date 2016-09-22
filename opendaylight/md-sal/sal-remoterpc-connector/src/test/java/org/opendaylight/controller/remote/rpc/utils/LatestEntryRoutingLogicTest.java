/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.remote.rpc.utils;

import static org.junit.Assert.assertTrue;
import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.japi.Pair;
import akka.testkit.JavaTestKit;
import akka.testkit.TestProbe;
import com.typesafe.config.ConfigFactory;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

public class LatestEntryRoutingLogicTest {

  static ActorSystem system;

  @BeforeClass
  public static void setup() throws InterruptedException {
    system = ActorSystem.create("opendaylight-rpc", ConfigFactory.load().getConfig("odl-cluster-rpc"));
  }

  @AfterClass
  public static void teardown() {
    JavaTestKit.shutdownActorSystem(system);
    system = null;
  }

  @Test
  public void testRoutingLogic() {
    List<Pair<ActorRef, Long>> pairList = new ArrayList<>();
    TestProbe probe1 = new TestProbe(system);
    TestProbe probe2 = new TestProbe(system);
    TestProbe probe3 = new TestProbe(system);
    ActorRef actor1 = probe1.ref();
    ActorRef actor2 = probe2.ref();
    ActorRef actor3 = probe3.ref();
    pairList.add(new Pair<>(actor1, 1000L));
    pairList.add(new Pair<>(actor2, 3000L));
    pairList.add(new Pair<>(actor3, 2000L));
    RoutingLogic logic = new LatestEntryRoutingLogic(pairList);
    assertTrue(logic.select().equals(actor2));
  }
}
