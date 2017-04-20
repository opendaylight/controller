/*
 * Copyright (c) 2014, 2017 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.remote.rpc;

import static org.mockito.Mockito.mock;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.testkit.JavaTestKit;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import java.util.concurrent.TimeUnit;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.opendaylight.controller.md.sal.dom.api.DOMRpcProviderService;
import org.opendaylight.controller.md.sal.dom.api.DOMRpcService;
import scala.concurrent.Await;
import scala.concurrent.duration.Duration;

public class RemoteRpcProviderTest {
    static ActorSystem system;
    static RemoteRpcProviderConfig moduleConfig;

    @BeforeClass
    public static void setup() throws InterruptedException {
        moduleConfig = new RemoteRpcProviderConfig.Builder("odl-cluster-rpc")
                .withConfigReader(ConfigFactory::load).build();
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
        try (RemoteRpcProvider rpcProvider = new RemoteRpcProvider(system, mock(DOMRpcProviderService.class),
            mock(DOMRpcService.class), new RemoteRpcProviderConfig(system.settings().config()))) {

            rpcProvider.start();

            final ActorRef actorRef = Await.result(
                    system.actorSelection(moduleConfig.getRpcManagerPath()).resolveOne(
                            Duration.create(1, TimeUnit.SECONDS)), Duration.create(2, TimeUnit.SECONDS));

            Assert.assertTrue(actorRef.path().toString().contains(moduleConfig.getRpcManagerPath()));
        }
    }
}
