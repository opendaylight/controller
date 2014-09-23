/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.netconf.impl.mapping.operations;

import com.google.common.base.Optional;
import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.controller.netconf.api.NetconfDocumentedException;
import org.opendaylight.controller.netconf.impl.mapping.CapabilityProvider;
import org.opendaylight.controller.netconf.util.xml.XmlElement;
import org.opendaylight.controller.netconf.util.xml.XmlUtil;
import org.w3c.dom.Document;

import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;

public class DefaultGetSchemaTest {

    private CapabilityProvider cap;
    private Document doc;
    private String getSchema;

    @Before
    public void setUp() throws Exception {
        cap = mock(CapabilityProvider.class);
        doc = XmlUtil.newDocument();
        getSchema = "<get-schema xmlns=\"urn:ietf:params:xml:ns:yang:ietf-netconf-monitoring\">\n" +
                "        <identifier>threadpool-api</identifier>\n" +
                "        <version>2010-09-24</version>\n" +
                "        <format\n" +
                "                xmlns:ncm=\"urn:ietf:params:xml:ns:yang:ietf-netconf-monitoring\">ncm:yang\n" +
                "        </format>\n" +
                "    </get-schema>";
    }

    @Test(expected = NetconfDocumentedException.class)
    public void testDefaultGetSchema() throws Exception {
        DefaultGetSchema schema = new DefaultGetSchema(cap, "");
        doThrow(IllegalStateException.class).when(cap).getSchemaForCapability(anyString(), any(Optional.class));
        schema.handleWithNoSubsequentOperations(doc, XmlElement.fromDomElement(XmlUtil.readXmlToElement(getSchema)));
    }

    @Test
    public void handleWithNoSubsequentOperations() throws Exception {
        DefaultGetSchema schema = new DefaultGetSchema(cap, "");
        doReturn("").when(cap).getSchemaForCapability(anyString(), any(Optional.class));
        assertNotNull(schema.handleWithNoSubsequentOperations(doc, XmlElement.fromDomElement(XmlUtil.readXmlToElement(getSchema))));
    }
}
