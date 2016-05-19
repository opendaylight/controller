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

/**
 * Unit tests for AbortTransaction.
 *
 * @author Thomas Pantelis
 */
public class AbortTransactionTest {

    @Test
    public void testSerialization() {
        AbortTransaction expected = new AbortTransaction("txId", DataStoreVersions.CURRENT_VERSION);

        Object serialized = expected.toSerializable();
        assertEquals("Serialized type", AbortTransaction.class, serialized.getClass());

        AbortTransaction actual = AbortTransaction.fromSerializable(
                SerializationUtils.clone((Serializable) serialized));
        assertEquals("getTransactionID", expected.getTransactionID(), actual.getTransactionID());
        assertEquals("getVersion", DataStoreVersions.CURRENT_VERSION, actual.getVersion());
    }

    @Test
    public void testIsSerializedType() {
        assertEquals("isSerializedType", true, AbortTransaction.isSerializedType(new AbortTransaction()));
        assertEquals("isSerializedType", false, AbortTransaction.isSerializedType(new Object()));
    }
}
