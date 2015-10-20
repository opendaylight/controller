package org.openflow.protocol;


import org.junit.Test;
import org.openflow.protocol.action.OFActionType;

import junit.framework.TestCase;


public class OFActionTypeTest extends TestCase {
    @Test
    public void testMapping() throws Exception {
        TestCase.assertEquals(OFActionType.OUTPUT,
                OFActionType.valueOf((short) 0));
        TestCase.assertEquals(OFActionType.OPAQUE_ENQUEUE,
                OFActionType.valueOf((short) 11));
        TestCase.assertEquals(OFActionType.VENDOR,
                OFActionType.valueOf((short) 0xffff));
    }
}
