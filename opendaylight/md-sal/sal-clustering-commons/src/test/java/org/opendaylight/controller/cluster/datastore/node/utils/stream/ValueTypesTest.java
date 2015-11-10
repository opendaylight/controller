package org.opendaylight.controller.cluster.datastore.node.utils.stream;

import static org.junit.Assert.assertEquals;
import org.junit.Test;

public class ValueTypesTest {
    @Test
    public void testStringType(){
        assertEquals(ValueTypes.STRING_TYPE, ValueTypes.getSerializableType("foobar"));
        final String largeString = largeString(ValueTypes.STRING_BYTES_LENGTH_THRESHOLD);
        assertEquals(ValueTypes.STRING_BYTES_TYPE, ValueTypes.getSerializableType(largeString));
    }

    private String largeString(int minSize){
        final int pow = (int) (Math.log(minSize * 2) / Math.log(2));
        String s = "X";
        for(int i=0;i<pow;i++){
            StringBuilder b = new StringBuilder();
            b.append(s).append(s);
            s = b.toString();
        }
        return s;
    }

}