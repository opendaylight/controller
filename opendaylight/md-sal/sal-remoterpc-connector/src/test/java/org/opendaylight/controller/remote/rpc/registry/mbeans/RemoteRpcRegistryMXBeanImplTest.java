/*
 * Copyright (c) 2017 Pantheon Technologies s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.remote.rpc.registry.mbeans;

import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;
import static org.opendaylight.controller.remote.rpc.registry.gossip.BucketStoreAccess.Singletons.GET_ALL_BUCKETS;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Address;
import akka.testkit.JavaTestKit;
import com.typesafe.config.ConfigFactory;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.opendaylight.controller.cluster.common.actor.AkkaConfigurationReader;
import org.opendaylight.controller.cluster.raft.TestActorFactory;
import org.opendaylight.controller.remote.rpc.RemoteRpcProviderConfig;
import org.opendaylight.controller.remote.rpc.registry.RoutingTable;
import org.opendaylight.controller.remote.rpc.registry.RpcRegistry;
import org.opendaylight.controller.remote.rpc.registry.gossip.Bucket;

public class RemoteRpcRegistryMXBeanImplTest {

    private RemoteRpcRegistryMXBeanImpl mxBean;
    private TestActorFactory factory;
    private static ActorSystem node1;


    @Mock
    private RpcRegistry rpcRegistry;


    @Before
    public void setUp() throws Exception {
        initMocks(this);

        factory = new TestActorFactory(ActorSystem.create("test"));

        AkkaConfigurationReader reader = ConfigFactory::load;

        RemoteRpcProviderConfig config1 = new RemoteRpcProviderConfig.Builder("memberA").gossipTickInterval("200ms")
                .withConfigReader(reader).build();
        node1 =  ActorSystem.create("opendaylight-rpc", config1.get());
        mxBean = new RemoteRpcRegistryMXBeanImpl(rpcRegistry);

    }

    @Test
    public void testGetGlobalRpc() throws Exception {
        final JavaTestKit testKit = new JavaTestKit(node1);

        final JavaTestKit invoker1;
        final JavaTestKit registrar1;
        invoker1 = new JavaTestKit(node1);
        registrar1 = new JavaTestKit(node1);
        final ActorRef registry1 = node1.actorOf(RpcRegistry.props(new RemoteRpcProviderConfig(node1.settings().config()),
                invoker1.getRef(), registrar1.getRef()));

        registry1.tell(GET_ALL_BUCKETS, testKit.getRef());
        final Map<Address, Bucket<RoutingTable>> buckets = testKit.expectMsgClass(Map.class);

        final Bucket<RoutingTable> localBucket = buckets.values().iterator().next();
        final RoutingTable routingTable = localBucket.getData();

        when(rpcRegistry.getLocalData()).thenReturn(routingTable);
        mxBean.getGlobalRpc();
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