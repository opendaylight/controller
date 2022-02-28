/*
 * Copyright (c) 2022 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.clustering.it.provider;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Address;
import akka.actor.AddressFromURIString;
import akka.cluster.Cluster;
import com.google.common.collect.Lists;
import java.util.ArrayList;
import java.util.List;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.opendaylight.clustering.it.karaf.cli.InstanceIdentifierSupport;
import org.opendaylight.controller.cluster.databroker.OSGiDOMDataBroker;
import org.opendaylight.controller.cluster.datastore.MemberNode;
import org.opendaylight.controller.cluster.raft.utils.InMemoryJournal;
import org.opendaylight.controller.cluster.raft.utils.InMemorySnapshotStore;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.binding.api.RpcConsumerRegistry;
import org.opendaylight.mdsal.binding.api.RpcProviderService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.sal.clustering.it.car.purchase.rev140818.CarPurchaseService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.sal.clustering.it.people.rev140818.PeopleService;
import org.opendaylight.yangtools.yang.model.api.EffectiveModelContext;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.opendaylight.yangtools.yang.test.util.YangParserTestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RpcRegistrationTest {

    private static final Logger LOG = LoggerFactory.getLogger(RpcRegistrationTest.class);

    private static final Address MEMBER_1_ADDRESS = AddressFromURIString.parse(
            "akka://cluster-test@127.0.0.1:2558");

    private final List<MemberNode> memberNodes = new ArrayList<>();
    private SchemaContext schemaContext;

    private static ActorSystem node1;
    private static ActorSystem node2;
    private static ActorSystem node3;

    private ActorRef invoker1;
    private ActorRef invoker2;
    private ActorRef invoker3;

    private RpcProviderService rpcProviderService;
    private RpcConsumerRegistry rpcConsumerRegistry;
    private InstanceIdentifierSupport iidSupport;

    private PeopleService peopleService;
    private CarPurchaseService carPurchaseService;


    @Before
    public void setUp() {
        schemaContext = YangParserTestUtils.parseYangResourceDirectory("/yang");
        Config config = ConfigFactory.load();

        node1 = ActorSystem.create("cluster-test", config.getConfig("Member1"));
        Cluster.get(node1).join(MEMBER_1_ADDRESS);

        node2 = ActorSystem.create("cluster-test", config.getConfig("Member2"));
        Cluster.get(node2).join(MEMBER_1_ADDRESS);

        node3 = ActorSystem.create("cluster-test", config.getConfig("Member3"));
        Cluster.get(node3).join(MEMBER_1_ADDRESS);

//        carPurchaseService =
//        peopleService =

//        rpcProviderService.registerRpcImplementation(PeopleService.class, peopleService);

        invoker1 = node1.actorOf(InvokerActor.props(rpcConsumerRegistry, iidSupport));
        invoker2 = node2.actorOf(InvokerActor.props(rpcConsumerRegistry, iidSupport));
        invoker3 = node3.actorOf(InvokerActor.props(rpcConsumerRegistry, iidSupport));

        InMemoryJournal.clear();
        InMemorySnapshotStore.clear();
    }

    @After
    public void tearDown() {
        for (MemberNode m : Lists.reverse(memberNodes)) {
            m.cleanup();
        }
        memberNodes.clear();
    }

    @Test
    public void test() throws Exception {
        // TODO
    }
}
