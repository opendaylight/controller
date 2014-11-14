/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.netconf.impl.mapping.operations;

import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;

import org.junit.Test;
import org.opendaylight.controller.netconf.api.NetconfDocumentedException;
import org.opendaylight.controller.netconf.util.xml.XmlElement;
import org.opendaylight.controller.netconf.util.xml.XmlUtil;
import org.w3c.dom.Document;

public class DefaultCloseSessionTest {
    @Test
    public void testDefaultCloseSession() throws Exception {
        AutoCloseable res = mock(AutoCloseable.class);
        doNothing().when(res).close();
        DefaultCloseSession session = new DefaultCloseSession("", res);
        Document doc = XmlUtil.newDocument();
        XmlElement elem = XmlElement.fromDomElement(XmlUtil.readXmlToElement("<elem/>"));
        session.handleWithNoSubsequentOperations(doc, elem);
    }

    @Test(expected = NetconfDocumentedException.class)
    public void testDefaultCloseSession2() throws Exception {
        AutoCloseable res = mock(AutoCloseable.class);
        doThrow(NetconfDocumentedException.class).when(res).close();
        DefaultCloseSession session = new DefaultCloseSession("", res);
        Document doc = XmlUtil.newDocument();
        XmlElement elem = XmlElement.fromDomElement(XmlUtil.readXmlToElement("<elem/>"));
        session.handleWithNoSubsequentOperations(doc, elem);
    }
}
