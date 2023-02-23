/*
 * Copyright (c) 2014, 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.modification;

import static org.junit.Assert.assertEquals;

import java.util.Optional;
import org.apache.commons.lang3.SerializationUtils;
import org.junit.Test;
import org.opendaylight.controller.md.cluster.datastore.model.TestModel;
import org.opendaylight.mdsal.dom.spi.store.DOMStoreReadWriteTransaction;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifier;
import org.opendaylight.yangtools.yang.data.impl.schema.Builders;
import org.opendaylight.yangtools.yang.data.impl.schema.ImmutableNodes;

public class WriteModificationTest extends AbstractModificationTest {
    @Test
    public void testApply() throws Exception {
        //Write something into the datastore
        DOMStoreReadWriteTransaction writeTransaction = store.newReadWriteTransaction();
        WriteModification writeModification = new WriteModification(TestModel.TEST_PATH, TEST_CONTAINER);
        writeModification.apply(writeTransaction);
        commitTransaction(writeTransaction);

        //Check if it's in the datastore
        assertEquals(Optional.of(TEST_CONTAINER), readData(TestModel.TEST_PATH));
    }

    @Test
    public void testSerialization() {
        WriteModification expected = new WriteModification(TestModel.TEST_PATH, Builders.containerBuilder()
            .withNodeIdentifier(new NodeIdentifier(TestModel.TEST_QNAME))
            .withChild(ImmutableNodes.leafNode(TestModel.DESC_QNAME, "foo"))
            .build());

        WriteModification clone = SerializationUtils.clone(expected);
        assertEquals("getPath", expected.getPath(), clone.getPath());
        assertEquals("getData", expected.getData(), clone.getData());
    }
}
