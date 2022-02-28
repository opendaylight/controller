/*
 * Copyright (c) 2022 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.clustering.it.provider;

import com.google.common.collect.Lists;
import java.util.ArrayList;
import java.util.List;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.controller.cluster.databroker.OSGiDOMDataBroker;
import org.opendaylight.controller.cluster.datastore.MemberNode;
import org.opendaylight.controller.cluster.raft.utils.InMemoryJournal;
import org.opendaylight.controller.cluster.raft.utils.InMemorySnapshotStore;
import org.opendaylight.yangtools.yang.model.api.EffectiveModelContext;
import org.opendaylight.yangtools.yang.test.util.YangParserTestUtils;

public class RpcRegistrationTest {
    public static final String PEOPLE_YANG = "/people.yang";
    public static final String CAR_YANG = "/yang/car.yang";
    public static final String CAR_PEOPLE_YANG = "/yang/car-people.yang";
    public static final String CAR_PURCHASE_YANG = "/yang/car-purchase.yang";

    private final List<MemberNode> memberNodes = new ArrayList<>();


    @Before
    public void setUp() {
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
        String name = "RpcRegistrationTest";
        String moduleShardsConfig = "module-shards.conf";
        EffectiveModelContext schema =
                YangParserTestUtils.parseYangResourceDirectory("/yang");

        MemberNode leaderNode1 = MemberNode.builder(memberNodes)
                .akkaConfig("Member1")
                .testName(name)
                .moduleShardsConfig(moduleShardsConfig)
                .schemaContext(schema)
                .build();

        MemberNode newReplicaNode2 = MemberNode.builder(memberNodes)
                .akkaConfig("Member2")
                .testName(name)
                .moduleShardsConfig(moduleShardsConfig)
                .build();

        leaderNode1.waitForMembersUp("member-2");

        MemberNode newReplicaNode3 = MemberNode.builder(memberNodes)
                .akkaConfig("Member3")
                .testName(name)
                .moduleShardsConfig(moduleShardsConfig)
                .build();

        leaderNode1.waitForMembersUp("member-3");
        newReplicaNode2.waitForMembersUp("member-3");

    }
}
