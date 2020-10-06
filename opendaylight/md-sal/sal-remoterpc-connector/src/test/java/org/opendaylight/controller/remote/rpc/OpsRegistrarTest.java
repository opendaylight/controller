/*
 * Copyright (c) 2017 Pantheon Technologies s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.remote.rpc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Address;
import akka.actor.Props;
import akka.testkit.TestActorRef;
import akka.testkit.javadsl.TestKit;
import com.google.common.collect.ImmutableMap;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.opendaylight.controller.remote.rpc.registry.ActionRegistry.Messages.UpdateRemoteActionEndpoints;
import org.opendaylight.controller.remote.rpc.registry.ActionRegistry.RemoteActionEndpoint;
import org.opendaylight.controller.remote.rpc.registry.RpcRegistry.Messages.UpdateRemoteEndpoints;
import org.opendaylight.controller.remote.rpc.registry.RpcRegistry.RemoteRpcEndpoint;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.mdsal.dom.api.DOMActionInstance;
import org.opendaylight.mdsal.dom.api.DOMActionProviderService;
import org.opendaylight.mdsal.dom.api.DOMRpcIdentifier;
import org.opendaylight.mdsal.dom.api.DOMRpcImplementationRegistration;
import org.opendaylight.mdsal.dom.api.DOMRpcProviderService;
import org.opendaylight.yangtools.concepts.ObjectRegistration;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.model.api.stmt.SchemaNodeIdentifier.Absolute;

public class OpsRegistrarTest {
    @Mock
    private DOMRpcProviderService rpcService;
    @Mock
    private DOMActionProviderService actionService;
    @Mock
    private DOMRpcImplementationRegistration<RemoteRpcImplementation> oldReg;
    @Mock
    private DOMRpcImplementationRegistration<RemoteRpcImplementation> newReg;
    @Mock
    private ObjectRegistration<RemoteActionImplementation> oldActionReg;
    @Mock
    private ObjectRegistration<RemoteActionImplementation> newActionReg;

    private ActorSystem system;
    private TestActorRef<OpsRegistrar> testActorRef;
    private Address endpointAddress;
    private RemoteRpcEndpoint firstEndpoint;
    private RemoteRpcEndpoint secondEndpoint;
    private RemoteActionEndpoint firstActionEndpoint;
    private RemoteActionEndpoint secondActionEndpoint;
    private OpsRegistrar opsRegistrar;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        system = ActorSystem.create("test");

        final TestKit testKit = new TestKit(system);
        final RemoteOpsProviderConfig config = new RemoteOpsProviderConfig.Builder("system").build();
        final Props props = OpsRegistrar.props(config, rpcService, actionService);
        testActorRef = new TestActorRef<>(system, props, testKit.getRef(), "actorRef");
        endpointAddress = new Address("http", "local");

        final DOMRpcIdentifier firstEndpointId = DOMRpcIdentifier.create(QName.create("first:identifier", "foo"));
        final DOMRpcIdentifier secondEndpointId = DOMRpcIdentifier.create(QName.create("second:identifier", "bar"));
        final QName firstActionQName = QName.create("first:actionIdentifier", "fooAction");

        final DOMActionInstance firstActionInstance = DOMActionInstance.of(Absolute.of(firstActionQName),
                LogicalDatastoreType.OPERATIONAL,
                YangInstanceIdentifier.create(new YangInstanceIdentifier.NodeIdentifier(firstActionQName)));

        final DOMActionInstance secondActionInstance = DOMActionInstance.of(Absolute.of(firstActionQName),
                LogicalDatastoreType.OPERATIONAL,
                YangInstanceIdentifier.create(new YangInstanceIdentifier.NodeIdentifier(firstActionQName)));

        final TestKit senderKit = new TestKit(system);
        firstEndpoint = new RemoteRpcEndpoint(senderKit.getRef(), Collections.singletonList(firstEndpointId));
        secondEndpoint = new RemoteRpcEndpoint(senderKit.getRef(), Collections.singletonList(secondEndpointId));
        firstActionEndpoint = new RemoteActionEndpoint(senderKit.getRef(),
                Collections.singletonList(firstActionInstance));
        secondActionEndpoint = new RemoteActionEndpoint(senderKit.getRef(),
                Collections.singletonList(secondActionInstance));

        doReturn(oldReg).when(rpcService).registerRpcImplementation(any(RemoteRpcImplementation.class),
            eq(firstEndpoint.getRpcs()));
        doReturn(newReg).when(rpcService).registerRpcImplementation(any(RemoteRpcImplementation.class),
            eq(secondEndpoint.getRpcs()));

        doReturn(oldActionReg).when(actionService).registerActionImplementation(any(RemoteActionImplementation.class),
            eq(secondActionEndpoint.getActions()));
        doReturn(oldActionReg).when(actionService).registerActionImplementation(any(RemoteActionImplementation.class),
                eq(secondActionEndpoint.getActions()));

        opsRegistrar = testActorRef.underlyingActor();
    }

    @After
    public void tearDown() {
        TestKit.shutdownActorSystem(system, true);
    }

    @Test
    public void testHandleReceiveAddEndpoint() {
        final Map<Address, Optional<RemoteRpcEndpoint>> endpoints = ImmutableMap.of(
                endpointAddress, Optional.of(firstEndpoint));
        testActorRef.tell(new UpdateRemoteEndpoints(endpoints), ActorRef.noSender());

        verify(rpcService).registerRpcImplementation(any(RemoteRpcImplementation.class),
            eq(firstEndpoint.getRpcs()));
        verifyNoMoreInteractions(rpcService, oldReg, newReg);
    }

    @Test
    public void testHandleReceiveRemoveEndpoint() {
        final Map<Address, Optional<RemoteRpcEndpoint>> endpoints = ImmutableMap.of(
                endpointAddress, Optional.empty());
        testActorRef.tell(new UpdateRemoteEndpoints(endpoints), ActorRef.noSender());
        verifyNoMoreInteractions(rpcService, oldReg, newReg);
    }

    @Test
    public void testHandleReceiveUpdateRpcEndpoint() {
        final InOrder inOrder = inOrder(rpcService, oldReg, newReg);

        testActorRef.tell(new UpdateRemoteEndpoints(ImmutableMap.of(endpointAddress, Optional.of(firstEndpoint))),
                ActorRef.noSender());

        inOrder.verify(rpcService).registerRpcImplementation(any(RemoteRpcImplementation.class),
                eq(firstEndpoint.getRpcs()));

        testActorRef.tell(new UpdateRemoteEndpoints(ImmutableMap.of(endpointAddress, Optional.of(secondEndpoint))),
                ActorRef.noSender());

        inOrder.verify(rpcService).registerRpcImplementation(any(RemoteRpcImplementation.class),
                eq(secondEndpoint.getRpcs()));

        // verify first registration is closed
        inOrder.verify(oldReg).close();

        verifyNoMoreInteractions(rpcService, oldReg, newReg);
    }

    @Test
    public void testHandleReceiveUpdateActionEndpoint() {
        final InOrder inOrder = inOrder(actionService, oldActionReg, newActionReg);

        testActorRef.tell(new UpdateRemoteActionEndpoints(ImmutableMap.of(endpointAddress,
                Optional.of(firstActionEndpoint))), ActorRef.noSender());

        inOrder.verify(actionService).registerActionImplementation(any(RemoteActionImplementation.class),
                eq(firstActionEndpoint.getActions()));

        testActorRef.tell(new UpdateRemoteActionEndpoints(ImmutableMap.of(endpointAddress,
                Optional.of(secondActionEndpoint))), ActorRef.noSender());

        inOrder.verify(actionService).registerActionImplementation(any(RemoteActionImplementation.class),
                eq(secondActionEndpoint.getActions()));

        // verify first registration is closed
        inOrder.verify(oldActionReg).close();

        verifyNoMoreInteractions(actionService, oldActionReg, newActionReg);
    }
}
