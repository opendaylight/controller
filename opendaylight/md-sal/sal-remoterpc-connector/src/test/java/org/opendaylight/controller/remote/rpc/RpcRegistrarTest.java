/*
 * Copyright (c) 2017 Pantheon Technologies s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.remote.rpc;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Address;
import akka.actor.Props;
import akka.testkit.JavaTestKit;
import akka.testkit.TestActorRef;
import com.google.common.util.concurrent.Futures;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.opendaylight.controller.cluster.raft.TestActorFactory;
import org.opendaylight.controller.cluster.raft.utils.MessageCollectorActor;
import org.opendaylight.controller.md.sal.dom.api.DOMRpcException;
import org.opendaylight.controller.md.sal.dom.api.DOMRpcIdentifier;
import org.opendaylight.controller.md.sal.dom.api.DOMRpcImplementation;
import org.opendaylight.controller.md.sal.dom.api.DOMRpcImplementationRegistration;
import org.opendaylight.controller.md.sal.dom.api.DOMRpcProviderService;
import org.opendaylight.controller.md.sal.dom.api.DOMRpcResult;
import org.opendaylight.controller.md.sal.dom.broker.impl.DOMRpcRouter;
import org.opendaylight.controller.md.sal.dom.spi.DefaultDOMRpcResult;
import org.opendaylight.controller.remote.rpc.registry.RpcRegistry.Messages.UpdateRemoteEndpoints;
import org.opendaylight.controller.remote.rpc.registry.RpcRegistry.RemoteRpcEndpoint;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;
import org.opendaylight.yangtools.yang.data.impl.schema.Builders;
import org.opendaylight.yangtools.yang.data.impl.schema.ImmutableNodes;
import org.opendaylight.yangtools.yang.model.api.SchemaPath;

public class RpcRegistrarTest {
    static final String TEST_REV = "2014-08-28";
    static final String TEST_NS = "urn:test";
    static final QName TEST_RPC_OUTPUT = QName.create(TEST_NS, TEST_REV, "output");
    private ActorSystem system;
    private RpcRegistrar rpcRegistrar;

    @Mock
    private DOMRpcProviderService service;

    public static ContainerNode makeRPCOutput(final String data) {
        return Builders.containerBuilder()
                .withNodeIdentifier(new YangInstanceIdentifier.NodeIdentifier(TEST_RPC_OUTPUT))
                .withChild(ImmutableNodes.leafNode(TEST_RPC_OUTPUT, data)).build();
    }

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
        final DOMRpcRouter router = new DOMRpcRouter();
        final DOMRpcImplementationRegistration<DOMRpcImplementation> bar =
                router.registerRpcImplementation((rpc, input) -> {
                    ContainerNode result = makeRPCOutput("bar");
                    return Futures.<DOMRpcResult, DOMRpcException>immediateCheckedFuture(new DefaultDOMRpcResult(result));
                }, DOMRpcIdentifier.create(SchemaPath.ROOT));

        when(service.registerRpcImplementation(any(DOMRpcImplementation.class), any(Set.class)))
                .thenReturn(bar);

        final Map<Address, Optional<RemoteRpcEndpoint>> endpoints = new HashMap<>();

        final TestActorFactory actorFactory = new TestActorFactory(ActorSystem.create("test"));

        final ActorRef actorRef = actorFactory.createActor(MessageCollectorActor.props());
        final Collection<DOMRpcIdentifier> identifiers = new ArrayList<>();
        identifiers.add(DOMRpcIdentifier.create(SchemaPath.ROOT));

        final RemoteRpcEndpoint remoteRpcEndpoint =
                new RemoteRpcEndpoint(actorRef, identifiers);
        endpoints.put(Address.apply("foo", "bar"), Optional.of(remoteRpcEndpoint));
        endpoints.put(Address.apply("foo2", "bar2"), Optional.of(remoteRpcEndpoint));

        final UpdateRemoteEndpoints message = new UpdateRemoteEndpoints(endpoints);
        rpcRegistrar.handleReceive(message);
        rpcRegistrar.handleReceive(message);
        //there must be possibility, how to trigger close in RpcRegistrar class,
        // without double rpcRegistrar.handleReceive(message);

        verify(service, times(4)).registerRpcImplementation(any(DOMRpcImplementation.class), any(Set.class));
    }
}