package org.openflow.util;

import org.openflow.protocol.factory.BasicFactory;
import org.openflow.protocol.factory.OFMessageFactory;

import junit.framework.TestCase;

public class OFTestCase extends TestCase {
    public OFMessageFactory messageFactory;
    
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        messageFactory = new BasicFactory();
    }

    public void test() throws Exception {
    }
}
