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
 * Unit tests for CommitTransactionReply.
 *
 * @author Thomas Pantelis
 */
public class CommitTransactionReplyTest {

    @Test
    public void testSerialization() {
        CommitTransactionReply expected = CommitTransactionReply.instance(DataStoreVersions.CURRENT_VERSION);

        Object serialized = expected.toSerializable();
        assertEquals("Serialized type", CommitTransactionReply.class, serialized.getClass());

        CommitTransactionReply actual = (CommitTransactionReply)SerializationUtils.clone((Serializable) serialized);
        assertEquals("getVersion", DataStoreVersions.CURRENT_VERSION, actual.getVersion());
    }

    @Test
    public void testSerializationWithPreBoronVersion() {
        CommitTransactionReply expected = CommitTransactionReply.instance(DataStoreVersions.LITHIUM_VERSION);

        Object serialized = expected.toSerializable();
        assertEquals("Serialized type", ThreePhaseCommitCohortMessages.CommitTransactionReply.class, serialized.getClass());
    }

    @Test
    public void testIsSerializedType() {
        assertEquals("isSerializedType", true, CommitTransactionReply.isSerializedType(
                ThreePhaseCommitCohortMessages.CommitTransactionReply.newBuilder().build()));

        assertEquals("isSerializedType", true, CommitTransactionReply.isSerializedType(new CommitTransactionReply()));
        assertEquals("isSerializedType", false, CommitTransactionReply.isSerializedType(new Object()));
    }
}
