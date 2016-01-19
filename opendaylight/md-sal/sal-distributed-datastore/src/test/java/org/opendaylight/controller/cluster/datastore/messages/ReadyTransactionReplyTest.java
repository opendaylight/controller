/*
 * Copyright (c) 2015 Brocade Communications Systems, Inc. and others.  All rights reserved.
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
 * Unit tests for ReadyTransactionReply.
 *
 * @author Thomas Pantelis
 */
public class ReadyTransactionReplyTest {

    @Test
    public void testSerialization() {
        String cohortPath = "cohort path";
        ReadyTransactionReply expected = new ReadyTransactionReply(cohortPath);

        Object serialized = expected.toSerializable();
        assertEquals("Serialized type", ReadyTransactionReply.class, serialized.getClass());

        ReadyTransactionReply actual = ReadyTransactionReply.fromSerializable(SerializationUtils.clone(
                (Serializable) serialized));
        assertEquals("getVersion", DataStoreVersions.CURRENT_VERSION, actual.getVersion());
        assertEquals("getCohortPath", cohortPath, actual.getCohortPath());
    }
}
