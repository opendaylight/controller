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
 * Unit test for CloseTransaction and CloseTransactionReply.
 *
 * @author Thomas Pantelis
 */
public class CloseTransactionTest {
    @Test
    public void testCloseTransactionSerialization() {
        CloseTransaction expected = new CloseTransaction(DataStoreVersions.CURRENT_VERSION);

        Object serialized = expected.toSerializable();
        assertEquals("Serialized type", CloseTransaction.class, serialized.getClass());

        CloseTransaction actual = (CloseTransaction)SerializationUtils.clone((Serializable) serialized);
        assertEquals("getVersion", DataStoreVersions.CURRENT_VERSION, actual.getVersion());
    }

    @Test
    public void testCloseTransactionReplySerialization() {
        CloseTransactionReply expected = new CloseTransactionReply();

        Object serialized = expected.toSerializable();
        assertEquals("Serialized type", CloseTransactionReply.class, serialized.getClass());

        CloseTransactionReply actual = (CloseTransactionReply)SerializationUtils.clone((Serializable) serialized);
        assertEquals("getVersion", DataStoreVersions.CURRENT_VERSION, actual.getVersion());
    }
}
