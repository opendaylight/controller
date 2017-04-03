/*
 * Copyright (c) 2017 Pantheon Technologies s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.remote.rpc.registry.mbeans;

import static org.mockito.MockitoAnnotations.initMocks;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.ExtendedActorSystem;
import akka.serialization.JavaSerializer;
import akka.testkit.TestProbe;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.opendaylight.controller.cluster.common.actor.AkkaConfigurationReader;
import org.opendaylight.controller.cluster.raft.TestActorFactory;
import org.opendaylight.controller.remote.rpc.RemoteRpcProviderConfig;
import org.opendaylight.controller.remote.rpc.registry.RpcRegistry;

public class RemoteRpcRegistryMXBeanImplTest {

    private static final ActorSystem SYSTEM = ActorSystem.create("test");
    private static final ActorRef ACTOR_REF = TestProbe.apply(SYSTEM).ref();
    private static final ActorSystem SYSTEM_1 = ActorSystem.create("test1");
    private static final ActorRef ACTOR_REF_1 = TestProbe.apply(SYSTEM_1).ref();

    private RemoteRpcRegistryMXBeanImpl mxBean;
    private TestActorFactory factory;
    private static ActorSystem node1;


    @Mock
    private RpcRegistry rpcRegistry;


    static ActorSystem system;
    private RemoteRpcProviderConfig moduleConfig;

    @Before
    public void setUp() throws Exception {
        initMocks(this);

        factory = new TestActorFactory(ActorSystem.create("test"));

        AkkaConfigurationReader reader = ConfigFactory::load;

        RemoteRpcProviderConfig config1 = new RemoteRpcProviderConfig.Builder("memberA").gossipTickInterval("200ms")
                .withConfigReader(reader).build();
        node1 =  ActorSystem.create("opendaylight-rpc", config1.get());
        mxBean = new RemoteRpcRegistryMXBeanImpl(rpcRegistry);

        moduleConfig = new RemoteRpcProviderConfig.Builder("odl-cluster-rpc")
                .withConfigReader(ConfigFactory::load).build();
        final Config config = moduleConfig.get();

        system = ActorSystem.apply("asnmfowenjf", config);
        JavaSerializer.currentSystem().value_$eq((ExtendedActorSystem) system);

        final RemoteRpcProviderConfig providerConfig = new RemoteRpcProviderConfig(system.settings().config());
        final ActorRef invoker = null;
        final ActorRef registrar = null;

        final RpcRegistry rpcRegistry = new RpcRegistry(
                providerConfig, TestProbe.apply(system).ref(), ACTOR_REF);
    }

    @Test
    public void testGetGlobalRpc() throws Exception {

        /*final DOMRpcIdentifier routeId = DOMRpcIdentifier.create(SchemaPath.create(true,
                new QName(new URI("/mockrpc"), "type" )));

        final QName qName = QName.create("test", "2015-06-12", "test");
        final SchemaPath rpcType = SchemaPath.create(true, qName);
        final YangInstanceIdentifier testPath = YangInstanceIdentifier
                .create(new YangInstanceIdentifier.NodeIdentifier(qName));
        final DOMRpcIdentifier rpcId = DOMRpcIdentifier.create(rpcType, testPath);

        Set<DOMRpcIdentifier> rpcIdentifiers = ImmutableSet.of(routeId, rpcId);

        final RoutingTable routingTable1 = mock(RoutingTable.class);
        final ActorRef actorRef = mock(ActorRef.class);
        RoutingTable routingTable = new RoutingTable(actorRef, rpcIdentifiers);

        final Set<String> globalRpc = mxBean.getGlobalRpc();*/
    }

    @Test
    public void testGetLocalRegisteredRoutedRpc() throws Exception {

    }

    @Test
    public void testFindRpcByName() throws Exception {

    }

    @Test
    public void testFindRpcByRoute() throws Exception {

    }

    @Test
    public void testGetBucketVersions() throws Exception {

    }
}