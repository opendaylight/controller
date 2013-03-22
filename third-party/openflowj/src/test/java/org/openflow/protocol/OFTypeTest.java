
/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.openflow.protocol;


import junit.framework.TestCase;

import org.junit.Test;


public class OFTypeTest extends TestCase {

    public void testOFTypeCreate() throws Exception {
        OFType foo = OFType.HELLO;
        Class<? extends OFMessage> c = foo.toClass();
        TestCase.assertEquals(c, OFHello.class);
    }

    @Test
    public void testMapping() throws Exception {
        TestCase.assertEquals(OFType.HELLO, OFType.valueOf((byte) 0));
        TestCase.assertEquals(OFType.BARRIER_REPLY, OFType.valueOf((byte) 19));
    }
}
