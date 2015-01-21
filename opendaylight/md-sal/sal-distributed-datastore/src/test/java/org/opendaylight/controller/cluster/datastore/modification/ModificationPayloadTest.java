/*
 * Copyright (c) 2015 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.modification;

import static org.junit.Assert.assertEquals;
import org.apache.commons.lang.SerializationUtils;
import org.junit.Test;
import org.opendaylight.controller.md.cluster.datastore.model.TestModel;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.impl.schema.ImmutableNodes;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.impl.ImmutableContainerNodeBuilder;

/**
 * Unit tests for ModificationPayload.
 *
 * @author Thomas Pantelis
 */
public class ModificationPayloadTest {

    @Test
    public void test() throws Exception {

        YangInstanceIdentifier writePath = TestModel.TEST_PATH;
        NormalizedNode<?, ?> writeData = ImmutableContainerNodeBuilder.create().withNodeIdentifier(
                new YangInstanceIdentifier.NodeIdentifier(TestModel.TEST_QNAME)).
                withChild(ImmutableNodes.leafNode(TestModel.DESC_QNAME, "foo")).build();

        MutableCompositeModification compositeModification = new MutableCompositeModification();
        compositeModification.addModification(new WriteModification(writePath, writeData));

        ModificationPayload payload = new ModificationPayload(compositeModification);

        MutableCompositeModification deserialized = (MutableCompositeModification) payload.getModification();

        assertEquals("getModifications size", 1, deserialized.getModifications().size());
        WriteModification write = (WriteModification)deserialized.getModifications().get(0);
        assertEquals("getPath", writePath, write.getPath());
        assertEquals("getData", writeData, write.getData());

        ModificationPayload cloned =
                (ModificationPayload) SerializationUtils.clone(payload);

        deserialized = (MutableCompositeModification) payload.getModification();

        assertEquals("getModifications size", 1, deserialized.getModifications().size());
        write = (WriteModification)deserialized.getModifications().get(0);
        assertEquals("getPath", writePath, write.getPath());
        assertEquals("getData", writeData, write.getData());
    }
}
