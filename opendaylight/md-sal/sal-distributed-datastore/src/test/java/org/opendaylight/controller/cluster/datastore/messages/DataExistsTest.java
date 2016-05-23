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
import org.opendaylight.controller.md.cluster.datastore.model.TestModel;

/**
 * Unit tests for DataExists.
 *
 * @author Thomas Pantelis
 */
public class DataExistsTest {

    @Test
    public void testSerialization() {
        DataExists expected = new DataExists(TestModel.TEST_PATH, ABIVersion.current());

        Object serialized = expected.toSerializable();
        assertEquals("Serialized type", DataExists.class, serialized.getClass());

        DataExists actual = DataExists.fromSerializable(SerializationUtils.clone((Serializable) serialized));
        assertEquals("getPath", expected.getPath(), actual.getPath());
        assertEquals("getVersion", ABIVersion.current(), actual.getVersion());
    }

    @Test
    public void testIsSerializedType() {
        assertEquals("isSerializedType", true, DataExists.isSerializedType(new DataExists()));
        assertEquals("isSerializedType", false, DataExists.isSerializedType(new Object()));
    }
}
