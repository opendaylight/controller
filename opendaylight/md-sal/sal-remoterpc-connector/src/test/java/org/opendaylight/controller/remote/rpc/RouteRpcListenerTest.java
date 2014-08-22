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
import org.opendaylight.controller.md.sal.common.impl.routing.RoutingUtils;
import org.opendaylight.controller.remote.rpc.registry.RpcRegistry;
import org.opendaylight.controller.sal.core.api.RpcRoutingContext;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;

import java.net.URI;
import java.net.URISyntaxException;

public class RouteRpcListenerTest {

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
  public void testRouteAdd() throws URISyntaxException, InterruptedException {
    new JavaTestKit(system) {
      {
        // Test announcements
        JavaTestKit probeReg = new JavaTestKit(system);
        ActorRef rpcRegistry = probeReg.getRef();

        RoutedRpcListener rpcListener = new RoutedRpcListener(rpcRegistry);

        QName qName = new QName(new URI("actor2"), "actor2");
        RpcRoutingContext context = RpcRoutingContext.create(qName, qName);
        YangInstanceIdentifier identifier = YangInstanceIdentifier.create(new YangInstanceIdentifier.NodeIdentifier(qName));
        rpcListener.onRouteChange(RoutingUtils.announcementChange(context, identifier));

        probeReg.expectMsgClass(RpcRegistry.Messages.AddOrUpdateRoutes.class);
      }};
  }

  @Test
  public void testRouteRemove() throws URISyntaxException, InterruptedException {
    new JavaTestKit(system) {
      {
        // Test announcements
        JavaTestKit probeReg = new JavaTestKit(system);
        ActorRef rpcRegistry = probeReg.getRef();

        RoutedRpcListener rpcListener = new RoutedRpcListener(rpcRegistry);

        QName qName = new QName(new URI("actor2"), "actor2");
        RpcRoutingContext context = RpcRoutingContext.create(qName, qName);
        YangInstanceIdentifier identifier = YangInstanceIdentifier.create(new YangInstanceIdentifier.NodeIdentifier(qName));
        rpcListener.onRouteChange(RoutingUtils.removalChange(context, identifier));

        probeReg.expectMsgClass(RpcRegistry.Messages.RemoveRoutes.class);
      }};
  }
}
