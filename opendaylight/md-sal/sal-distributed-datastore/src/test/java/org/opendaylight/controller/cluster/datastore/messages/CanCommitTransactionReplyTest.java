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
 * Unit tests for CanCommitTransactionReply.
 *
 * @author Thomas Pantelis
 */
public class CanCommitTransactionReplyTest {

    @Test
    public void testSerialization() {
        testSerialization(CanCommitTransactionReply.yes(DataStoreVersions.CURRENT_VERSION),
                CanCommitTransactionReply.class);
        testSerialization(CanCommitTransactionReply.no(DataStoreVersions.CURRENT_VERSION),
                CanCommitTransactionReply.class);
    }

    private static void testSerialization(CanCommitTransactionReply expected, Class<?> expSerialized) {
        Object serialized = expected.toSerializable();
        assertEquals("Serialized type", expSerialized, serialized.getClass());

        CanCommitTransactionReply actual = CanCommitTransactionReply.fromSerializable(
                SerializationUtils.clone((Serializable) serialized));
        assertEquals("getCanCommit", expected.getCanCommit(), actual.getCanCommit());
        assertEquals("getVersion", DataStoreVersions.CURRENT_VERSION, actual.getVersion());
    }

    @Test
    public void testSerializationWithPreBoronVersion() {
        testSerialization(CanCommitTransactionReply.yes(DataStoreVersions.LITHIUM_VERSION),
                ThreePhaseCommitCohortMessages.CanCommitTransactionReply.class);
        testSerialization(CanCommitTransactionReply.no(DataStoreVersions.LITHIUM_VERSION),
                ThreePhaseCommitCohortMessages.CanCommitTransactionReply.class);
    }

    @Test
    public void testIsSerializedType() {
        assertEquals("isSerializedType", true, CanCommitTransactionReply.isSerializedType(
                ThreePhaseCommitCohortMessages.CanCommitTransactionReply.newBuilder().setCanCommit(true).build()));

        assertEquals("isSerializedType", true, CanCommitTransactionReply.isSerializedType(
                new CanCommitTransactionReply()));
        assertEquals("isSerializedType", false, CanCommitTransactionReply.isSerializedType(new Object()));
    }
}
