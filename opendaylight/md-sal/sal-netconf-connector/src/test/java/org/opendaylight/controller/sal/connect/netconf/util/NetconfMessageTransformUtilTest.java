/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.sal.connect.netconf.util;

import com.google.common.collect.Sets;
import java.net.URI;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.opendaylight.controller.netconf.api.NetconfDocumentedException;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.CompositeNode;
import org.opendaylight.yangtools.yang.data.api.Node;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doReturn;

public class NetconfMessageTransformUtilTest {

    @Mock
    private SchemaContext schemaContext;
    @Mock
    private CompositeNode compositeNode;
    @Mock
    private Node node;

    private YangInstanceIdentifier yangIId;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        doReturn(Sets.newHashSet()).when(schemaContext).getChildNodes();
        yangIId = YangInstanceIdentifier.builder().node(QName.create("namespace", "2012-12-12", "name")).build();
    }

    @Test
    public void testCreateSchema() throws Exception {
        assertNotNull(NetconfMessageTransformUtil.createSchemaForEdit(schemaContext));
        assertNotNull(NetconfMessageTransformUtil.createSchemaForGet(schemaContext));
        assertNotNull(NetconfMessageTransformUtil.createSchemaForGetConfig(schemaContext));
    }

    @Test
    public void testFindNode() throws Exception {
        doReturn(null).when(compositeNode).getFirstCompositeByName(any(QName.class));
        doReturn(null).when(compositeNode).getFirstSimpleByName(any(QName.class));
        assertNull(NetconfMessageTransformUtil.findNode(compositeNode, yangIId));
    }

    @Test
    public void testCompositeNode() throws Exception {
        QName qname = new QName(URI.create("urn:ietf:params:xml:ns:netconf:base:1.0"), "edit-config");
        doReturn(qname).when(node).getNodeType();
        assertNotNull(NetconfMessageTransformUtil.wrap(qname, null));
        assertNotNull(NetconfMessageTransformUtil.wrap(qname, node, node));
        assertNotNull(NetconfMessageTransformUtil.wrap(qname, node, null));
    }

    @Test
    public void testToRpcError() throws Exception {
        NetconfDocumentedException ex = new NetconfDocumentedException("msg", NetconfDocumentedException.ErrorType.application,
                NetconfDocumentedException.ErrorTag.access_denied, NetconfDocumentedException.ErrorSeverity.warning);
        assertNotNull(NetconfMessageTransformUtil.toRpcError(ex));
    }
}
