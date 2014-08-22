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
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.opendaylight.controller.sal.core.api.Broker;
import org.opendaylight.controller.sal.core.api.RpcProvisionRegistry;
import org.opendaylight.controller.sal.core.api.model.SchemaService;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import scala.concurrent.Await;
import scala.concurrent.duration.Duration;

import java.util.concurrent.TimeUnit;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class RemoteRpcProviderTest {

  static ActorSystem system;


  @BeforeClass
  public static void setup() throws InterruptedException {
    system = ActorSystem.create("odl-cluster-rpc", ConfigFactory.load().getConfig("odl-cluster-rpc"));
  }

  @AfterClass
  public static void teardown() {
    JavaTestKit.shutdownActorSystem(system);
    system = null;
  }

  @Test
  public void testRemoteRpcProvider() throws Exception {
    RemoteRpcProvider rpcProvider = new RemoteRpcProvider(system, mock(RpcProvisionRegistry.class));
    Broker.ProviderSession session = mock(Broker.ProviderSession.class);
    SchemaService schemaService = mock(SchemaService.class);
    when(schemaService.getGlobalContext()). thenReturn(mock(SchemaContext.class));
    when(session.getService(SchemaService.class)).thenReturn(schemaService);
    rpcProvider.onSessionInitiated(session);
    ActorRef actorRef = Await.result(system.actorSelection(ActorConstants.RPC_MANAGER_PATH).resolveOne(Duration.create(1, TimeUnit.SECONDS)),
        Duration.create(2, TimeUnit.SECONDS));
    Assert.assertTrue(actorRef.path().toString().contains(ActorConstants.RPC_MANAGER_PATH));
  }
}
