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
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.opendaylight.controller.cluster.raft.TestActorFactory;
import org.opendaylight.controller.cluster.raft.utils.MessageCollectorActor;
import org.opendaylight.controller.md.sal.dom.api.DOMRpcIdentifier;
import org.opendaylight.controller.md.sal.dom.api.DOMRpcProviderService;
import org.opendaylight.controller.remote.rpc.registry.RpcRegistry.Messages.UpdateRemoteEndpoints;
import org.opendaylight.controller.remote.rpc.registry.RpcRegistry.RemoteRpcEndpoint;
import org.opendaylight.yangtools.yang.model.api.SchemaPath;
import scala.concurrent.duration.Duration;

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
        final JavaTestKit registrar1 = new JavaTestKit(system);
        final JavaTestKit registrar2 = new JavaTestKit(system);

        final UpdateRemoteEndpoints remoteEndpoints =
                new UpdateRemoteEndpoints(ImmutableMap.of(Address.apply("foo", "bar"), Optional.empty()));

        registrar2.send(registrar1.getTestActor(), remoteEndpoints);

        final UpdateRemoteEndpoints msg = registrar1.expectMsgClass(Duration.create(3, TimeUnit.SECONDS),
                UpdateRemoteEndpoints.class);

        final Map<Address, Optional<RemoteRpcEndpoint>> endpoints = msg.getEndpoints();

        final UpdateRemoteEndpoints message = new UpdateRemoteEndpoints(endpoints);
        rpcRegistrar.handleReceive(message);
    }

    @Test
    public void testHandleReceive1() throws Exception {

        final Map<Address, Optional<RemoteRpcEndpoint>> endpoints = new HashMap<>();

        final TestActorFactory actorFactory = new TestActorFactory(ActorSystem.create("test"));

        final ActorRef actorRef = actorFactory.createActor(MessageCollectorActor.props());
        final Collection<DOMRpcIdentifier> identifiers = new ArrayList<>();
        identifiers.add(DOMRpcIdentifier.create(SchemaPath.ROOT));

        final RemoteRpcEndpoint remoteRpcEndpoint =
                new RemoteRpcEndpoint(actorRef, identifiers);
        endpoints.put(Address.apply("foo", "bar"), Optional.of(remoteRpcEndpoint));

        final UpdateRemoteEndpoints message = new UpdateRemoteEndpoints(endpoints);
        rpcRegistrar.handleReceive(message);
    }
}