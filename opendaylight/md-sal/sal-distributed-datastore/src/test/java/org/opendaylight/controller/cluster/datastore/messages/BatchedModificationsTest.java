/*
 * Copyright (c) 2015 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.messages;

import static org.junit.Assert.assertEquals;
import java.io.Serializable;
import org.apache.commons.lang.SerializationUtils;
import org.junit.Test;
import org.opendaylight.controller.cluster.datastore.DataStoreVersions;
import org.opendaylight.controller.cluster.datastore.modification.DeleteModification;
import org.opendaylight.controller.cluster.datastore.modification.MergeModification;
import org.opendaylight.controller.cluster.datastore.modification.WriteModification;
import org.opendaylight.controller.md.cluster.datastore.model.TestModel;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.impl.schema.ImmutableNodes;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.impl.ImmutableContainerNodeBuilder;

/**
 * Unit tests for BatchedModifications.
 *
 * @author Thomas Pantelis
 */
public class BatchedModificationsTest {

    @Test
    public void testSerialization() {
        YangInstanceIdentifier writePath = TestModel.TEST_PATH;
        NormalizedNode<?, ?> writeData = ImmutableContainerNodeBuilder.create().withNodeIdentifier(
                new YangInstanceIdentifier.NodeIdentifier(TestModel.TEST_QNAME)).
                withChild(ImmutableNodes.leafNode(TestModel.DESC_QNAME, "foo")).build();

        YangInstanceIdentifier mergePath = TestModel.OUTER_LIST_PATH;
        NormalizedNode<?, ?> mergeData = ImmutableContainerNodeBuilder.create().withNodeIdentifier(
                new YangInstanceIdentifier.NodeIdentifier(TestModel.OUTER_LIST_QNAME)).build();

        YangInstanceIdentifier deletePath = TestModel.TEST_PATH;

        BatchedModifications batched = new BatchedModifications(DataStoreVersions.CURRENT_VERSION);
        batched.addModification(new WriteModification(writePath, writeData));
        batched.addModification(new MergeModification(mergePath, mergeData));
        batched.addModification(new DeleteModification(deletePath));

        BatchedModifications clone = (BatchedModifications) SerializationUtils.clone(
                (Serializable) batched.toSerializable());

        assertEquals("getVersion", DataStoreVersions.CURRENT_VERSION, clone.getVersion());

        assertEquals("getModifications size", 3, clone.getModifications().size());

        WriteModification write = (WriteModification)clone.getModifications().get(0);
        assertEquals("getVersion", DataStoreVersions.CURRENT_VERSION, write.getVersion());
        assertEquals("getPath", writePath, write.getPath());
        assertEquals("getData", writeData, write.getData());

        MergeModification merge = (MergeModification)clone.getModifications().get(1);
        assertEquals("getVersion", DataStoreVersions.CURRENT_VERSION, merge.getVersion());
        assertEquals("getPath", mergePath, merge.getPath());
        assertEquals("getData", mergeData, merge.getData());

        DeleteModification delete = (DeleteModification)clone.getModifications().get(2);
        assertEquals("getVersion", DataStoreVersions.CURRENT_VERSION, delete.getVersion());
        assertEquals("getPath", deletePath, delete.getPath());
    }

    @Test
    public void testBatchedModificationsReplySerialization() {
        SerializationUtils.clone(BatchedModificationsReply.INSTANCE);
    }
}
