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
import org.opendaylight.controller.protobuff.messages.cohort3pc.ThreePhaseCommitCohortMessages;

/**
 * Unit tests for CanCommitTransaction.
 *
 * @author Thomas Pantelis
 */
public class CanCommitTransactionTest {

    @Test
    public void testSerialization() {
        CanCommitTransaction expected = new CanCommitTransaction("txId", DataStoreVersions.CURRENT_VERSION);

        Object serialized = expected.toSerializable();
        assertEquals("Serialized type", CanCommitTransaction.class, serialized.getClass());

        CanCommitTransaction actual = CanCommitTransaction.fromSerializable(
                SerializationUtils.clone((Serializable) serialized));
        assertEquals("getTransactionID", expected.getTransactionID(), actual.getTransactionID());
        assertEquals("getVersion", DataStoreVersions.CURRENT_VERSION, actual.getVersion());
    }

    @Test
    public void testSerializationWithPreBoronVersion() {
        CanCommitTransaction expected = new CanCommitTransaction("txId", DataStoreVersions.LITHIUM_VERSION);

        Object serialized = expected.toSerializable();
        assertEquals("Serialized type", ThreePhaseCommitCohortMessages.CanCommitTransaction.class, serialized.getClass());

        CanCommitTransaction actual = CanCommitTransaction.fromSerializable(
                SerializationUtils.clone((Serializable) serialized));
        assertEquals("getTransactionID", expected.getTransactionID(), actual.getTransactionID());
        assertEquals("getVersion", DataStoreVersions.LITHIUM_VERSION, actual.getVersion());
    }

    @Test
    public void testIsSerializedType() {
        assertEquals("isSerializedType", true, CanCommitTransaction.isSerializedType(
                ThreePhaseCommitCohortMessages.CanCommitTransaction.newBuilder().
                setTransactionId("").build()));

        assertEquals("isSerializedType", true, CanCommitTransaction.isSerializedType(new CanCommitTransaction()));
        assertEquals("isSerializedType", false, CanCommitTransaction.isSerializedType(new Object()));
    }
}
