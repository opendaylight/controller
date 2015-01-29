/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.datastore;

import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang.SerializationUtils;
import org.junit.Test;
import org.opendaylight.controller.cluster.datastore.modification.MutableCompositeModification;
import org.opendaylight.controller.cluster.datastore.modification.WriteModification;
import org.opendaylight.controller.cluster.raft.ReplicatedLogEntry;
import org.opendaylight.controller.cluster.raft.ReplicatedLogImplEntry;
import org.opendaylight.controller.cluster.raft.messages.AppendEntries;
import org.opendaylight.controller.cluster.raft.protobuff.client.messages.CompositeModificationByteStringPayload;
import org.opendaylight.controller.md.cluster.datastore.model.TestModel;
import org.opendaylight.yangtools.yang.data.impl.schema.ImmutableNodes;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;

import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;

@Deprecated
public class CompositeModificationByteStringPayloadTest {

    private static final SchemaContext SCHEMA_CONTEXT = TestModel.createTestContext();

    @Test
    public void testSerialization(){
        WriteModification writeModification =
                new WriteModification(TestModel.TEST_PATH, ImmutableNodes
                        .containerNode(TestModel.TEST_QNAME));

        MutableCompositeModification compositeModification =
                new MutableCompositeModification();

        compositeModification.addModification(writeModification);

        CompositeModificationByteStringPayload compositeModificationByteStringPayload
                = new CompositeModificationByteStringPayload(compositeModification.toSerializable());

        byte[] bytes = SerializationUtils.serialize(compositeModificationByteStringPayload);

        Object deserialize = SerializationUtils.deserialize(bytes);

        assertTrue(deserialize instanceof CompositeModificationByteStringPayload);

    }

    @Test
    public void testAppendEntries(){
        List<ReplicatedLogEntry> entries = new ArrayList<>();

        WriteModification writeModification = new WriteModification(TestModel.OUTER_LIST_PATH,
                ImmutableNodes.mapNodeBuilder(TestModel.OUTER_LIST_QNAME).build());

        MutableCompositeModification compositeModification = new MutableCompositeModification();

        compositeModification.addModification(writeModification);

        CompositeModificationByteStringPayload payload =
                new CompositeModificationByteStringPayload(compositeModification.toSerializable());

        payload.clearModificationReference();

        entries.add(new ReplicatedLogImplEntry(0, 1, payload));

        assertNotNull(new AppendEntries(10, "foobar", 10, 10, entries, 10, -1).toSerializable());
    }
}
