/*
 * Copyright (c) 2015 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.entityownership;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.opendaylight.controller.cluster.entityownership.EntityOwnersModel.ENTITY_OWNERS_PATH;
import static org.opendaylight.controller.cluster.entityownership.EntityOwnersModel.entityEntryWithOwner;
import static org.opendaylight.controller.cluster.entityownership.EntityOwnersModel.entityOwnersWithCandidate;
import static org.opendaylight.controller.cluster.entityownership.EntityOwnersModel.entityPath;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.opendaylight.controller.cluster.access.concepts.MemberName;
import org.opendaylight.controller.cluster.datastore.Shard;
import org.opendaylight.controller.cluster.datastore.ShardDataTree;
import org.opendaylight.controller.md.cluster.datastore.model.SchemaContextHelper;
import org.opendaylight.mdsal.eos.dom.api.DOMEntity;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataValidationFailedException;
import org.opendaylight.yangtools.yang.data.api.schema.tree.TreeType;

/**
 * Unit tests for EntityOwnerChangeListener.
 *
 * @author Thomas Pantelis
 */
public class EntityOwnerChangeListenerTest {
    private static final String LOCAL_MEMBER_NAME = "member-1";
    private static final String REMOTE_MEMBER_NAME1 = "member-2";
    private static final String REMOTE_MEMBER_NAME2 = "member-3";
    private static final String ENTITY_TYPE = "test";
    private static final YangInstanceIdentifier ENTITY_ID1 =
            YangInstanceIdentifier.of(QName.create("test", "2015-08-14", "entity1"));
    private static final YangInstanceIdentifier ENTITY_ID2 =
            YangInstanceIdentifier.of(QName.create("test", "2015-08-14", "entity2"));
    private static final DOMEntity ENTITY1 = new DOMEntity(ENTITY_TYPE, ENTITY_ID1);
    private static final DOMEntity ENTITY2 = new DOMEntity(ENTITY_TYPE, ENTITY_ID2);

    private final Shard mockShard = Mockito.mock(Shard.class);

    private final ShardDataTree shardDataTree = new ShardDataTree(mockShard, SchemaContextHelper.entityOwners(),
        TreeType.OPERATIONAL);
    private final EntityOwnershipListenerSupport mockListenerSupport = mock(EntityOwnershipListenerSupport.class);
    private EntityOwnerChangeListener listener;

    @Before
    public void setup() {
        listener = new EntityOwnerChangeListener(MemberName.forName(LOCAL_MEMBER_NAME), mockListenerSupport);
        listener.init(shardDataTree);
    }

    @Test
    public void testOnDataTreeChanged() throws Exception {
        writeNode(ENTITY_OWNERS_PATH, entityOwnersWithCandidate(ENTITY_TYPE, ENTITY_ID1, LOCAL_MEMBER_NAME));
        writeNode(ENTITY_OWNERS_PATH, entityOwnersWithCandidate(ENTITY_TYPE, ENTITY_ID2, LOCAL_MEMBER_NAME));
        verify(mockListenerSupport, never()).notifyEntityOwnershipListeners(any(DOMEntity.class), anyBoolean(),
                anyBoolean(), anyBoolean());

        // Write local member as owner for entity 1

        writeNode(entityPath(ENTITY_TYPE, ENTITY_ID1), entityEntryWithOwner(ENTITY_ID1, LOCAL_MEMBER_NAME));
        verify(mockListenerSupport).notifyEntityOwnershipListeners(ENTITY1, false, true, true);

        // Add remote member 1 as candidate for entity 1 - listener support should not get notified

        reset(mockListenerSupport);
        writeNode(ENTITY_OWNERS_PATH, entityOwnersWithCandidate(ENTITY_TYPE, ENTITY_ID1, REMOTE_MEMBER_NAME1));
        verify(mockListenerSupport, never()).notifyEntityOwnershipListeners(any(DOMEntity.class), anyBoolean(),
                anyBoolean(), anyBoolean());

        // Change owner to remote member 1 for entity 1

        reset(mockListenerSupport);
        writeNode(entityPath(ENTITY_TYPE, ENTITY_ID1), entityEntryWithOwner(ENTITY_ID1, REMOTE_MEMBER_NAME1));
        verify(mockListenerSupport).notifyEntityOwnershipListeners(ENTITY1, true, false, true);

        // Change owner to remote member 2 for entity 1

        reset(mockListenerSupport);
        writeNode(entityPath(ENTITY_TYPE, ENTITY_ID1), entityEntryWithOwner(ENTITY_ID1, REMOTE_MEMBER_NAME2));
        verify(mockListenerSupport).notifyEntityOwnershipListeners(ENTITY1, false, false, true);

        // Clear the owner for entity 1

        reset(mockListenerSupport);
        writeNode(entityPath(ENTITY_TYPE, ENTITY_ID1), entityEntryWithOwner(ENTITY_ID1, ""));
        verify(mockListenerSupport).notifyEntityOwnershipListeners(ENTITY1, false, false, false);

        // Change owner to the local member for entity 1

        writeNode(entityPath(ENTITY_TYPE, ENTITY_ID1), entityEntryWithOwner(ENTITY_ID1, LOCAL_MEMBER_NAME));
        verify(mockListenerSupport).notifyEntityOwnershipListeners(ENTITY1, false, true, true);

        // Change owner to remote member 2 for entity 2

        reset(mockListenerSupport);
        writeNode(entityPath(ENTITY_TYPE, ENTITY_ID2), entityEntryWithOwner(ENTITY_ID2, REMOTE_MEMBER_NAME1));
        verify(mockListenerSupport).notifyEntityOwnershipListeners(ENTITY2, false, false, true);

        // Change owner to the local member for entity 2

        reset(mockListenerSupport);
        writeNode(entityPath(ENTITY_TYPE, ENTITY_ID2), entityEntryWithOwner(ENTITY_ID2, LOCAL_MEMBER_NAME));
        verify(mockListenerSupport).notifyEntityOwnershipListeners(ENTITY2, false, true, true);

        // Write local member owner for entity 2 again - expect no change

        reset(mockListenerSupport);
        writeNode(entityPath(ENTITY_TYPE, ENTITY_ID2), entityEntryWithOwner(ENTITY_ID2, LOCAL_MEMBER_NAME));
        verify(mockListenerSupport, never()).notifyEntityOwnershipListeners(any(DOMEntity.class), anyBoolean(),
                anyBoolean(), anyBoolean());

        // Clear the owner for entity 2

        reset(mockListenerSupport);
        writeNode(entityPath(ENTITY_TYPE, ENTITY_ID2), entityEntryWithOwner(ENTITY_ID2, null));
        verify(mockListenerSupport).notifyEntityOwnershipListeners(ENTITY2, true, false, false);

        // Clear the owner for entity 2 again - expect no change

        reset(mockListenerSupport);
        writeNode(entityPath(ENTITY_TYPE, ENTITY_ID2), entityEntryWithOwner(ENTITY_ID2, null));
        verify(mockListenerSupport, never()).notifyEntityOwnershipListeners(any(DOMEntity.class), anyBoolean(),
                anyBoolean(), anyBoolean());
    }

    private void writeNode(final YangInstanceIdentifier path, final NormalizedNode<?, ?> node)
            throws DataValidationFailedException {
        AbstractEntityOwnershipTest.writeNode(path, node, shardDataTree);
    }
}
