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
import org.opendaylight.controller.cluster.datastore.AbstractTest;
import org.opendaylight.controller.cluster.datastore.DataStoreVersions;

/**
 * Unit tests for CreateTransaction.
 *
 * @author Thomas Pantelis
 */
public class CreateTransactionTest extends AbstractTest {

    @Test
    public void testSerialization() {
        CreateTransaction expected = new CreateTransaction(nextTransactionId(), 2, DataStoreVersions.CURRENT_VERSION);

        Object serialized = expected.toSerializable();
        assertEquals("Serialized type", CreateTransaction.class, serialized.getClass());

        CreateTransaction actual = CreateTransaction.fromSerializable(
                SerializationUtils.clone((Serializable) serialized));
        assertEquals("getTransactionId", expected.getTransactionId(), actual.getTransactionId());
        assertEquals("getTransactionType", expected.getTransactionType(), actual.getTransactionType());
        assertEquals("getVersion", DataStoreVersions.CURRENT_VERSION, actual.getVersion());
    }

    @Test
    public void testSerializationWithNewerVersion() {
        short newerVersion = DataStoreVersions.CURRENT_VERSION + (short)1;
        CreateTransaction expected = new CreateTransaction(nextTransactionId(), 2, newerVersion);

        Object serialized = expected.toSerializable();
        assertEquals("Serialized type", CreateTransaction.class, serialized.getClass());

        CreateTransaction actual = CreateTransaction.fromSerializable(
                SerializationUtils.clone((Serializable) serialized));
        assertEquals("getTransactionId", expected.getTransactionId(), actual.getTransactionId());
        assertEquals("getTransactionType", expected.getTransactionType(), actual.getTransactionType());
        assertEquals("getVersion", DataStoreVersions.CURRENT_VERSION, actual.getVersion());
    }

    @Test
    public void testIsSerializedType() {
        assertEquals("isSerializedType", true, CreateTransaction.isSerializedType(new CreateTransaction()));
        assertEquals("isSerializedType", false, CreateTransaction.isSerializedType(new Object()));
    }
}
