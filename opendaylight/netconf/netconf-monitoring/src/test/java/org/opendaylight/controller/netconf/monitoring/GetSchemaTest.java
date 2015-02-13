/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.netconf.monitoring;

import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;

import com.google.common.base.Optional;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.controller.netconf.api.NetconfDocumentedException;
import org.opendaylight.controller.netconf.api.monitoring.NetconfMonitoringService;
import org.opendaylight.controller.netconf.util.xml.XmlElement;
import org.opendaylight.controller.netconf.util.xml.XmlUtil;
import org.w3c.dom.Document;

public class GetSchemaTest {


    private NetconfMonitoringService cap;
    private Document doc;
    private String getSchema;

    @Before
    public void setUp() throws Exception {
        cap = mock(NetconfMonitoringService.class);
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
        GetSchema schema = new GetSchema(cap);
        doThrow(IllegalStateException.class).when(cap).getSchemaForCapability(anyString(), any(Optional.class));
        schema.handleWithNoSubsequentOperations(doc, XmlElement.fromDomElement(XmlUtil.readXmlToElement(getSchema)));
    }

    @Test
    public void handleWithNoSubsequentOperations() throws Exception {
        GetSchema schema = new GetSchema(cap);
        doReturn("").when(cap).getSchemaForCapability(anyString(), any(Optional.class));
        assertNotNull(schema.handleWithNoSubsequentOperations(doc, XmlElement.fromDomElement(XmlUtil.readXmlToElement(getSchema))));
    }

}