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
import org.opendaylight.controller.cluster.access.ABIVersion;

/**
 * Unit tests for CommitTransactionReply.
 *
 * @author Thomas Pantelis
 */
public class CommitTransactionReplyTest {

    @Test
    public void testSerialization() {
        CommitTransactionReply expected = CommitTransactionReply.instance(ABIVersion.current());

        Object serialized = expected.toSerializable();
        assertEquals("Serialized type", CommitTransactionReply.class, serialized.getClass());

        CommitTransactionReply actual = (CommitTransactionReply)SerializationUtils.clone((Serializable) serialized);
        assertEquals("getVersion", ABIVersion.current(), actual.getVersion());
    }

    @Test
    public void testIsSerializedType() {
        assertEquals("isSerializedType", true, CommitTransactionReply.isSerializedType(new CommitTransactionReply()));
        assertEquals("isSerializedType", false, CommitTransactionReply.isSerializedType(new Object()));
    }
}
