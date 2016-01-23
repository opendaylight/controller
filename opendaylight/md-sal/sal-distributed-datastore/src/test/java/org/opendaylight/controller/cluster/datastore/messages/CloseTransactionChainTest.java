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
import org.opendaylight.controller.protobuff.messages.transaction.ShardTransactionChainMessages;

/**
 * Unit tests for CloseTransactionChain.
 *
 * @author Thomas Pantelis
 */
public class CloseTransactionChainTest {

    @Test
    public void testSerialization() {
        CloseTransactionChain expected = new CloseTransactionChain("txId", DataStoreVersions.CURRENT_VERSION);

        Object serialized = expected.toSerializable();
        assertEquals("Serialized type", CloseTransactionChain.class, serialized.getClass());

        CloseTransactionChain actual = CloseTransactionChain.fromSerializable(
                SerializationUtils.clone((Serializable) serialized));
        assertEquals("getTransactionChainId", expected.getTransactionChainId(), actual.getTransactionChainId());
        assertEquals("getVersion", DataStoreVersions.CURRENT_VERSION, actual.getVersion());
    }

    @Test
    public void testSerializationWithPreBoronVersion() {
        CloseTransactionChain expected = new CloseTransactionChain("txId", DataStoreVersions.LITHIUM_VERSION);

        Object serialized = expected.toSerializable();
        assertEquals("Serialized type", ShardTransactionChainMessages.CloseTransactionChain.class, serialized.getClass());

        CloseTransactionChain actual = CloseTransactionChain.fromSerializable(
                SerializationUtils.clone((Serializable) serialized));
        assertEquals("getTransactionChainId", expected.getTransactionChainId(), actual.getTransactionChainId());
        assertEquals("getVersion", DataStoreVersions.LITHIUM_VERSION, actual.getVersion());
    }

    @Test
    public void testIsSerializedType() {
        assertEquals("isSerializedType", true, CloseTransactionChain.isSerializedType(
                ShardTransactionChainMessages.CloseTransactionChain.newBuilder().setTransactionChainId("").build()));

        assertEquals("isSerializedType", true, CloseTransactionChain.isSerializedType(new CloseTransactionChain()));
        assertEquals("isSerializedType", false, CloseTransactionChain.isSerializedType(new Object()));
    }
}
