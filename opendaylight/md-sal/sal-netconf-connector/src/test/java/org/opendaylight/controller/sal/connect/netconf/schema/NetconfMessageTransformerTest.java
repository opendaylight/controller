/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.sal.connect.netconf.schema;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import java.net.URI;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.opendaylight.controller.netconf.api.NetconfMessage;
import org.opendaylight.controller.netconf.util.test.XmlFileLoader;
import org.opendaylight.controller.sal.connect.netconf.schema.mapping.NetconfMessageTransformer;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.CompositeNode;
import org.opendaylight.yangtools.yang.model.api.DataSchemaNode;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;

import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

public class NetconfMessageTransformerTest {
    @Mock
    private SchemaContext schemaContext;
    @Mock
    private CompositeNode compositeNode;

    private QName qname;
    private NetconfMessageTransformer transformer;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        qname = new QName(URI.create("uri"), "local");
        doReturn(qname).when(compositeNode).getNodeType();
        doReturn(null).when(compositeNode).getFirstCompositeByName(any(QName.class));
        doReturn(Lists.newArrayList()).when(compositeNode).getValue();
        transformer = new NetconfMessageTransformer();
    }

    @Test
    public void testRpcRequest() throws Exception {
        assertNotNull(transformer.toRpcRequest(qname, compositeNode));
    }

    @Test
    public void testRpcRequest2() throws Exception {
        qname = new QName(URI.create("urn:ietf:params:xml:ns:netconf:base:1.0"), "edit-config");
        System.out.println(qname.getLocalName());
        assertNotNull(transformer.toRpcRequest(qname, compositeNode));
    }

    @Test
    public void testRpcRequestEdit() throws Exception {
        doReturn(Sets.newHashSet()).when(schemaContext).getOperations();
        doReturn(Sets.newHashSet()).when(schemaContext).getChildNodes();
        transformer.onGlobalContextUpdated(schemaContext);
        qname = new QName(URI.create("urn:ietf:params:xml:ns:netconf:base:1.0"), "edit-config");
        System.out.println(qname.getLocalName());
        assertNotNull(transformer.toRpcRequest(qname, compositeNode));
    }

    @Test
    public void testRpcRequestGet() throws Exception {
        doReturn(Sets.newHashSet()).when(schemaContext).getOperations();
        doReturn(Sets.newHashSet()).when(schemaContext).getChildNodes();
        transformer.onGlobalContextUpdated(schemaContext);
        qname = new QName(URI.create("urn:ietf:params:xml:ns:netconf:base:1.0"), "get");
        System.out.println(qname.getLocalName());
        assertNotNull(transformer.toRpcRequest(qname, compositeNode));
    }

    @Test
    public void testRpcRequestGetConfig() throws Exception {
        doReturn(Sets.newHashSet()).when(schemaContext).getOperations();
        doReturn(Sets.newHashSet()).when(schemaContext).getChildNodes();
        transformer.onGlobalContextUpdated(schemaContext);
        qname = new QName(URI.create("urn:ietf:params:xml:ns:netconf:base:1.0"), "get-config");
        System.out.println(qname.getLocalName());
        assertNotNull(transformer.toRpcRequest(qname, compositeNode));
    }

    @Test
    public void testRpcResult() throws Exception {
        NetconfMessage netconfMessage = new NetconfMessage(XmlFileLoader.xmlFileToDocument("netconfMessages/rpc-reply_ok.xml"));
        qname = new QName(URI.create("urn:ietf:params:xml:ns:netconf:base:1.0"), "ok");
        assertNotNull(transformer.toRpcResult(netconfMessage, qname));
    }

    @Test
    public void testRpcResult2() throws Exception {
        doReturn(Sets.newHashSet()).when(schemaContext).getOperations();
        transformer.onGlobalContextUpdated(schemaContext);
        NetconfMessage netconfMessage = new NetconfMessage(XmlFileLoader.xmlFileToDocument("netconfMessages/rpc-reply_ok.xml"));
        qname = new QName(URI.create("urn:ietf:params:xml:ns:netconf:base:1.0"), "ok");
        assertNotNull(transformer.toRpcResult(netconfMessage, qname));
    }

    @Test
    public void testRpcResultGet() throws Exception {
        doReturn(Sets.newHashSet()).when(schemaContext).getOperations();
        DataSchemaNode node = mock(DataSchemaNode.class);
        doReturn(Sets.newHashSet()).when(schemaContext).getDataDefinitions();
        transformer.onGlobalContextUpdated(schemaContext);
        NetconfMessage netconfMessage = new NetconfMessage(XmlFileLoader.xmlFileToDocument("netconfMessages/editConfig_expectedResult.xml"));
        qname = new QName(URI.create("urn:ietf:params:xml:ns:netconf:base:1.0"), "get");
        assertNotNull(transformer.toRpcResult(netconfMessage, qname));
    }
}
