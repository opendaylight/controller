/*
 * Copyright (c) 2014, 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.datastore.node.utils.serialization;

import static org.junit.Assert.assertEquals;
import org.junit.Test;

public class ValueTypeTest {

    @Test
    public void testGetSerializableType(){
        byte[] b = new byte[10];
        b[0] = 1;
        b[2] = 2;

        ValueType serializableType = ValueType.getSerializableType(b);
        assertEquals(ValueType.BINARY_TYPE, serializableType);
    }

    @Test
    public void testNullType(){
        ValueType serializableType = ValueType.getSerializableType(null);
        assertEquals(ValueType.NULL_TYPE, serializableType);

        assertEquals(null, ValueType.NULL_TYPE.deserialize(""));
    }
}