/*
 * Copyright (c) 2014, 2017 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.remote.rpc;

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.testkit.javadsl.TestKit;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import java.util.concurrent.TimeUnit;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.opendaylight.mdsal.dom.api.DOMActionProviderService;
import org.opendaylight.mdsal.dom.api.DOMActionService;
import org.opendaylight.mdsal.dom.api.DOMRpcProviderService;
import org.opendaylight.mdsal.dom.api.DOMRpcService;
import scala.concurrent.Await;
import scala.concurrent.duration.FiniteDuration;

public class RemoteOpsProviderTest {
    static ActorSystem system;
    static RemoteOpsProviderConfig moduleConfig;

    @BeforeClass
    public static void setup() {
        moduleConfig = new RemoteOpsProviderConfig.Builder("odl-cluster-rpc")
                .withConfigReader(ConfigFactory::load).build();
        final Config config = moduleConfig.get();
        system = ActorSystem.create("odl-cluster-rpc", config);

    }

    @AfterClass
    public static void teardown() {
        TestKit.shutdownActorSystem(system);
        system = null;
    }

    @Test
    public void testRemoteRpcProvider() throws Exception {
        try (RemoteOpsProvider rpcProvider = new RemoteOpsProvider(system, mock(DOMRpcProviderService.class),
                mock(DOMRpcService.class), new RemoteOpsProviderConfig(system.settings().config()),
                mock(DOMActionProviderService.class), mock(DOMActionService.class))) {

            rpcProvider.start();
            final ActorRef actorRef = Await.result(
                    system.actorSelection(moduleConfig.getRpcManagerPath()).resolveOne(
                            FiniteDuration.create(1, TimeUnit.SECONDS)), FiniteDuration.create(2, TimeUnit.SECONDS));

            assertTrue(actorRef.path().toString().contains(moduleConfig.getRpcManagerPath()));
        }
    }
}
