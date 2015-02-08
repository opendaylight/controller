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
import org.opendaylight.controller.md.cluster.datastore.model.TestModel;
import org.opendaylight.controller.protobuff.messages.common.NormalizedNodeMessages.InstanceIdentifier;
import org.opendaylight.controller.protobuff.messages.transaction.ShardTransactionMessages;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;

/**
 * Unit tests for DeleteData.
 *
 * @author Thomas Pantelis
 */
public class DeleteDataTest {

    @Test
    public void testSerialization() {
        YangInstanceIdentifier path = TestModel.TEST_PATH;

        DeleteData expected = new DeleteData(path, DataStoreVersions.CURRENT_VERSION);

        Object serialized = expected.toSerializable();
        assertEquals("Serialized type", DeleteData.class, serialized.getClass());
        assertEquals("Version", DataStoreVersions.CURRENT_VERSION, ((DeleteData)serialized).getVersion());

        Object clone = SerializationUtils.clone((Serializable) serialized);
        DeleteData actual = DeleteData.fromSerializable(clone);
        assertEquals("getVersion", DataStoreVersions.CURRENT_VERSION, actual.getVersion());
        assertEquals("getPath", expected.getPath(), actual.getPath());
    }

    @Test
    public void testIsSerializedType() {
        assertEquals("isSerializedType", true, DeleteData.isSerializedType(
                ShardTransactionMessages.DeleteData.newBuilder()
                    .setInstanceIdentifierPathArguments(InstanceIdentifier.getDefaultInstance()).build()));
        assertEquals("isSerializedType", true,
                DeleteData.isSerializedType(new DeleteData()));
        assertEquals("isSerializedType", false, DeleteData.isSerializedType(new Object()));
    }

    /**
     * Tests backwards compatible serialization/deserialization of a DeleteData message with the
     * base and R1 Helium versions, which used the protobuff DeleteData message.
     */
    @Test
    public void testSerializationWithHeliumR1Version() throws Exception {
        YangInstanceIdentifier path = TestModel.TEST_PATH;

        DeleteData expected = new DeleteData(path, DataStoreVersions.HELIUM_1_VERSION);

        Object serialized = expected.toSerializable();
        assertEquals("Serialized type", ShardTransactionMessages.DeleteData.class, serialized.getClass());

        DeleteData actual = DeleteData.fromSerializable(SerializationUtils.clone((Serializable) serialized));
        assertEquals("getPath", expected.getPath(), actual.getPath());
    }
}
