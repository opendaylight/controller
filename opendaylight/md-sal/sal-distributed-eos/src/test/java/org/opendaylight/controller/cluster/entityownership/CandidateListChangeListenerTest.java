/*
 * Copyright (c) 2015 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.entityownership;

import static org.junit.Assert.assertEquals;
import static org.opendaylight.controller.cluster.entityownership.EntityOwnersModel.ENTITY_OWNERS_PATH;
import static org.opendaylight.controller.cluster.entityownership.EntityOwnersModel.candidatePath;
import static org.opendaylight.controller.cluster.entityownership.EntityOwnersModel.entityOwnersWithCandidate;
import static org.opendaylight.controller.cluster.entityownership.EntityOwnersModel.entityPath;

import akka.testkit.javadsl.TestKit;
import com.google.common.collect.ImmutableSet;
import java.time.Duration;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.opendaylight.controller.cluster.datastore.AbstractActorTest;
import org.opendaylight.controller.cluster.datastore.Shard;
import org.opendaylight.controller.cluster.datastore.ShardDataTree;
import org.opendaylight.controller.cluster.entityownership.messages.CandidateAdded;
import org.opendaylight.controller.cluster.entityownership.messages.CandidateRemoved;
import org.opendaylight.controller.md.cluster.datastore.model.SchemaContextHelper;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataValidationFailedException;
import org.opendaylight.yangtools.yang.data.api.schema.tree.TreeType;

/**
 * Unit tests for CandidateListChangeListener.
 *
 * @author Thomas Pantelis
 */
public class CandidateListChangeListenerTest extends AbstractActorTest {
    private static final String ENTITY_TYPE = "test";
    private static final YangInstanceIdentifier ENTITY_ID1 =
            YangInstanceIdentifier.of(QName.create("test", "2015-08-14", "entity1"));
    private static final YangInstanceIdentifier ENTITY_ID2 =
            YangInstanceIdentifier.of(QName.create("test", "2015-08-14", "entity2"));

    private ShardDataTree shardDataTree;

    @Mock
    private Shard mockShard;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        shardDataTree = new ShardDataTree(mockShard, SchemaContextHelper.entityOwners(), TreeType.OPERATIONAL);
    }

    @Test
    public void testOnDataTreeChanged() throws Exception {
        TestKit kit = new TestKit(getSystem());

        new CandidateListChangeListener(kit.getRef(), "test").init(shardDataTree);

        String memberName1 = "member-1";
        writeNode(ENTITY_OWNERS_PATH, entityOwnersWithCandidate(ENTITY_TYPE, ENTITY_ID1, memberName1));

        CandidateAdded candidateAdded = kit.expectMsgClass(CandidateAdded.class);
        assertEquals("getEntityId", entityPath(ENTITY_TYPE, ENTITY_ID1), candidateAdded.getEntityPath());
        assertEquals("getNewCandidate", memberName1, candidateAdded.getNewCandidate());
        assertEquals("getAllCandidates", ImmutableSet.of(memberName1),
                ImmutableSet.copyOf(candidateAdded.getAllCandidates()));

        writeNode(ENTITY_OWNERS_PATH, entityOwnersWithCandidate(ENTITY_TYPE, ENTITY_ID1, memberName1));
        kit.expectNoMessage(Duration.ofMillis(500));

        String memberName2 = "member-2";
        writeNode(ENTITY_OWNERS_PATH, entityOwnersWithCandidate(ENTITY_TYPE, ENTITY_ID1, memberName2));

        candidateAdded = kit.expectMsgClass(CandidateAdded.class);
        assertEquals("getEntityId", entityPath(ENTITY_TYPE, ENTITY_ID1), candidateAdded.getEntityPath());
        assertEquals("getNewCandidate", memberName2, candidateAdded.getNewCandidate());
        assertEquals("getAllCandidates", ImmutableSet.of(memberName1, memberName2),
                ImmutableSet.copyOf(candidateAdded.getAllCandidates()));

        writeNode(ENTITY_OWNERS_PATH, entityOwnersWithCandidate(ENTITY_TYPE, ENTITY_ID2, memberName1));

        candidateAdded = kit.expectMsgClass(CandidateAdded.class);
        assertEquals("getEntityId", entityPath(ENTITY_TYPE, ENTITY_ID2), candidateAdded.getEntityPath());
        assertEquals("getNewCandidate", memberName1, candidateAdded.getNewCandidate());
        assertEquals("getAllCandidates", ImmutableSet.of(memberName1),
                ImmutableSet.copyOf(candidateAdded.getAllCandidates()));

        deleteNode(candidatePath(ENTITY_TYPE, ENTITY_ID1, memberName1));

        CandidateRemoved candidateRemoved = kit.expectMsgClass(CandidateRemoved.class);
        assertEquals("getEntityId", entityPath(ENTITY_TYPE, ENTITY_ID1), candidateRemoved.getEntityPath());
        assertEquals("getRemovedCandidate", memberName1, candidateRemoved.getRemovedCandidate());
        assertEquals("getRemainingCandidates", ImmutableSet.of(memberName2),
                ImmutableSet.copyOf(candidateRemoved.getRemainingCandidates()));

        deleteNode(candidatePath(ENTITY_TYPE, ENTITY_ID1, memberName2));

        candidateRemoved = kit.expectMsgClass(CandidateRemoved.class);
        assertEquals("getEntityId", entityPath(ENTITY_TYPE, ENTITY_ID1), candidateRemoved.getEntityPath());
        assertEquals("getRemovedCandidate", memberName2, candidateRemoved.getRemovedCandidate());
        assertEquals("getRemainingCandidates", ImmutableSet.of(),
                ImmutableSet.copyOf(candidateRemoved.getRemainingCandidates()));
    }

    private void writeNode(final YangInstanceIdentifier path, final NormalizedNode<?, ?> node)
            throws DataValidationFailedException {
        AbstractEntityOwnershipTest.writeNode(path, node, shardDataTree);
    }

    private void deleteNode(final YangInstanceIdentifier path) throws DataValidationFailedException {
        AbstractEntityOwnershipTest.deleteNode(path, shardDataTree);
    }
}
