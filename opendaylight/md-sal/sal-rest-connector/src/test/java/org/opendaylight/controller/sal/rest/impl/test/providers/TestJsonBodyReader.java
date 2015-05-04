/**
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.sal.rest.impl.test.providers;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import com.google.common.base.Optional;
import com.google.common.collect.Sets;
import java.io.InputStream;
import java.net.URI;
import javax.ws.rs.core.MediaType;
import org.junit.BeforeClass;
import org.junit.Test;
import org.opendaylight.controller.sal.rest.impl.JsonNormalizedNodeBodyReader;
import org.opendaylight.controller.sal.restconf.impl.NormalizedNodeContext;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.PathArgument;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;
import org.opendaylight.yangtools.yang.data.api.schema.DataContainerChild;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNodes;
import org.opendaylight.yangtools.yang.model.api.DataNodeContainer;
import org.opendaylight.yangtools.yang.model.api.DataSchemaNode;
import org.opendaylight.yangtools.yang.model.api.Module;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;

/**
 * sal-rest-connector
 * org.opendaylight.controller.sal.rest.impl.test.providers
 *
 *
 *
 * @author <a href="mailto:vdemcak@cisco.com">Vaclav Demcak</a>
 *
 * Created: Mar 11, 2015
 */
public class TestJsonBodyReader extends AbstractBodyReaderTest {

    private final JsonNormalizedNodeBodyReader jsonBodyReader;
    private static SchemaContext schemaContext;

    public TestJsonBodyReader () throws NoSuchFieldException, SecurityException {
        super();
        jsonBodyReader = new JsonNormalizedNodeBodyReader();
    }

    @Override
    protected MediaType getMediaType() {
        return new MediaType(MediaType.APPLICATION_XML, null);
    }

    @BeforeClass
    public static void initialization() throws NoSuchFieldException, SecurityException {
        schemaContext = schemaContextLoader("/instanceidentifier/yang", schemaContext);
        schemaContext = schemaContextLoader("/modules", schemaContext);
        schemaContext = schemaContextLoader("/invoke-rpc", schemaContext);
        controllerContext.setSchemas(schemaContext);
    }

    @Test
    public void moduleDataTest() throws Exception {
        final DataSchemaNode dataSchemaNode = schemaContext.getDataChildByName("cont");
        final YangInstanceIdentifier dataII = YangInstanceIdentifier.of(dataSchemaNode.getQName());
        final String uri = "instance-identifier-module:cont";
        mockBodyReader(uri, jsonBodyReader, false);
        final InputStream inputStream = TestJsonBodyReader.class
                .getResourceAsStream("/instanceidentifier/json/jsondata.json");
        final NormalizedNodeContext returnValue = jsonBodyReader
                .readFrom(null, null, null, mediaType, null, inputStream);
        checkNormalizedNodeContext(returnValue);
        checkExpectValueNormalizeNodeContext(dataSchemaNode, returnValue, dataII);
    }

    @Test
    public void moduleSubContainerDataPutTest() throws Exception {
        final DataSchemaNode dataSchemaNode = schemaContext.getDataChildByName("cont");
        QName cont1QName = QName.create(dataSchemaNode.getQName(), "cont1");
        final YangInstanceIdentifier dataII = YangInstanceIdentifier.of(dataSchemaNode.getQName()).node(cont1QName);
        final DataSchemaNode dataSchemaNodeOnPath = ((DataNodeContainer) dataSchemaNode).getDataChildByName(cont1QName);
        final String uri = "instance-identifier-module:cont/cont1";
        mockBodyReader(uri, jsonBodyReader, false);
        final InputStream inputStream = TestJsonBodyReader.class
                .getResourceAsStream("/instanceidentifier/json/json_sub_container.json");
        final NormalizedNodeContext returnValue = jsonBodyReader
                .readFrom(null, null, null, mediaType, null, inputStream);
        checkNormalizedNodeContext(returnValue);
        checkExpectValueNormalizeNodeContext(dataSchemaNodeOnPath, returnValue, dataII);
    }

    @Test
    public void moduleSubContainerDataPostTest() throws Exception {
        final DataSchemaNode dataSchemaNode = schemaContext.getDataChildByName("cont");
        QName cont1QName = QName.create(dataSchemaNode.getQName(), "cont1");
        final YangInstanceIdentifier dataII = YangInstanceIdentifier.of(dataSchemaNode.getQName()).node(cont1QName);
        final String uri = "instance-identifier-module:cont";
        mockBodyReader(uri, jsonBodyReader, true);
        final InputStream inputStream = TestJsonBodyReader.class
                .getResourceAsStream("/instanceidentifier/json/json_sub_container.json");
        final NormalizedNodeContext returnValue = jsonBodyReader
                .readFrom(null, null, null, mediaType, null, inputStream);
        checkNormalizedNodeContext(returnValue);
        checkExpectValueNormalizeNodeContext(dataSchemaNode, returnValue, dataII);
    }

    @Test
    public void moduleSubContainerAugmentDataPostTest() throws Exception {
        final DataSchemaNode dataSchemaNode = schemaContext.getDataChildByName("cont");
        final Module augmentModule = schemaContext.findModuleByNamespace(new URI("augment:module")).iterator().next();
        QName contAugmentQName = QName.create(augmentModule.getQNameModule(), "cont-augment");
        YangInstanceIdentifier.AugmentationIdentifier augII = new YangInstanceIdentifier.AugmentationIdentifier(
                Sets.newHashSet(contAugmentQName));
        final YangInstanceIdentifier dataII = YangInstanceIdentifier.of(dataSchemaNode.getQName())
                .node(augII).node(contAugmentQName);
        final String uri = "instance-identifier-module:cont";
        mockBodyReader(uri, jsonBodyReader, true);
        final InputStream inputStream = TestXmlBodyReader.class
                .getResourceAsStream("/instanceidentifier/json/json_augment_container.json");
        final NormalizedNodeContext returnValue = jsonBodyReader
                .readFrom(null, null, null, mediaType, null, inputStream);
        checkNormalizedNodeContext(returnValue);
        checkExpectValueNormalizeNodeContext(dataSchemaNode, returnValue, dataII);
    }

    //FIXME: Uncomment this when JsonParserStream works correctly with case augmentation with choice
    //@Test
    public void moduleSubContainerChoiceAugmentDataPostTest() throws Exception {
        final DataSchemaNode dataSchemaNode = schemaContext.getDataChildByName("cont");
        final Module augmentModule = schemaContext.findModuleByNamespace(new URI("augment:module")).iterator().next();
        QName augmentChoice1QName = QName.create(augmentModule.getQNameModule(), "augment-choice1");
        QName augmentChoice2QName = QName.create(augmentChoice1QName, "augment-choice2");
        final QName containerQName = QName.create(augmentChoice1QName, "case-choice-case-container1");
        YangInstanceIdentifier.AugmentationIdentifier augChoice1II = new YangInstanceIdentifier.AugmentationIdentifier(
                Sets.newHashSet(augmentChoice1QName));
        YangInstanceIdentifier.AugmentationIdentifier augChoice2II = new YangInstanceIdentifier.AugmentationIdentifier(
                Sets.newHashSet(augmentChoice2QName));
        final YangInstanceIdentifier dataII = YangInstanceIdentifier.of(dataSchemaNode.getQName())
                .node(augChoice1II).node(augmentChoice1QName).node(augChoice2II).node(augmentChoice2QName)
                .node(containerQName);
        final String uri = "instance-identifier-module:cont";
        mockBodyReader(uri, jsonBodyReader, true);
        final InputStream inputStream = TestXmlBodyReader.class
                .getResourceAsStream("/instanceidentifier/json/json_augment_choice_container.json");
        final NormalizedNodeContext returnValue = jsonBodyReader
                .readFrom(null, null, null, mediaType, null, inputStream);
        checkNormalizedNodeContext(returnValue);
        checkExpectValueNormalizeNodeContext(dataSchemaNode, returnValue, dataII);
    }

    @Test
    public void rpcModuleInputTest() throws Exception {
        final String uri = "invoke-rpc-module:rpc-test";
        mockBodyReader(uri, jsonBodyReader, true);
        final InputStream inputStream = TestJsonBodyReader.class
                .getResourceAsStream("/invoke-rpc/json/rpc-input.json");
        final NormalizedNodeContext returnValue = jsonBodyReader.readFrom(null,
                null, null, mediaType, null, inputStream);
        checkNormalizedNodeContext(returnValue);
        final ContainerNode inputNode = (ContainerNode) returnValue.getData();
        final YangInstanceIdentifier yangCont = YangInstanceIdentifier.of(QName
                .create(inputNode.getNodeType(), "cont"));
        final Optional<DataContainerChild<? extends PathArgument, ?>> contDataNode = inputNode
                .getChild(yangCont.getLastPathArgument());
        assertTrue(contDataNode.isPresent());
        assertTrue(contDataNode.get() instanceof ContainerNode);
        final YangInstanceIdentifier yangleaf = YangInstanceIdentifier.of(QName
                .create(inputNode.getNodeType(), "lf"));
        final Optional<DataContainerChild<? extends PathArgument, ?>> leafDataNode = ((ContainerNode) contDataNode
                .get()).getChild(yangleaf.getLastPathArgument());
        assertTrue(leafDataNode.isPresent());
        assertTrue("lf-test".equalsIgnoreCase(leafDataNode.get().getValue()
                .toString()));
    }

    private void checkExpectValueNormalizeNodeContext(
            final DataSchemaNode dataSchemaNode,
            final NormalizedNodeContext nnContext) {
        checkExpectValueNormalizeNodeContext(dataSchemaNode, nnContext, null);
    }

    private void checkExpectValueNormalizeNodeContext(final DataSchemaNode dataSchemaNode,
                                                      final NormalizedNodeContext nnContext,
                                                      final YangInstanceIdentifier dataNodeIdent) {
        assertEquals(dataSchemaNode, nnContext.getInstanceIdentifierContext().getSchemaNode());
        assertEquals(dataNodeIdent, nnContext.getInstanceIdentifierContext().getInstanceIdentifier());
        assertNotNull(NormalizedNodes.findNode(nnContext.getData(), dataNodeIdent));
    }
}
