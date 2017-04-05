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
import com.google.common.collect.ImmutableMap;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.opendaylight.controller.md.sal.dom.api.DOMRpcIdentifier;
import org.opendaylight.controller.md.sal.dom.api.DOMRpcImplementationRegistration;
import org.opendaylight.controller.md.sal.dom.api.DOMRpcProviderService;
import org.opendaylight.controller.remote.rpc.registry.RpcRegistry.Messages.UpdateRemoteEndpoints;
import org.opendaylight.controller.remote.rpc.registry.RpcRegistry.RemoteRpcEndpoint;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.model.api.SchemaPath;

public class RpcRegistrarTest {
    @Mock
    private DOMRpcProviderService service;
    @Mock
    private DOMRpcImplementationRegistration<RemoteRpcImplementation> oldReg;
    @Mock
    private DOMRpcImplementationRegistration<RemoteRpcImplementation> newReg;

    private ActorSystem system;
    private TestActorRef<RpcRegistrar> testActorRef;
    private Address endpointAddress;
    private RemoteRpcEndpoint firstEndpoint;
    private RemoteRpcEndpoint secondEndpoint;
    private RpcRegistrar rpcRegistrar;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        system = ActorSystem.create("test");

        final JavaTestKit testKit = new JavaTestKit(system);
        final RemoteRpcProviderConfig config = new RemoteRpcProviderConfig.Builder("system").build();
        final Props props = RpcRegistrar.props(config, service);
        testActorRef = new TestActorRef<>(system, props, testKit.getRef(), "actorRef");
        endpointAddress = new Address("http", "local");

        final DOMRpcIdentifier firstEndpointId = DOMRpcIdentifier.create(
                SchemaPath.create(true, QName.create("first:identifier", "foo")));
        final DOMRpcIdentifier secondEndpointId = DOMRpcIdentifier.create(
                SchemaPath.create(true, QName.create("second:identifier", "bar")));

        final JavaTestKit senderKit = new JavaTestKit(system);
        firstEndpoint = new RemoteRpcEndpoint(senderKit.getRef(), Collections.singletonList(firstEndpointId));
        secondEndpoint = new RemoteRpcEndpoint(senderKit.getRef(), Collections.singletonList(secondEndpointId));

        Mockito.doReturn(oldReg).when(service).registerRpcImplementation(
                Mockito.any(RemoteRpcImplementation.class), Mockito.eq(firstEndpoint.getRpcs()));

        Mockito.doReturn(newReg).when(service).registerRpcImplementation(
                Mockito.any(RemoteRpcImplementation.class), Mockito.eq(secondEndpoint.getRpcs()));

        rpcRegistrar = testActorRef.underlyingActor();
    }

    @After
    public void tearDown() throws Exception {
        JavaTestKit.shutdownActorSystem(system, null, Boolean.TRUE);
    }

    @Test
    public void testPostStop() throws Exception {
        testActorRef.tell(new UpdateRemoteEndpoints(ImmutableMap.of(endpointAddress, Optional.of(firstEndpoint))),
                ActorRef.noSender());
        testActorRef.tell(new UpdateRemoteEndpoints(ImmutableMap.of(endpointAddress, Optional.of(secondEndpoint))),
                ActorRef.noSender());

        rpcRegistrar.postStop();

        Mockito.verify(oldReg).close();
        Mockito.verify(newReg).close();
    }

    @Test
    public void testHandleReceiveAddEndpoint() throws Exception {
        final Map<Address, Optional<RemoteRpcEndpoint>> endpoints = ImmutableMap.of(
                endpointAddress, Optional.of(firstEndpoint));
        testActorRef.tell(new UpdateRemoteEndpoints(endpoints), ActorRef.noSender());

        Mockito.verify(service).registerRpcImplementation(
                Mockito.any(RemoteRpcImplementation.class), Mockito.eq(firstEndpoint.getRpcs()));
        Mockito.verifyNoMoreInteractions(service, oldReg, newReg);
    }

    @Test
    public void testHandleReceiveRemoveEndpoint() throws Exception {
        final Map<Address, Optional<RemoteRpcEndpoint>> endpoints = ImmutableMap.of(
                endpointAddress, Optional.empty());
        testActorRef.tell(new UpdateRemoteEndpoints(endpoints), ActorRef.noSender());
        Mockito.verifyNoMoreInteractions(service, oldReg, newReg);
    }

    @Test
    public void testHandleReceiveUpdateEndpoint() throws Exception {
        final InOrder inOrder = Mockito.inOrder(service, oldReg, newReg);

        testActorRef.tell(new UpdateRemoteEndpoints(ImmutableMap.of(endpointAddress, Optional.of(firstEndpoint))),
                ActorRef.noSender());

        // first registration
        inOrder.verify(service).registerRpcImplementation(
                Mockito.any(RemoteRpcImplementation.class), Mockito.eq(firstEndpoint.getRpcs()));

        testActorRef.tell(new UpdateRemoteEndpoints(ImmutableMap.of(endpointAddress, Optional.of(secondEndpoint))),
                ActorRef.noSender());

        // second registration
        inOrder.verify(service).registerRpcImplementation(
                Mockito.any(RemoteRpcImplementation.class), Mockito.eq(secondEndpoint.getRpcs()));

        // verify first registration is closed
        inOrder.verify(oldReg).close();

        Mockito.verifyNoMoreInteractions(service, oldReg, newReg);
    }
}