/**
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.sal.rest.impl.test.providers;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.InputStream;

import javax.ws.rs.core.MediaType;

import org.junit.BeforeClass;
import org.junit.Test;
import org.opendaylight.controller.sal.rest.impl.XmlNormalizedNodeBodyReader;
import org.opendaylight.controller.sal.restconf.impl.NormalizedNodeContext;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.PathArgument;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;
import org.opendaylight.yangtools.yang.data.api.schema.DataContainerChild;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNodes;
import org.opendaylight.yangtools.yang.model.api.DataNodeContainer;
import org.opendaylight.yangtools.yang.model.api.DataSchemaNode;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;

import com.google.common.base.Optional;

/**
 * sal-rest-connector org.opendaylight.controller.sal.rest.impl.test.providers
 *
 *
 *
 * @author <a href="mailto:vdemcak@cisco.com">Vaclav Demcak</a>
 *
 *         Created: Mar 7, 2015
 */
public class TestXmlBodyReader extends AbstractBodyReaderTest {

    private final XmlNormalizedNodeBodyReader xmlBodyReader;
    private static SchemaContext schemaContext;

    public TestXmlBodyReader() throws NoSuchFieldException, SecurityException {
        super();
        xmlBodyReader = new XmlNormalizedNodeBodyReader();
    }

    @Override
    protected MediaType getMediaType() {
        return new MediaType(MediaType.APPLICATION_XML, null);
    }

    @BeforeClass
    public static void initialization() throws NoSuchFieldException,
            SecurityException {
        schemaContext = schemaContextLoader("/instanceidentifier/yang",
                schemaContext);
        schemaContext = schemaContextLoader("/modules", schemaContext);
        schemaContext = schemaContextLoader("/invoke-rpc", schemaContext);
        controllerContext.setSchemas(schemaContext);
    }

    @Test
    public void moduleDataTest() throws Exception {
        final DataSchemaNode dataSchemaNode = schemaContext
                .getDataChildByName("cont");
        final String uri = "instance-identifier-module:cont";
        mockBodyReader(uri, xmlBodyReader, false);
        final InputStream inputStream = TestXmlBodyReader.class
                .getResourceAsStream("/instanceidentifier/xml/xmldata.xml");
        final NormalizedNodeContext returnValue = xmlBodyReader.readFrom(null,
                null, null, mediaType, null, inputStream);
        checkNormalizedNodeContext(returnValue);
        checkExpectValueNormalizeNodeContext(dataSchemaNode, returnValue);
    }

    @Test
    public void moduleSubContainerDataPutTest() throws Exception {
        final DataSchemaNode dataSchemaNode = schemaContext
                .getDataChildByName("cont");
        final String uri = "instance-identifier-module:cont/cont1";
        mockBodyReader(uri, xmlBodyReader, false);
        final InputStream inputStream = TestXmlBodyReader.class
                .getResourceAsStream("/instanceidentifier/xml/xml_sub_container.xml");
        final NormalizedNodeContext returnValue = xmlBodyReader.readFrom(null,
                null, null, mediaType, null, inputStream);
        checkNormalizedNodeContext(returnValue);
        checkExpectValueNormalizeNodeContext(dataSchemaNode, returnValue,
                "cont1");
    }

    @Test
    public void moduleSubContainerDataPostTest() throws Exception {
        final DataSchemaNode dataSchemaNode = schemaContext
                .getDataChildByName("cont");
        final String uri = "instance-identifier-module:cont";
        mockBodyReader(uri, xmlBodyReader, true);
        final InputStream inputStream = TestXmlBodyReader.class
                .getResourceAsStream("/instanceidentifier/xml/xml_sub_container.xml");
        final NormalizedNodeContext returnValue = xmlBodyReader.readFrom(null,
                null, null, mediaType, null, inputStream);
        checkNormalizedNodeContext(returnValue);
        checkExpectValueNormalizeNodeContext(dataSchemaNode, returnValue);
    }

    @Test
    public void rpcModuleInputTest() throws Exception {
        final String uri = "invoke-rpc-module:rpc-test";
        mockBodyReader(uri, xmlBodyReader, true);
        final InputStream inputStream = TestXmlBodyReader.class
                .getResourceAsStream("/invoke-rpc/xml/rpc-input.xml");
        final NormalizedNodeContext returnValue = xmlBodyReader.readFrom(null,
                null, null, mediaType, null, inputStream);
        checkNormalizedNodeContext(returnValue);
        final ContainerNode contNode = (ContainerNode) returnValue.getData();
        final YangInstanceIdentifier yangleaf = YangInstanceIdentifier.of(QName
                .create(contNode.getNodeType(), "lf"));
        final Optional<DataContainerChild<? extends PathArgument, ?>> leafDataNode = contNode
                .getChild(yangleaf.getLastPathArgument());
        assertTrue(leafDataNode.isPresent());
        assertTrue("lf-test".equalsIgnoreCase(leafDataNode.get().getValue()
                .toString()));
    }

    private void checkExpectValueNormalizeNodeContext(
            final DataSchemaNode dataSchemaNode,
            final NormalizedNodeContext nnContext) {
        checkExpectValueNormalizeNodeContext(dataSchemaNode, nnContext, null);
    }

    private void checkExpectValueNormalizeNodeContext(
            final DataSchemaNode dataSchemaNode,
            final NormalizedNodeContext nnContext, final String localQname) {
        YangInstanceIdentifier dataNodeIdent = YangInstanceIdentifier
                .of(dataSchemaNode.getQName());

        if (localQname != null && dataSchemaNode instanceof DataNodeContainer) {
            final DataSchemaNode child = ((DataNodeContainer) dataSchemaNode)
                    .getDataChildByName(localQname);
            dataNodeIdent = YangInstanceIdentifier.builder(dataNodeIdent)
                    .node(child.getQName()).build();
            assertTrue(nnContext.getInstanceIdentifierContext().getSchemaNode()
                    .equals(child));
        } else {
            assertTrue(nnContext.getInstanceIdentifierContext().getSchemaNode()
                    .equals(dataSchemaNode));
        }
        assertTrue(nnContext.getInstanceIdentifierContext()
                .getInstanceIdentifier().equals(dataNodeIdent));
        assertNotNull(NormalizedNodes.findNode(nnContext.getData(),
                dataNodeIdent));
    }
}
