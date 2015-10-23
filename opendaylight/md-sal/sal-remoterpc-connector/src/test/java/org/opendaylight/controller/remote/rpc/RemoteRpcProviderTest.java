/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */


package org.opendaylight.controller.remote.rpc;


import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.testkit.JavaTestKit;
import com.typesafe.config.Config;
import java.util.concurrent.TimeUnit;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.opendaylight.controller.md.sal.dom.api.DOMRpcProviderService;
import org.opendaylight.controller.md.sal.dom.api.DOMRpcService;
import org.opendaylight.controller.sal.core.api.Broker;
import org.opendaylight.controller.sal.core.api.model.SchemaService;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import scala.concurrent.Await;
import scala.concurrent.duration.Duration;

public class RemoteRpcProviderTest {

  static ActorSystem system;
  static RemoteRpcProviderConfig moduleConfig;

  @BeforeClass
  public static void setup() throws InterruptedException {
    moduleConfig = new RemoteRpcProviderConfig.Builder("odl-cluster-rpc").build();
    final Config config = moduleConfig.get();
    system = ActorSystem.create("odl-cluster-rpc", config);

  }

  @AfterClass
  public static void teardown() {
    JavaTestKit.shutdownActorSystem(system);
    system = null;
  }

    @Test
    public void testRemoteRpcProvider() throws Exception {
        try (final RemoteRpcProvider rpcProvider = new RemoteRpcProvider(system, mock(DOMRpcProviderService.class),
                new RemoteRpcProviderConfig(system.settings().config()))) {
            final Broker.ProviderSession session = mock(Broker.ProviderSession.class);
            final SchemaService schemaService = mock(SchemaService.class);
            when(schemaService.getGlobalContext()).thenReturn(mock(SchemaContext.class));
            when(session.getService(SchemaService.class)).thenReturn(schemaService);
            when(session.getService(DOMRpcService.class)).thenReturn(mock(DOMRpcService.class));

            rpcProvider.onSessionInitiated(session);

            final ActorRef actorRef = Await.result(
                    system.actorSelection(moduleConfig.getRpcManagerPath()).resolveOne(
                            Duration.create(1, TimeUnit.SECONDS)), Duration.create(2, TimeUnit.SECONDS));

            Assert.assertTrue(actorRef.path().toString().contains(moduleConfig.getRpcManagerPath()));
        }
    }
}
