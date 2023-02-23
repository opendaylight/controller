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
import org.opendaylight.controller.cluster.datastore.AbstractTest;
import org.opendaylight.controller.cluster.datastore.DataStoreVersions;

/**
 * Unit tests for CloseTransactionChain.
 *
 * @author Thomas Pantelis
 */
public class CloseTransactionChainTest extends AbstractTest {
    @Test
    public void testSerialization() {
        CloseTransactionChain expected = new CloseTransactionChain(newHistoryId(1), DataStoreVersions.CURRENT_VERSION);

        var serialized = (Serializable) expected.toSerializable();
        assertEquals("Serialized type", CloseTransactionChain.class, serialized.getClass());

        final byte[] bytes = SerializationUtils.serialize(serialized);
        assertEquals(241, bytes.length);

        CloseTransactionChain actual = CloseTransactionChain.fromSerializable(
                SerializationUtils.deserialize(bytes));
        assertEquals("getIdentifier", expected.getIdentifier(), actual.getIdentifier());
        assertEquals("getVersion", DataStoreVersions.CURRENT_VERSION, actual.getVersion());
    }

    @Test
    public void testIsSerializedType() {
        assertTrue("isSerializedType", CloseTransactionChain.isSerializedType(new CloseTransactionChain()));
        assertFalse("isSerializedType", CloseTransactionChain.isSerializedType(new Object()));
    }
}
