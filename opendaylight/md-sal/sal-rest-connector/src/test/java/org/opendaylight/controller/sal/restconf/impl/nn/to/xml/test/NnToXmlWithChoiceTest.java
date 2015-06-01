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
import org.opendaylight.yangtools.yang.data.api.schema.ChoiceNode;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;
import org.opendaylight.yangtools.yang.data.impl.schema.Builders;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.api.DataContainerNodeAttrBuilder;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.api.DataContainerNodeBuilder;
import org.opendaylight.yangtools.yang.model.api.ChoiceSchemaNode;
import org.opendaylight.yangtools.yang.model.api.ContainerSchemaNode;
import org.opendaylight.yangtools.yang.model.api.DataNodeContainer;
import org.opendaylight.yangtools.yang.model.api.DataSchemaNode;
import org.opendaylight.yangtools.yang.model.api.LeafSchemaNode;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;

import com.google.common.collect.Iterables;

public class NnToXmlWithChoiceTest extends AbstractBodyReaderTest {

    private final NormalizedNodeXmlBodyWriter xmlBodyWriter;
    private static SchemaContext schemaContext;

    public NnToXmlWithChoiceTest() throws NoSuchFieldException,
            SecurityException {
        super();
        xmlBodyWriter = new NormalizedNodeXmlBodyWriter();
    }

    @BeforeClass
    public static void initialization() {
        schemaContext = schemaContextLoader("/nn-to-xml/choice", schemaContext);
        controllerContext.setSchemas(schemaContext);
    }

    @Test
    public void cnSnToXmlWithYangChoice() {
        NormalizedNodeContext normalizedNodeContext = prepareNNC("lf1",
                "String data1");
        OutputStream output = new ByteArrayOutputStream();
        try {
            xmlBodyWriter.writeTo(normalizedNodeContext, null, null, null,
                    mediaType, null, output);
        } catch (WebApplicationException | IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        assertTrue(output.toString().contains("<lf1>String data1</lf1>"));

        normalizedNodeContext = prepareNNC("lf2", "String data2");
        output = new ByteArrayOutputStream();
        try {
            xmlBodyWriter.writeTo(normalizedNodeContext, null, null, null,
                    mediaType, null, output);
        } catch (WebApplicationException | IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        assertTrue(output.toString().contains("<lf2>String data2</lf2>"));
    }

    private NormalizedNodeContext prepareNNC(final String name,
            final Object value) {

        QName contQname = QName.create("module:with:choice", "2013-12-18",
                "cont");
        QName lf = QName.create("module:with:choice", "2013-12-18", name);
        QName choA = QName.create("module:with:choice", "2013-12-18", "choA");

        DataSchemaNode contSchemaNode = schemaContext
                .getDataChildByName(contQname);
        DataContainerNodeAttrBuilder<NodeIdentifier, ContainerNode> dataContainerNodeAttrBuilder = Builders
                .containerBuilder((ContainerSchemaNode) contSchemaNode);

        DataSchemaNode choiceSchemaNode = ((ContainerSchemaNode) contSchemaNode)
                .getDataChildByName(choA);
        assertTrue(choiceSchemaNode instanceof ChoiceSchemaNode);

        DataContainerNodeBuilder<NodeIdentifier, ChoiceNode> dataChoice = Builders
                .choiceBuilder((ChoiceSchemaNode) choiceSchemaNode);

        List<DataSchemaNode> instanceLf = ControllerContext
                .findInstanceDataChildrenByName(
                        (DataNodeContainer) contSchemaNode, lf.getLocalName());
        DataSchemaNode schemaLf = Iterables.getFirst(instanceLf, null);

        dataChoice.withChild(Builders.leafBuilder((LeafSchemaNode) schemaLf)
                .withValue(value).build());

        dataContainerNodeAttrBuilder.withChild(dataChoice.build());

        NormalizedNodeContext testNormalizedNodeContext = new NormalizedNodeContext(
                new InstanceIdentifierContext<DataSchemaNode>(null,
                        contSchemaNode, null, schemaContext),
                dataContainerNodeAttrBuilder.build());

        return testNormalizedNodeContext;
    }

    @Override
    protected MediaType getMediaType() {
        // TODO Auto-generated method stub
        return null;
    }
}
