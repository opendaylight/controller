/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.remote.rpc;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.testkit.JavaTestKit;
import com.typesafe.config.ConfigFactory;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.opendaylight.controller.remote.rpc.registry.RpcRegistry;
import org.opendaylight.yangtools.yang.common.QName;

import java.net.URI;
import java.net.URISyntaxException;

public class RpcListenerTest {

  static ActorSystem system;


  @BeforeClass
  public static void setup() throws InterruptedException {
    //Let previous class release the port
    Thread.sleep(1000);
    system = ActorSystem.create("opendaylight-rpc", ConfigFactory.load().getConfig("odl-cluster"));
  }

  @AfterClass
  public static void teardown() {
    JavaTestKit.shutdownActorSystem(system);
    system = null;
  }

  @Test
  public void testRpcAdd() throws URISyntaxException {
    new JavaTestKit(system) {
      {
        JavaTestKit probeReg = new JavaTestKit(system);
        ActorRef rpcRegistry = probeReg.getRef();

        RpcListener rpcListener = new RpcListener(rpcRegistry);

        QName qName = new QName(new URI("actor2"), "actor2");

        rpcListener.onRpcImplementationAdded(qName);
        probeReg.expectMsgClass(RpcRegistry.Messages.AddOrUpdateRoutes.class);
      }};

  }

  @Test
  public void testRocRemove() throws URISyntaxException {
    new JavaTestKit(system) {
      {
        JavaTestKit probeReg = new JavaTestKit(system);
        ActorRef rpcRegistry = probeReg.getRef();

        RpcListener rpcListener = new RpcListener(rpcRegistry);

        QName qName = new QName(new URI("actor2"), "actor2");

        rpcListener.onRpcImplementationRemoved(qName);
        probeReg.expectMsgClass(RpcRegistry.Messages.RemoveRoutes.class);
      }};

  }
}
