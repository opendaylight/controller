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

import java.util.Optional;
import org.apache.commons.lang3.SerializationUtils;
import org.junit.Test;
import org.opendaylight.controller.md.cluster.datastore.model.TestModel;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifier;
import org.opendaylight.yangtools.yang.data.spi.node.ImmutableNodes;

/**
 * Unit tests for ShardSnapshotState.
 *
 * @author Thomas Pantelis
 */
public class ShardSnapshotStateTest {
    @Test
    public void testSerialization() {
        final var expectedNode = ImmutableNodes.newContainerBuilder()
            .withNodeIdentifier(new NodeIdentifier(TestModel.TEST_QNAME))
            .withChild(ImmutableNodes.leafNode(TestModel.DESC_QNAME, "foo"))
            .build();

        final var expected = new ShardSnapshotState(new MetadataShardDataTreeSnapshot(expectedNode));
        final var cloned = SerializationUtils.clone(expected);

        assertNotNull("getSnapshot is null", cloned.getSnapshot());
        assertEquals("getSnapshot type", MetadataShardDataTreeSnapshot.class, cloned.getSnapshot().getClass());
        assertEquals("getRootNode", Optional.of(expectedNode),
                ((MetadataShardDataTreeSnapshot)cloned.getSnapshot()).getRootNode());
    }
}
