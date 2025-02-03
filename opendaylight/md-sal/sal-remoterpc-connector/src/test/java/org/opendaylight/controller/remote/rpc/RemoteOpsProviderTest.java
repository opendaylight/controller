/*
 * Copyright (c) 2014, 2017 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.remote.rpc;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.typesafe.config.ConfigFactory;
import java.util.concurrent.TimeUnit;
import org.apache.pekko.actor.ActorSystem;
import org.apache.pekko.testkit.javadsl.TestKit;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opendaylight.mdsal.dom.api.DOMActionProviderService;
import org.opendaylight.mdsal.dom.api.DOMActionService;
import org.opendaylight.mdsal.dom.api.DOMRpcProviderService;
import org.opendaylight.mdsal.dom.api.DOMRpcService;
import scala.concurrent.Await;
import scala.concurrent.duration.FiniteDuration;

@ExtendWith(MockitoExtension.class)
class RemoteOpsProviderTest {
    @Mock
    private DOMRpcProviderService rpcProviderService;
    @Mock
    private DOMRpcService rpcService;
    @Mock
    private DOMActionProviderService actionProviderService;
    @Mock
    private DOMActionService actionService;

    static ActorSystem system;
    static RemoteOpsProviderConfig moduleConfig;

    @BeforeAll
    static void beforeAll() {
        moduleConfig = new RemoteOpsProviderConfig.Builder("odl-cluster-rpc")
            .withConfigReader(ConfigFactory::load)
            .build();
        system = ActorSystem.create("odl-cluster-rpc", moduleConfig.get());

    }

    @AfterAll
    static void afterAll() {
        TestKit.shutdownActorSystem(system);
        system = null;
    }

    @Test
    void testRemoteRpcProvider() throws Exception {
        try (var rpcProvider = new RemoteOpsProvider("test", system, rpcProviderService, rpcService,
            new RemoteOpsProviderConfig(system.settings().config()), actionProviderService, actionService)) {

            rpcProvider.start();
            final var actorRef = Await.result(
                    system.actorSelection(moduleConfig.getRpcManagerPath()).resolveOne(
                            FiniteDuration.create(1, TimeUnit.SECONDS)), FiniteDuration.create(2, TimeUnit.SECONDS));

            assertTrue(actorRef.path().toString().contains(moduleConfig.getRpcManagerPath()));
        }
    }
}
