/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.restconf.impl.nn.to.json.test;

import static org.junit.Assert.assertTrue;
import com.google.common.collect.Iterables;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URISyntaxException;
import java.util.List;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import org.junit.BeforeClass;
import org.junit.Test;
import org.opendaylight.controller.sal.rest.impl.NormalizedNodeJsonBodyWriter;
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

public class NnToJsonWithDataFromSeveralModulesTest extends
        AbstractBodyReaderTest {

    private static SchemaContext schemaContext;
    private final NormalizedNodeJsonBodyWriter jsonWriter;

    public NnToJsonWithDataFromSeveralModulesTest()
            throws NoSuchFieldException, SecurityException {
        super();
        jsonWriter = new NormalizedNodeJsonBodyWriter();
    }

    @BeforeClass
    public static void initialize() {
        schemaContext = schemaContextLoader(
                "/nn-to-json/data-of-several-modules/yang", schemaContext);
        controllerContext.setSchemas(schemaContext);
    }

    @Test
    public void dataFromSeveralModulesToJsonTest()
            throws WebApplicationException, IOException, URISyntaxException {

        final NormalizedNodeContext normalizedNodeContext = prepareNormalizedNodeContext();

        final OutputStream output = new ByteArrayOutputStream();
        jsonWriter.writeTo(normalizedNodeContext, null, null, null, mediaType,
                null, output);

        final String json = output.toString();
        System.out.println(json);
        assertTrue(json.contains("\"module2:cont_m2\":{"));
        assertTrue(json.contains("\"lf1_m2\":\"lf1 m2 value\""));
        assertTrue(json.contains("\"contB_m2\":{}"));
        assertTrue(json.contains("\"module1:cont_m1\":{"));
        assertTrue(json.contains("\"lf1_m1\":\"lf1 m1 value\""));
        assertTrue(json.contains("\"contB_m1\":{}"));
    }

    private NormalizedNodeContext prepareNormalizedNodeContext() {
        final String rev = "2014-01-17";

        final DataSchemaNode schemaContNode = schemaContext;

        assertTrue(schemaContNode instanceof ContainerSchemaNode);

        final DataContainerNodeAttrBuilder<NodeIdentifier, ContainerNode> dataContSchemaContNode = Builders
                .containerBuilder((ContainerSchemaNode) schemaContNode);

        final DataContainerNodeAttrBuilder<NodeIdentifier, ContainerNode> modul1 = buildContBuilderMod1(
                "module:one", rev, "cont_m1", "contB_m1", "lf1_m1",
                "lf1 m1 value");
        dataContSchemaContNode.withChild(modul1.build());

        final DataContainerNodeAttrBuilder<NodeIdentifier, ContainerNode> modul2 = buildContBuilderMod1(
                "module:two", rev, "cont_m2", "contB_m2", "lf1_m2",
                "lf1 m2 value");
        dataContSchemaContNode.withChild(modul2.build());

        final NormalizedNodeContext testNormalizedNodeContext = new NormalizedNodeContext(
                new InstanceIdentifierContext<DataSchemaNode>(null,
                        schemaContNode, null, schemaContext),
                dataContSchemaContNode.build());

        return testNormalizedNodeContext;
    }

    private DataContainerNodeAttrBuilder<NodeIdentifier, ContainerNode> buildContBuilderMod1(
            final String uri, final String rev, final String cont, final String contB, final String lf1,
            final String lf1_value) {
        final QName contQname = QName.create(uri, rev, cont);
        final QName contBQname = QName.create(uri, rev, contB);
        final QName lf1Qname = QName.create(contQname, lf1);

        final DataSchemaNode contSchemaNode = schemaContext
                .getDataChildByName(contQname);
        final DataContainerNodeAttrBuilder<NodeIdentifier, ContainerNode> dataContainerNodeAttrBuilder = Builders
                .containerBuilder((ContainerSchemaNode) contSchemaNode);

        assertTrue(contSchemaNode instanceof ContainerSchemaNode);

        final List<DataSchemaNode> instanceLf1_m1 = ControllerContext
                .findInstanceDataChildrenByName(
                        (DataNodeContainer) contSchemaNode,
                        lf1Qname.getLocalName());
        final DataSchemaNode schemaLf1_m1 = Iterables.getFirst(instanceLf1_m1, null);

        dataContainerNodeAttrBuilder.withChild(Builders
                .leafBuilder((LeafSchemaNode) schemaLf1_m1)
                .withValue(lf1_value).build());

        final DataSchemaNode contBSchemaNode = ((ContainerSchemaNode) contSchemaNode)
                .getDataChildByName(contBQname);

        final DataContainerNodeAttrBuilder<NodeIdentifier, ContainerNode> dataContainerB = Builders
                .containerBuilder((ContainerSchemaNode) contBSchemaNode);

        return dataContainerNodeAttrBuilder.withChild(dataContainerB.build());
    }

    @Override
    protected MediaType getMediaType() {
        return null;
    }

}
