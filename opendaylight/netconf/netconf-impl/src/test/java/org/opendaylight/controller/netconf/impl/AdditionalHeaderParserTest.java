/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.netconf.impl;

import junit.framework.Assert;

import org.junit.Test;
import org.opendaylight.controller.netconf.util.messages.NetconfHelloMessageAdditionalHeader;

public class AdditionalHeaderParserTest {

    @Test
    public void testParsing() throws Exception {
        String s = "[netconf;10.12.0.102:48528;ssh;;;;;;]";
        NetconfHelloMessageAdditionalHeader header = NetconfHelloMessageAdditionalHeader.fromString(s);
        Assert.assertEquals("netconf", header.getUserName());
        Assert.assertEquals("10.12.0.102", header.getAddress());
        Assert.assertEquals("ssh", header.getTransport());
    }

    @Test
    public void testParsing2() throws Exception {
        String s = "[tomas;10.0.0.0/10000;tcp;1000;1000;;/home/tomas;;]";
        NetconfHelloMessageAdditionalHeader header = NetconfHelloMessageAdditionalHeader.fromString(s);
        Assert.assertEquals("tomas", header.getUserName());
        Assert.assertEquals("10.0.0.0", header.getAddress());
        Assert.assertEquals("tcp", header.getTransport());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testParsingNoUsername() throws Exception {
        String s = "[10.12.0.102:48528;ssh;;;;;;]";
        NetconfHelloMessageAdditionalHeader.fromString(s);
    }
}
