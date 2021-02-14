/*
 * Copyright (c) 2017 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.persisted;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.apache.commons.lang.SerializationUtils;
import org.junit.Test;
import org.opendaylight.controller.md.cluster.datastore.model.TestModel;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.impl.schema.ImmutableNodes;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.impl.ImmutableContainerNodeBuilder;

/**
 * Unit tests for ShardSnapshotState.
 *
 * @author Thomas Pantelis
 */
public class ShardSnapshotStateTest {

    @Test
    public void testSerialization() {
        NormalizedNode expectedNode = ImmutableContainerNodeBuilder.create()
                .withNodeIdentifier(new YangInstanceIdentifier.NodeIdentifier(TestModel.TEST_QNAME))
                .withChild(ImmutableNodes.leafNode(TestModel.DESC_QNAME, "foo")).build();

        ShardSnapshotState expected = new ShardSnapshotState(new MetadataShardDataTreeSnapshot(expectedNode));
        ShardSnapshotState cloned = (ShardSnapshotState) SerializationUtils.clone(expected);

        assertNotNull("getSnapshot is null", cloned.getSnapshot());
        assertEquals("getSnapshot type", MetadataShardDataTreeSnapshot.class, cloned.getSnapshot().getClass());
        assertEquals("getRootNode", expectedNode,
                ((MetadataShardDataTreeSnapshot)cloned.getSnapshot()).getRootNode().get());
    }
}
