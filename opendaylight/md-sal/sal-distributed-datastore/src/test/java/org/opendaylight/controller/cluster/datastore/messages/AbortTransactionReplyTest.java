/*
 * Copyright (c) 2016 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.messages;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.Serializable;
import org.apache.commons.lang3.SerializationUtils;
import org.junit.Test;
import org.opendaylight.controller.cluster.datastore.DataStoreVersions;

/**
 * Unit tests for AbortTransactionReply.
 *
 * @author Thomas Pantelis
 */
public class AbortTransactionReplyTest {

    @Test
    public void testSerialization() {
        AbortTransactionReply expected = AbortTransactionReply.instance(DataStoreVersions.CURRENT_VERSION);

        Object serialized = expected.toSerializable();
        assertEquals("Serialized type", AbortTransactionReply.class, serialized.getClass());

        AbortTransactionReply actual = (AbortTransactionReply)SerializationUtils.clone((Serializable) serialized);
        assertEquals("getVersion", DataStoreVersions.CURRENT_VERSION, actual.getVersion());
    }

    @Test
    public void testIsSerializedType() {
        assertTrue("isSerializedType", AbortTransactionReply.isSerializedType(new AbortTransactionReply()));
        assertFalse("isSerializedType", AbortTransactionReply.isSerializedType(new Object()));
    }
}
