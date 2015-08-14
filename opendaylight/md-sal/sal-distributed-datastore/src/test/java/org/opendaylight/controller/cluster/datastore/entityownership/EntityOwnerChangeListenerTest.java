/*
 * Copyright (c) 2015 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.entityownership;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.opendaylight.controller.cluster.datastore.entityownership.EntityOwnersModel.ENTITY_OWNERS_PATH;
import static org.opendaylight.controller.cluster.datastore.entityownership.EntityOwnersModel.entityEntryWithOwner;
import static org.opendaylight.controller.cluster.datastore.entityownership.EntityOwnersModel.entityOwnersWithCandidate;
import static org.opendaylight.controller.cluster.datastore.entityownership.EntityOwnersModel.entityPath;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.controller.cluster.datastore.ShardDataTree;
import org.opendaylight.controller.md.cluster.datastore.model.SchemaContextHelper;
import org.opendaylight.controller.md.sal.common.api.clustering.Entity;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataValidationFailedException;

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
    private static final Entity ENTITY1 = new Entity(ENTITY_TYPE, ENTITY_ID1);
    private static final Entity ENTITY2 = new Entity(ENTITY_TYPE, ENTITY_ID2);

    private final ShardDataTree shardDataTree = new ShardDataTree(SchemaContextHelper.entityOwners());
    private final EntityOwnershipListenerSupport mockListenerSupport = mock(EntityOwnershipListenerSupport.class);
    private EntityOwnerChangeListener listener;

    @Before
    public void setup() {
        listener = new EntityOwnerChangeListener(LOCAL_MEMBER_NAME, mockListenerSupport);
        listener.init(shardDataTree);
    }

    @Test
    public void testOnDataTreeChanged() throws Exception {
        writeNode(ENTITY_OWNERS_PATH, entityOwnersWithCandidate(ENTITY_TYPE, ENTITY_ID1, LOCAL_MEMBER_NAME));
        writeNode(ENTITY_OWNERS_PATH, entityOwnersWithCandidate(ENTITY_TYPE, ENTITY_ID2, LOCAL_MEMBER_NAME));
        verify(mockListenerSupport, never()).notifyEntityOwnershipListeners(any(Entity.class), anyBoolean(), anyBoolean());

        writeNode(entityPath(ENTITY_TYPE, ENTITY_ID1), entityEntryWithOwner(ENTITY_ID1, LOCAL_MEMBER_NAME));
        verify(mockListenerSupport).notifyEntityOwnershipListeners(ENTITY1, false, true);

        reset(mockListenerSupport);
        writeNode(ENTITY_OWNERS_PATH, entityOwnersWithCandidate(ENTITY_TYPE, ENTITY_ID1, REMOTE_MEMBER_NAME1));
        verify(mockListenerSupport, never()).notifyEntityOwnershipListeners(any(Entity.class), anyBoolean(), anyBoolean());

        reset(mockListenerSupport);
        writeNode(entityPath(ENTITY_TYPE, ENTITY_ID1), entityEntryWithOwner(ENTITY_ID1, REMOTE_MEMBER_NAME1));
        verify(mockListenerSupport).notifyEntityOwnershipListeners(ENTITY1, true, false);

        reset(mockListenerSupport);
        writeNode(entityPath(ENTITY_TYPE, ENTITY_ID1), entityEntryWithOwner(ENTITY_ID1, REMOTE_MEMBER_NAME2));
        verify(mockListenerSupport, never()).notifyEntityOwnershipListeners(any(Entity.class), anyBoolean(), anyBoolean());

        writeNode(entityPath(ENTITY_TYPE, ENTITY_ID1), entityEntryWithOwner(ENTITY_ID1, LOCAL_MEMBER_NAME));
        verify(mockListenerSupport).notifyEntityOwnershipListeners(ENTITY1, false, true);

        reset(mockListenerSupport);
        writeNode(entityPath(ENTITY_TYPE, ENTITY_ID2), entityEntryWithOwner(ENTITY_ID2, REMOTE_MEMBER_NAME1));
        verify(mockListenerSupport, never()).notifyEntityOwnershipListeners(any(Entity.class), anyBoolean(), anyBoolean());

        reset(mockListenerSupport);
        writeNode(entityPath(ENTITY_TYPE, ENTITY_ID2), entityEntryWithOwner(ENTITY_ID2, LOCAL_MEMBER_NAME));
        verify(mockListenerSupport).notifyEntityOwnershipListeners(ENTITY2, false, true);
    }

    private void writeNode(YangInstanceIdentifier path, NormalizedNode<?, ?> node) throws DataValidationFailedException {
        AbstractEntityOwnershipTest.writeNode(path, node, shardDataTree);
    }
}
