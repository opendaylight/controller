/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.netconf.util.messages;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Collection;
import org.junit.Test;
import org.opendaylight.controller.netconf.api.NetconfMessage;
import org.opendaylight.controller.netconf.util.test.XmlFileLoader;
import org.w3c.dom.Document;

public class NetconfMessageUtilTest {
    @Test
    public void testNetconfMessageUtil() throws Exception {
        Document okMessage = XmlFileLoader.xmlFileToDocument("netconfMessages/rpc-reply_ok.xml");
        assertTrue(NetconfMessageUtil.isOKMessage(new NetconfMessage(okMessage)));
        assertFalse(NetconfMessageUtil.isErrorMessage(new NetconfMessage(okMessage)));

        Document errorMessage = XmlFileLoader.xmlFileToDocument("netconfMessages/communicationError/testClientSendsRpcReply_expectedResponse.xml");
        assertTrue(NetconfMessageUtil.isErrorMessage(new NetconfMessage(errorMessage)));
        assertFalse(NetconfMessageUtil.isOKMessage(new NetconfMessage(errorMessage)));

        Document helloMessage = XmlFileLoader.xmlFileToDocument("netconfMessages/client_hello.xml");
        Collection<String> caps = NetconfMessageUtil.extractCapabilitiesFromHello(new NetconfMessage(helloMessage).getDocument());
        assertTrue(caps.contains("urn:ietf:params:netconf:base:1.0"));
        assertTrue(caps.contains("urn:ietf:params:netconf:base:1.1"));
    }
}
