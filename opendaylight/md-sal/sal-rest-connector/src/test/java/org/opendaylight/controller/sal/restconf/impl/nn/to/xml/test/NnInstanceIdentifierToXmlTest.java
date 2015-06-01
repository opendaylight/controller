/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.restconf.impl.nn.to.xml.test;

import static org.junit.Assert.assertNotNull;
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
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifierWithPredicates;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;
import org.opendaylight.yangtools.yang.data.api.schema.DataContainerChild;
import org.opendaylight.yangtools.yang.data.api.schema.LeafSetEntryNode;
import org.opendaylight.yangtools.yang.data.api.schema.MapEntryNode;
import org.opendaylight.yangtools.yang.data.api.schema.MapNode;
import org.opendaylight.yangtools.yang.data.impl.schema.Builders;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.api.CollectionNodeBuilder;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.api.DataContainerNodeAttrBuilder;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.api.ListNodeBuilder;
import org.opendaylight.yangtools.yang.model.api.ContainerSchemaNode;
import org.opendaylight.yangtools.yang.model.api.DataNodeContainer;
import org.opendaylight.yangtools.yang.model.api.DataSchemaNode;
import org.opendaylight.yangtools.yang.model.api.LeafListSchemaNode;
import org.opendaylight.yangtools.yang.model.api.LeafSchemaNode;
import org.opendaylight.yangtools.yang.model.api.ListSchemaNode;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;

import com.google.common.collect.Iterables;

public class NnInstanceIdentifierToXmlTest extends AbstractBodyReaderTest {

    private static SchemaContext schemaContext;
    NormalizedNodeXmlBodyWriter xmlBodyWriter;

    public NnInstanceIdentifierToXmlTest() throws NoSuchFieldException,
            SecurityException {
        super();
        xmlBodyWriter = new NormalizedNodeXmlBodyWriter();
    }

    @BeforeClass
    public static void initialization() throws URISyntaxException {
        schemaContext = schemaContextLoader("/instanceidentifier/yang",
                schemaContext);
        controllerContext.setSchemas(schemaContext);
    }

    @Test
    public void nnAsYangInstanceIdentifierAugmentLeafList()
            throws WebApplicationException, IOException, URISyntaxException {
        NormalizedNodeContext normalizedNodeContext = prepareNNCLeafList();

        OutputStream output = new ByteArrayOutputStream();

        xmlBodyWriter.writeTo(normalizedNodeContext, null, null, null,
                mediaType, null, output);

        assertNotNull(output);

        assertTrue(output.toString().contains("<cont xmlns="));
        assertTrue(output.toString().contains(
                '"' + "instance:identifier:module" + '"'));
        assertTrue(output.toString().contains(">"));

        assertTrue(output.toString().contains("<cont1>"));

        assertTrue(output.toString().contains("<lf11 xmlns="));
        assertTrue(output.toString().contains(
                '"' + "augment:module:leaf:list" + '"'));
        assertTrue(output.toString().contains(">"));
        assertTrue(output.toString().contains("/instanceidentifier/"));
        assertTrue(output.toString().contains("</lf11>"));

        assertTrue(output.toString().contains("<lflst11 xmlns="));
        assertTrue(output.toString().contains(
                '"' + "augment:module:leaf:list" + '"'));
        assertTrue(output.toString().contains(">"));
        assertTrue(output.toString().contains("lflst11 value"));
        assertTrue(output.toString().contains("</lflst11>"));

        assertTrue(output.toString().contains("</cont1>"));
        assertTrue(output.toString().contains("</cont>"));
    }

    private NormalizedNodeContext prepareNNCLeafList()
            throws URISyntaxException {
        QName cont = QName.create("instance:identifier:module", "2014-01-17",
                "cont");
        QName cont1 = QName.create("instance:identifier:module", "2014-01-17",
                "cont1");
        QName lflst11 = QName.create("augment:module:leaf:list", "2014-01-17",
                "lflst11");
        QName lf11 = QName.create("augment:module:leaf:list", "2014-01-17",
                "lf11");

        DataSchemaNode schemaCont = schemaContext.getDataChildByName(cont);

        DataContainerNodeAttrBuilder<NodeIdentifier, ContainerNode> dataCont = Builders
                .containerBuilder((ContainerSchemaNode) schemaCont);

        DataSchemaNode schemaCont1 = ((ContainerSchemaNode) schemaCont)
                .getDataChildByName(cont1);

        DataContainerNodeAttrBuilder<NodeIdentifier, ContainerNode> dataCont1 = Builders
                .containerBuilder((ContainerSchemaNode) schemaCont1);

        List<DataSchemaNode> instanceLfLst11 = ControllerContext
                .findInstanceDataChildrenByName(
                        (DataNodeContainer) schemaCont1, lflst11.getLocalName());

        DataSchemaNode lfLst11Schema = Iterables
                .getFirst(instanceLfLst11, null);
        ListNodeBuilder<Object, LeafSetEntryNode<Object>> lfLst11Data = Builders
                .leafSetBuilder((LeafListSchemaNode) lfLst11Schema);

        lfLst11Data.withChild(Builders
                .leafSetEntryBuilder((LeafListSchemaNode) lfLst11Schema)
                .withValue("lflst11 value").build());
        dataCont1.withChild(lfLst11Data.build());

        List<DataSchemaNode> instanceLf11 = ControllerContext
                .findInstanceDataChildrenByName(
                        (DataNodeContainer) schemaCont1, lf11.getLocalName());
        DataSchemaNode lf11Schema = Iterables.getFirst(instanceLf11, null);

        dataCont1.withChild(Builders.leafBuilder((LeafSchemaNode) lf11Schema)
                .withValue("/instanceidentifier/").build());
        dataCont.withChild(dataCont1.build());

        NormalizedNodeContext testNormalizedNodeContext = new NormalizedNodeContext(
                new InstanceIdentifierContext<DataSchemaNode>(null, schemaCont,
                        null, schemaContext), dataCont.build());

        return testNormalizedNodeContext;
    }

    @Test
    public void nnAsYangInstanceIdentifierAugment()
            throws WebApplicationException, IOException, URISyntaxException {

        NormalizedNodeContext normalizedNodeContext = preparNNC();
        NormalizedNodeXmlBodyWriter xmlBodyWriter = new NormalizedNodeXmlBodyWriter();
        OutputStream output = new ByteArrayOutputStream();

        xmlBodyWriter.writeTo(normalizedNodeContext, null, null, null,
                mediaType, null, output);

        assertNotNull(output);

        assertTrue(output.toString().contains("<cont xmlns="));
        assertTrue(output.toString().contains(
                '"' + "instance:identifier:module" + '"'));
        assertTrue(output.toString().contains(">"));

        assertTrue(output.toString().contains("<cont1>"));

        assertTrue(output.toString().contains("<lst11 xmlns="));
        assertTrue(output.toString().contains('"' + "augment:module" + '"'));
        assertTrue(output.toString().contains(">"));

        assertTrue(output.toString().contains(
                "<keyvalue111>keyvalue111</keyvalue111>"));
        assertTrue(output.toString().contains(
                "<keyvalue112>keyvalue112</keyvalue112>"));

        assertTrue(output.toString().contains("<lf111 xmlns="));
        assertTrue(output.toString().contains(
                '"' + "augment:augment:module" + '"'));
        assertTrue(output.toString().contains(">/cont/cont1/lf12</lf111>"));

        assertTrue(output.toString().contains("<lf112 xmlns="));
        assertTrue(output.toString().contains(
                '"' + "augment:augment:module" + '"'));
        assertTrue(output.toString().contains(">lf12 value</lf112>"));

        assertTrue(output.toString().contains("</lst11></cont1></cont>"));
    }

    private NormalizedNodeContext preparNNC() {
        QName cont = QName.create("instance:identifier:module", "2014-01-17",
                "cont");
        QName cont1 = QName.create("instance:identifier:module", "2014-01-17",
                "cont1");
        QName lst11 = QName.create("augment:module", "2014-01-17", "lst11");
        QName lf11 = QName.create("augment:augment:module", "2014-01-17",
                "lf111");
        QName lf12 = QName.create("augment:augment:module", "2014-01-17",
                "lf112");
        QName keyvalue111 = QName.create("augment:module", "2014-01-17",
                "keyvalue111");
        QName keyvalue112 = QName.create("augment:module", "2014-01-17",
                "keyvalue112");

        DataSchemaNode schemaCont = schemaContext.getDataChildByName(cont);

        DataContainerNodeAttrBuilder<NodeIdentifier, ContainerNode> dataCont = Builders
                .containerBuilder((ContainerSchemaNode) schemaCont);

        DataSchemaNode schemaCont1 = ((ContainerSchemaNode) schemaCont)
                .getDataChildByName(cont1);

        DataContainerNodeAttrBuilder<NodeIdentifier, ContainerNode> dataCont1 = Builders
                .containerBuilder((ContainerSchemaNode) schemaCont1);

        List<DataSchemaNode> instanceLst11 = ControllerContext
                .findInstanceDataChildrenByName(
                        (DataNodeContainer) schemaCont1, lst11.getLocalName());
        DataSchemaNode lst11Schema = Iterables.getFirst(instanceLst11, null);

        CollectionNodeBuilder<MapEntryNode, MapNode> dataLst11 = Builders
                .mapBuilder((ListSchemaNode) lst11Schema);

        DataContainerNodeAttrBuilder<NodeIdentifierWithPredicates, MapEntryNode> dataLst11Vaule = Builders
                .mapEntryBuilder((ListSchemaNode) lst11Schema);

        dataLst11Vaule.withChild(buildLeaf(lst11Schema, keyvalue111, dataLst11,
                "keyvalue111"));

        dataLst11Vaule.withChild(buildLeaf(lst11Schema, keyvalue112, dataLst11,
                "keyvalue112"));

        dataLst11Vaule.withChild(buildLeaf(lst11Schema, lf11, dataLst11,
                "/cont/cont1/lf12"));

        dataLst11Vaule.withChild(buildLeaf(lst11Schema, lf12, dataLst11,
                "lf12 value"));

        dataLst11.withChild(dataLst11Vaule.build());

        dataCont1.withChild(dataLst11.build());
        dataCont.withChild(dataCont1.build());

        NormalizedNodeContext testNormalizedNodeContext = new NormalizedNodeContext(
                new InstanceIdentifierContext<DataSchemaNode>(null, schemaCont,
                        null, schemaContext), dataCont.build());

        return testNormalizedNodeContext;
    }

    private DataContainerChild<?, ?> buildLeaf(DataSchemaNode lst11Schema,
            QName qname,
            CollectionNodeBuilder<MapEntryNode, MapNode> dataLst11, Object value) {

        List<DataSchemaNode> instanceLf = ControllerContext
                .findInstanceDataChildrenByName(
                        (DataNodeContainer) lst11Schema, qname.getLocalName());
        DataSchemaNode schemaLf = Iterables.getFirst(instanceLf, null);

        return Builders.leafBuilder((LeafSchemaNode) schemaLf).withValue(value)
                .build();
    }

    @Override
    protected MediaType getMediaType() {
        // TODO Auto-generated method stub
        return null;
    }
}
