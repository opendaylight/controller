package org.opendaylight.controller.cluster.datastore.node.utils.serialization;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class ValueTypeTest {

    @Test
    public void testGetSerializableType(){
        byte[] b = new byte[10];
        b[0] = 1;
        b[2] = 2;

        ValueType serializableType = ValueType.getSerializableType(b);
        assertEquals(ValueType.BINARY_TYPE, serializableType);
    }
}