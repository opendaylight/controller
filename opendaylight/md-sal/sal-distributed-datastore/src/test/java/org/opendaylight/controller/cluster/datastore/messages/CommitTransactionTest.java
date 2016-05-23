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
import org.opendaylight.controller.cluster.datastore.AbstractTest;

/**
 * Unit tests for CommitTransaction.
 *
 * @author Thomas Pantelis
 */
public class CommitTransactionTest extends AbstractTest {

    @Test
    public void testSerialization() {
        CommitTransaction expected = new CommitTransaction(nextTransactionId(), ABIVersion.current());

        Object serialized = expected.toSerializable();
        assertEquals("Serialized type", CommitTransaction.class, serialized.getClass());

        CommitTransaction actual = CommitTransaction.fromSerializable(
                SerializationUtils.clone((Serializable) serialized));
        assertEquals("getTransactionID", expected.getTransactionID(), actual.getTransactionID());
        assertEquals("getVersion", ABIVersion.current(), actual.getVersion());
    }

    @Test
    public void testIsSerializedType() {
        assertEquals("isSerializedType", true, CommitTransaction.isSerializedType(new CommitTransaction()));
        assertEquals("isSerializedType", false, CommitTransaction.isSerializedType(new Object()));
    }
}
