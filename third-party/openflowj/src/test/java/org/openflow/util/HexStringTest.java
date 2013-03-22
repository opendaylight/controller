
/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.openflow.util;

import junit.framework.TestCase;

/**
 * Does hexstring conversion work?
 * 
 * @author Rob Sherwood (rob.sherwood@stanford.edu)
 * 
 */

public class HexStringTest extends TestCase {

    public void testMarshalling() throws Exception {
        String dpidStr = "00:00:00:23:20:2d:16:71";
        long dpid = HexString.toLong(dpidStr);
        String testStr = HexString.toHexString(dpid);
        TestCase.assertEquals(dpidStr, testStr);
    }

    public void testToStringBytes() {
        byte[] dpid = { 0, 0, 0, 0, 0, 0, 0, -1 };
        String valid = "00:00:00:00:00:00:00:ff";
        String testString = HexString.toHexString(dpid);
        TestCase.assertEquals(valid, testString);
    }
}
