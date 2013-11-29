/**
 * @author Maros Marsalek
 *
 * 12 2013
 *
 * Copyright (c) 2012 by Cisco Systems, Inc.
 * All rights reserved.
 */
package org.opendaylight.controller.netconf.impl;

import junit.framework.Assert;
import org.junit.Test;

public class AdditionalHeaderParserTest {

    @Test
    public void testParsing() throws Exception {
        String s = "[netconf;10.12.0.102:48528;ssh;;;;;;]";
        NetconfServerSessionNegotiator.AdditionalHeader header = new NetconfServerSessionNegotiator.AdditionalHeader(s);
        Assert.assertEquals("netconf", header.getUsername());
        Assert.assertEquals("10.12.0.102", header.getAddress());
        Assert.assertEquals("ssh", header.getTransport());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testParsingNoUsername() throws Exception {
        String s = "[10.12.0.102:48528;ssh;;;;;;]";
        NetconfServerSessionNegotiator.AdditionalHeader header = new NetconfServerSessionNegotiator.AdditionalHeader(s);
    }
}
