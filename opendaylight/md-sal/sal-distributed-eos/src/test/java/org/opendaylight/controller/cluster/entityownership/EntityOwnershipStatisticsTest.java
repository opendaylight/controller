/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.entityownership;

import static org.junit.Assert.assertEquals;
import static org.opendaylight.controller.cluster.entityownership.EntityOwnersModel.ENTITY_OWNERS_PATH;
import static org.opendaylight.controller.cluster.entityownership.EntityOwnersModel.entityEntryWithOwner;
import static org.opendaylight.controller.cluster.entityownership.EntityOwnersModel.entityOwnersWithCandidate;
import static org.opendaylight.controller.cluster.entityownership.EntityOwnersModel.entityPath;

import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.opendaylight.controller.cluster.datastore.AbstractActorTest;
import org.opendaylight.controller.cluster.datastore.Shard;
import org.opendaylight.controller.cluster.datastore.ShardDataTree;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataValidationFailedException;
import org.opendaylight.yangtools.yang.data.api.schema.tree.TreeType;

public class EntityOwnershipStatisticsTest extends AbstractActorTest {
    private static final String LOCAL_MEMBER_NAME = "member-1";
    private static final String REMOTE_MEMBER_NAME1 = "member-2";
    private static final String REMOTE_MEMBER_NAME2 = "member-3";
    private static final String ENTITY_TYPE = "test";
    private static final YangInstanceIdentifier ENTITY_ID1 =
            YangInstanceIdentifier.of(QName.create("test", "2015-08-14", "entity1"));
    private static final YangInstanceIdentifier ENTITY_ID2 =
            YangInstanceIdentifier.of(QName.create("test", "2015-08-14", "entity2"));

    private final Shard mockShard = Mockito.mock(Shard.class);

    private final ShardDataTree shardDataTree = new ShardDataTree(mockShard, EOSTestUtils.SCHEMA_CONTEXT,
        TreeType.OPERATIONAL);
    private EntityOwnershipStatistics ownershipStatistics;

    @Before
    public void setup() {
        ownershipStatistics = new EntityOwnershipStatistics();
        ownershipStatistics.init(shardDataTree);
    }

    @Test
    public void testOnDataTreeChanged() throws Exception {
        writeNode(ENTITY_OWNERS_PATH, entityOwnersWithCandidate(ENTITY_TYPE, ENTITY_ID1, LOCAL_MEMBER_NAME));
        writeNode(ENTITY_OWNERS_PATH, entityOwnersWithCandidate(ENTITY_TYPE, ENTITY_ID2, LOCAL_MEMBER_NAME));

        // Write local member as owner for entity 1

        writeNode(entityPath(ENTITY_TYPE, ENTITY_ID1), entityEntryWithOwner(ENTITY_ID1, LOCAL_MEMBER_NAME));
        assertStatistics(ownershipStatistics.all(), LOCAL_MEMBER_NAME, 1L);

        // Add remote member 1 as candidate for entity 1 - ownershipStatistics support should not get notified

        writeNode(ENTITY_OWNERS_PATH, entityOwnersWithCandidate(ENTITY_TYPE, ENTITY_ID1, REMOTE_MEMBER_NAME1));
        assertStatistics(ownershipStatistics.all(), LOCAL_MEMBER_NAME, 1L);

        // Change owner to remote member 1 for entity 1

        writeNode(entityPath(ENTITY_TYPE, ENTITY_ID1), entityEntryWithOwner(ENTITY_ID1, REMOTE_MEMBER_NAME1));
        Map<String, Map<String, Long>> statistics = ownershipStatistics.all();
        assertStatistics(statistics, LOCAL_MEMBER_NAME, 0L);
        assertStatistics(statistics, REMOTE_MEMBER_NAME1, 1L);

        // Change owner to remote member 2 for entity 1

        writeNode(entityPath(ENTITY_TYPE, ENTITY_ID1), entityEntryWithOwner(ENTITY_ID1, REMOTE_MEMBER_NAME2));
        statistics = ownershipStatistics.all();
        assertStatistics(statistics, LOCAL_MEMBER_NAME, 0L);
        assertStatistics(statistics, REMOTE_MEMBER_NAME1, 0L);
        assertStatistics(statistics, REMOTE_MEMBER_NAME2, 1L);

        // Clear the owner for entity 1

        writeNode(entityPath(ENTITY_TYPE, ENTITY_ID1), entityEntryWithOwner(ENTITY_ID1, ""));
        statistics = ownershipStatistics.all();
        assertStatistics(statistics, LOCAL_MEMBER_NAME, 0L);
        assertStatistics(statistics, REMOTE_MEMBER_NAME1, 0L);
        assertStatistics(statistics, REMOTE_MEMBER_NAME2, 0L);

        // Change owner to the local member for entity 1

        writeNode(entityPath(ENTITY_TYPE, ENTITY_ID1), entityEntryWithOwner(ENTITY_ID1, LOCAL_MEMBER_NAME));
        statistics = ownershipStatistics.all();
        assertStatistics(statistics, LOCAL_MEMBER_NAME, 1L);
        assertStatistics(statistics, REMOTE_MEMBER_NAME1, 0L);
        assertStatistics(statistics, REMOTE_MEMBER_NAME2, 0L);

        // Change owner to remote member 1 for entity 2

        writeNode(entityPath(ENTITY_TYPE, ENTITY_ID2), entityEntryWithOwner(ENTITY_ID2, REMOTE_MEMBER_NAME1));
        statistics = ownershipStatistics.all();
        assertStatistics(statistics, LOCAL_MEMBER_NAME, 1L);
        assertStatistics(statistics, REMOTE_MEMBER_NAME1, 1L);
        assertStatistics(statistics, REMOTE_MEMBER_NAME2, 0L);

        // Change owner to the local member for entity 2

        writeNode(entityPath(ENTITY_TYPE, ENTITY_ID2), entityEntryWithOwner(ENTITY_ID2, LOCAL_MEMBER_NAME));
        statistics = ownershipStatistics.all();
        assertStatistics(statistics, LOCAL_MEMBER_NAME, 2L);
        assertStatistics(statistics, REMOTE_MEMBER_NAME1, 0L);
        assertStatistics(statistics, REMOTE_MEMBER_NAME2, 0L);

        // Write local member owner for entity 2 again - expect no change
        writeNode(entityPath(ENTITY_TYPE, ENTITY_ID2), entityEntryWithOwner(ENTITY_ID2, LOCAL_MEMBER_NAME));
        statistics = ownershipStatistics.all();
        assertStatistics(statistics, LOCAL_MEMBER_NAME, 2L);
        assertStatistics(statistics, REMOTE_MEMBER_NAME1, 0L);
        assertStatistics(statistics, REMOTE_MEMBER_NAME2, 0L);

        // Clear the owner for entity 2
        writeNode(entityPath(ENTITY_TYPE, ENTITY_ID2), entityEntryWithOwner(ENTITY_ID2, ""));
        statistics = ownershipStatistics.all();
        assertStatistics(statistics, LOCAL_MEMBER_NAME, 1L);
        assertStatistics(statistics, REMOTE_MEMBER_NAME1, 0L);
        assertStatistics(statistics, REMOTE_MEMBER_NAME2, 0L);

        // Clear the owner for entity 2 again - expect no change

        writeNode(entityPath(ENTITY_TYPE, ENTITY_ID2), entityEntryWithOwner(ENTITY_ID2, ""));
        statistics = ownershipStatistics.all();
        assertStatistics(statistics, LOCAL_MEMBER_NAME, 1L);
        assertStatistics(statistics, REMOTE_MEMBER_NAME1, 0L);
        assertStatistics(statistics, REMOTE_MEMBER_NAME2, 0L);

    }

    private static void assertStatistics(final Map<String, Map<String, Long>> statistics, final String memberName,
            final long val) {
        assertEquals(val, statistics.get(ENTITY_TYPE).get(memberName).longValue());
    }

    private void writeNode(final YangInstanceIdentifier path, final NormalizedNode<?, ?> node)
            throws DataValidationFailedException {
        AbstractEntityOwnershipTest.writeNode(path, node, shardDataTree);
    }
}
