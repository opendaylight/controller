/*
 * Copyright (c) 2017 Pantheon Technologies s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.remote.rpc;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Address;
import akka.actor.Props;
import akka.testkit.JavaTestKit;
import akka.testkit.TestActorRef;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.opendaylight.controller.md.sal.dom.api.DOMRpcIdentifier;
import org.opendaylight.controller.md.sal.dom.api.DOMRpcImplementationRegistration;
import org.opendaylight.controller.md.sal.dom.api.DOMRpcProviderService;
import org.opendaylight.controller.remote.rpc.registry.RpcRegistry.Messages.UpdateRemoteEndpoints;
import org.opendaylight.controller.remote.rpc.registry.RpcRegistry.RemoteRpcEndpoint;
import org.opendaylight.yangtools.yang.model.api.SchemaPath;

public class RpcRegistrarTest {
    private ActorSystem system;
    private TestActorRef<RpcRegistrar> testActorRef;
    private RpcRegistrar rpcRegistrar;

    @Mock
    private DOMRpcProviderService service;
    @Mock
    private DOMRpcImplementationRegistration<RemoteRpcImplementation> registration;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        system = ActorSystem.create("test");

        final JavaTestKit testKit = new JavaTestKit(system);
        final RemoteRpcProviderConfig config = new RemoteRpcProviderConfig.Builder("system").build();
        final Props props = RpcRegistrar.props(config, service);
        testActorRef = new TestActorRef<>(system, props, testKit.getRef(), "actorRef");

        rpcRegistrar = testActorRef.underlyingActor();
    }

    @After
    public void tearDown() throws Exception {
        JavaTestKit.shutdownActorSystem(system, null, Boolean.TRUE);
    }

    @Test
    public void testPostStop() throws Exception {
        rpcRegistrar.postStop();
    }

    @Test
    public void testHandleReceiveAddEndpoint() throws Exception {
        final Set<DOMRpcIdentifier> identifiers = new HashSet<>();
        identifiers.add(DOMRpcIdentifier.create(SchemaPath.ROOT));

        final Map<Address, Optional<RemoteRpcEndpoint>> endpoints = new HashMap<>();
        final RemoteRpcEndpoint remoteRpcEndpoint = new RemoteRpcEndpoint(testActorRef, identifiers);
        endpoints.put(Address.apply("foo", "bar"), Optional.of(remoteRpcEndpoint));

        final UpdateRemoteEndpoints message = new UpdateRemoteEndpoints(endpoints);

        Mockito.doReturn(registration).when(service).registerRpcImplementation(
                Mockito.any(RemoteRpcImplementation.class), Mockito.eq(identifiers));

        testActorRef.tell(message, ActorRef.noSender());
        Mockito.verify(registration, Mockito.never()).close();
    }

    @Test
    public void testHandleReceiveAddAndRemoveEndpoint() throws Exception {
        final Set<DOMRpcIdentifier> identifiers = new HashSet<>();
        identifiers.add(DOMRpcIdentifier.create(SchemaPath.ROOT));

        final Map<Address, Optional<RemoteRpcEndpoint>> endpoints = new HashMap<>();
        final RemoteRpcEndpoint remoteRpcEndpoint = new RemoteRpcEndpoint(testActorRef, identifiers);
        endpoints.put(Address.apply("foo", "bar"), Optional.of(remoteRpcEndpoint));

        final UpdateRemoteEndpoints message = new UpdateRemoteEndpoints(endpoints);

        Mockito.doReturn(registration).when(service).registerRpcImplementation(
                Mockito.any(RemoteRpcImplementation.class), Mockito.eq(identifiers));

        testActorRef.tell(message, ActorRef.noSender());
        Mockito.verify(registration, Mockito.never()).close();

        testActorRef.tell(message, ActorRef.noSender());
        Mockito.verify(registration).close();
    }
}