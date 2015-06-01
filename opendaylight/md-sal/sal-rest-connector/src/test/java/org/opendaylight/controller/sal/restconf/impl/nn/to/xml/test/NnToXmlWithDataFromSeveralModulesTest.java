/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.restconf.impl.nn.to.xml.test;

import static org.junit.Assert.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URISyntaxException;
import java.util.List;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;

import org.junit.BeforeClass;
import org.junit.Test;
import org.opendaylight.controller.sal.rest.impl.NormalizedNodeXmlBodyWriter;
import org.opendaylight.controller.sal.rest.impl.test.providers.AbstractBodyReaderTest;
import org.opendaylight.controller.sal.restconf.impl.ControllerContext;
import org.opendaylight.controller.sal.restconf.impl.InstanceIdentifierContext;
import org.opendaylight.controller.sal.restconf.impl.NormalizedNodeContext;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;
import org.opendaylight.yangtools.yang.data.impl.schema.Builders;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.api.DataContainerNodeAttrBuilder;
import org.opendaylight.yangtools.yang.model.api.ContainerSchemaNode;
import org.opendaylight.yangtools.yang.model.api.DataNodeContainer;
import org.opendaylight.yangtools.yang.model.api.DataSchemaNode;
import org.opendaylight.yangtools.yang.model.api.LeafSchemaNode;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;

import com.google.common.collect.Iterables;

public class NnToXmlWithDataFromSeveralModulesTest extends
        AbstractBodyReaderTest {

    private final NormalizedNodeXmlBodyWriter xmlBodyWriter;
    private static SchemaContext schemaContext;

    public NnToXmlWithDataFromSeveralModulesTest() throws NoSuchFieldException,
            SecurityException {
        super();
        xmlBodyWriter = new NormalizedNodeXmlBodyWriter();
    }

    @BeforeClass
    public static void initialize() {
        schemaContext = schemaContextLoader(
                "/nn-to-xml/data-of-several-modules/yang", schemaContext);
        controllerContext.setSchemas(schemaContext);
    }

    @Test
    public void dataFromSeveralModulesToXmlTest()
            throws WebApplicationException, IOException, URISyntaxException {
        NormalizedNodeContext normalizedNodeContext = prepareNormalizedNodeContext();
        final OutputStream output = new ByteArrayOutputStream();
        xmlBodyWriter.writeTo(normalizedNodeContext, null, null, null,
                mediaType, null, output);

        // data
        assertTrue(output.toString()
                .contains(
                        "<data xmlns=" + '"'
                                + "urn:ietf:params:xml:ns:netconf:base:1.0"
                                + '"' + '>'));
        // cont m2
        assertTrue(output.toString().contains(
                "<cont_m2 xmlns=" + '"' + "module:two" + '"' + '>'));
        assertTrue(output.toString().contains("<lf1_m2>lf1 m2 value</lf1_m2>"));
        assertTrue(output.toString().contains("<contB_m2></contB_m2>"));
        assertTrue(output.toString().contains("</cont_m2>"));

        // cont m1
        assertTrue(output.toString().contains(
                "<cont_m1 xmlns=" + '"' + "module:one" + '"' + '>'));
        assertTrue(output.toString().contains("<contB_m1></contB_m1>"));
        assertTrue(output.toString().contains("<lf1_m1>lf1 m1 value</lf1_m1>"));
        assertTrue(output.toString().contains("</cont_m1>"));

        // end
        assertTrue(output.toString().contains("</data>"));
    }

    @Override
    protected MediaType getMediaType() {
        // TODO Auto-generated method stub
        return null;
    }

    private NormalizedNodeContext prepareNormalizedNodeContext() {
        String rev = "2014-01-17";

        DataSchemaNode schemaContNode = schemaContext;

        assertTrue(schemaContNode instanceof ContainerSchemaNode);

        DataContainerNodeAttrBuilder<NodeIdentifier, ContainerNode> dataContSchemaContNode = Builders
                .containerBuilder((ContainerSchemaNode) schemaContNode);

        DataContainerNodeAttrBuilder<NodeIdentifier, ContainerNode> modul1 = buildContBuilderMod1(
                "module:one", rev, "cont_m1", "contB_m1", "lf1_m1",
                "lf1 m1 value");
        dataContSchemaContNode.withChild(modul1.build());

        DataContainerNodeAttrBuilder<NodeIdentifier, ContainerNode> modul2 = buildContBuilderMod1(
                "module:two", rev, "cont_m2", "contB_m2", "lf1_m2",
                "lf1 m2 value");
        dataContSchemaContNode.withChild(modul2.build());

        NormalizedNodeContext testNormalizedNodeContext = new NormalizedNodeContext(
                new InstanceIdentifierContext<DataSchemaNode>(null,
                        schemaContNode, null, schemaContext),
                dataContSchemaContNode.build());

        return testNormalizedNodeContext;
    }

    private DataContainerNodeAttrBuilder<NodeIdentifier, ContainerNode> buildContBuilderMod1(
            String uri, String rev, String cont, String contB, String lf1,
            String lf1_value) {
        QName contQname = QName.create(uri, rev, cont);
        QName contBQname = QName.create(uri, rev, contB);
        QName lf1Qname = QName.create(contQname, lf1);

        DataSchemaNode contSchemaNode = schemaContext
                .getDataChildByName(contQname);
        DataContainerNodeAttrBuilder<NodeIdentifier, ContainerNode> dataContainerNodeAttrBuilder = Builders
                .containerBuilder((ContainerSchemaNode) contSchemaNode);

        assertTrue(contSchemaNode instanceof ContainerSchemaNode);

        List<DataSchemaNode> instanceLf1_m1 = ControllerContext
                .findInstanceDataChildrenByName(
                        (DataNodeContainer) contSchemaNode,
                        lf1Qname.getLocalName());
        DataSchemaNode schemaLf1_m1 = Iterables.getFirst(instanceLf1_m1, null);

        dataContainerNodeAttrBuilder.withChild(Builders
                .leafBuilder((LeafSchemaNode) schemaLf1_m1)
                .withValue(lf1_value).build());

        DataSchemaNode contBSchemaNode = ((ContainerSchemaNode) contSchemaNode)
                .getDataChildByName(contBQname);

        DataContainerNodeAttrBuilder<NodeIdentifier, ContainerNode> dataContainerB = Builders
                .containerBuilder((ContainerSchemaNode) contBSchemaNode);

        return dataContainerNodeAttrBuilder.withChild(dataContainerB.build());
    }
}
