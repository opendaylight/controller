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
 * Unit tests for CreateTransactionReply.
 *
 * @author Thomas Pantelis
 */
public class CreateTransactionReplyTest {

    @Test
    public void testSerialization() {
        CreateTransactionReply expected = new CreateTransactionReply("txPath", "txId", DataStoreVersions.CURRENT_VERSION);

        Object serialized = expected.toSerializable();
        assertEquals("Serialized type", CreateTransactionReply.class, serialized.getClass());

        CreateTransactionReply actual = CreateTransactionReply.fromSerializable(
                SerializationUtils.clone((Serializable) serialized));
        assertEquals("getTransactionId", expected.getTransactionId(), actual.getTransactionId());
        assertEquals("getTransactionPath", expected.getTransactionPath(), actual.getTransactionPath());
        assertEquals("getVersion", DataStoreVersions.CURRENT_VERSION, actual.getVersion());
    }

    @Test
    public void testSerializationWithPreBoronVersion() {
        CreateTransactionReply expected = new CreateTransactionReply("txPath", "txId", DataStoreVersions.LITHIUM_VERSION);

        Object serialized = expected.toSerializable();
        assertEquals("Serialized type", ShardTransactionMessages.CreateTransactionReply.class, serialized.getClass());

        CreateTransactionReply actual = CreateTransactionReply.fromSerializable(
                SerializationUtils.clone((Serializable) serialized));
        assertEquals("getTransactionId", expected.getTransactionId(), actual.getTransactionId());
        assertEquals("getTransactionPath", expected.getTransactionPath(), actual.getTransactionPath());
        assertEquals("getVersion", DataStoreVersions.LITHIUM_VERSION, actual.getVersion());
    }

    @Test
    public void testIsSerializedType() {
        assertEquals("isSerializedType", true, CreateTransactionReply.isSerializedType(
                ShardTransactionMessages.CreateTransactionReply.newBuilder().setTransactionActorPath("")
                    .setTransactionId("").setMessageVersion(4).build()));

        assertEquals("isSerializedType", true, CreateTransactionReply.isSerializedType(new CreateTransactionReply()));
        assertEquals("isSerializedType", false, CreateTransactionReply.isSerializedType(new Object()));
    }
}
