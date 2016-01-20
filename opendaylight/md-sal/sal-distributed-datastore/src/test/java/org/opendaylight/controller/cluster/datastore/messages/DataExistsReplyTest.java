/*
 * Copyright (c) 2016 Brocade Communications Systems, Inc. and others.  All rights reserved.
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
import org.opendaylight.controller.protobuff.messages.transaction.ShardTransactionMessages;

/**
 * Unit tests for DataExistsReply.
 *
 * @author Thomas Pantelis
 */
public class DataExistsReplyTest {

    @Test
    public void testSerialization() {
        DataExistsReply expected = new DataExistsReply(true, DataStoreVersions.CURRENT_VERSION);

        Object serialized = expected.toSerializable();
        assertEquals("Serialized type", DataExistsReply.class, serialized.getClass());

        DataExistsReply actual = DataExistsReply.fromSerializable(SerializationUtils.clone((Serializable) serialized));
        assertEquals("exists", expected.exists(), actual.exists());
        assertEquals("getVersion", DataStoreVersions.CURRENT_VERSION, actual.getVersion());
    }

    @Test
    public void testSerializationWithPreBoronVersion() {
        DataExistsReply expected = new DataExistsReply(true, DataStoreVersions.LITHIUM_VERSION);

        Object serialized = expected.toSerializable();
        assertEquals("Serialized type", ShardTransactionMessages.DataExistsReply.class, serialized.getClass());

        DataExistsReply actual = DataExistsReply.fromSerializable(SerializationUtils.clone((Serializable) serialized));
        assertEquals("exists", expected.exists(), actual.exists());
        assertEquals("getVersion", DataStoreVersions.LITHIUM_VERSION, actual.getVersion());
    }

    @Test
    public void testIsSerializedType() {
        assertEquals("isSerializedType", true, DataExistsReply.isSerializedType(
                ShardTransactionMessages.DataExistsReply.newBuilder().setExists(true).build()));

        assertEquals("isSerializedType", true, DataExistsReply.isSerializedType(new DataExistsReply()));
        assertEquals("isSerializedType", false, DataExistsReply.isSerializedType(new Object()));
    }
}
