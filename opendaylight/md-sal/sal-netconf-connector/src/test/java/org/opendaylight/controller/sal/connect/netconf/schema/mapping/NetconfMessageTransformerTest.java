/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html.
 */

package org.opendaylight.controller.sal.connect.netconf.schema.mapping;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.io.InputStream;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import org.hamcrest.CoreMatchers;
import org.junit.Test;
import org.opendaylight.controller.netconf.api.NetconfMessage;
import org.opendaylight.controller.netconf.util.xml.XmlUtil;
import org.opendaylight.controller.sal.connect.netconf.NetconfToRpcRequestTest;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.data.api.CompositeNode;
import org.opendaylight.yangtools.yang.data.api.Node;
import org.opendaylight.yangtools.yang.data.impl.NodeFactory;
import org.opendaylight.yangtools.yang.model.api.Module;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.opendaylight.yangtools.yang.parser.impl.YangParserImpl;

public class NetconfMessageTransformerTest {

    private static final QName COMMIT_Q_NAME = QName.create("namespace", "2012-12-12", "commit");

    @Test
    public void testToRpcRequestNoSchemaForRequest() throws Exception {
        final NetconfMessageTransformer netconfMessageTransformer = getTransformer();
        final NetconfMessage netconfMessage = netconfMessageTransformer.toRpcRequest(COMMIT_Q_NAME,
                NodeFactory.createImmutableCompositeNode(COMMIT_Q_NAME, null, Collections.<Node<?>>emptyList()));
        assertThat(XmlUtil.toString(netconfMessage.getDocument()), CoreMatchers.containsString("<commit"));
    }

    private NetconfMessageTransformer getTransformer() {
        final NetconfMessageTransformer netconfMessageTransformer = new NetconfMessageTransformer();
        netconfMessageTransformer.onGlobalContextUpdated(getSchema());
        return netconfMessageTransformer;
    }

    @Test
    public void testToRpcResultNoSchemaForResult() throws Exception {
        final NetconfMessageTransformer netconfMessageTransformer = getTransformer();
        final NetconfMessage response = new NetconfMessage(XmlUtil.readXmlToDocument(
                "<rpc-reply><ok/></rpc-reply>"
        ));
        final RpcResult<CompositeNode> compositeNodeRpcResult = netconfMessageTransformer.toRpcResult(response, COMMIT_Q_NAME);
        assertTrue(compositeNodeRpcResult.isSuccessful());
        assertEquals("ok", compositeNodeRpcResult.getResult().getValue().get(0).getKey().getLocalName());
    }

    public SchemaContext getSchema() {
        final List<InputStream> modelsToParse = Collections
                .singletonList(NetconfToRpcRequestTest.class.getResourceAsStream("/schemas/rpc-notification-subscription.yang"));
        final YangParserImpl parser = new YangParserImpl();
        final Set<Module> configModules = parser.parseYangModelsFromStreams(modelsToParse);
        final SchemaContext cfgCtx = parser.resolveSchemaContext(configModules);
        assertNotNull(cfgCtx);
        return cfgCtx;
    }
}
