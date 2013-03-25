package org.openflow.protocol.queue;


import junit.framework.TestCase;

import org.junit.Test;


public class OFQueuePropertyTypeTest extends TestCase {
    @Test
    public void testMapping() throws Exception {
        TestCase.assertEquals(OFQueuePropertyType.NONE,
                OFQueuePropertyType.valueOf((short) 0));
        TestCase.assertEquals(OFQueuePropertyType.MIN_RATE,
                OFQueuePropertyType.valueOf((short) 1));
    }
}
