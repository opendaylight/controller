/*
 * Copyright (c) 2017 Pantheon Technologies s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.remote.rpc;

import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.testkit.JavaTestKit;
import akka.testkit.TestActorRef;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.opendaylight.controller.md.sal.dom.api.DOMRpcProviderService;
import org.opendaylight.controller.remote.rpc.registry.RpcRegistry;

public class RpcRegistrarTest {
    private ActorSystem system;
    private RpcRegistrar rpcRegistrar;

    @Mock
    private DOMRpcProviderService service;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        system = ActorSystem.create("test");

        final JavaTestKit testKit = new JavaTestKit(system);
        final RemoteRpcProviderConfig config = new RemoteRpcProviderConfig.Builder("system").build();
        final Props props = RpcRegistrar.props(config, service);
        final TestActorRef<?> testActorRef = new TestActorRef<>(system, props, testKit.getRef(), "actorRef");

        rpcRegistrar = (RpcRegistrar) testActorRef.underlyingActor();
    }

    @After
    public void tearDown() throws Exception {
        JavaTestKit.shutdownActorSystem(system);
    }

    @Test
    public void testPostStop() throws Exception {
        rpcRegistrar.postStop();
    }

    @Test
    public void testHandleReceive() throws Exception {
        //RpcRegistry.Messages.UpdateRemoteEndpoints message = new RpcRegistry.Messages.UpdateRemoteEndpoints();
        //rpcRegistrar.handleReceive(message);
    }
}