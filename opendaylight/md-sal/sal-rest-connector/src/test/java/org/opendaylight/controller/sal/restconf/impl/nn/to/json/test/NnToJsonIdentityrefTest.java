/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.restconf.impl.nn.to.json.test;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
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
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNodes;
import org.opendaylight.yangtools.yang.data.impl.schema.Builders;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.api.DataContainerNodeAttrBuilder;
import org.opendaylight.yangtools.yang.model.api.ContainerSchemaNode;
import org.opendaylight.yangtools.yang.model.api.DataNodeContainer;
import org.opendaylight.yangtools.yang.model.api.DataSchemaNode;
import org.opendaylight.yangtools.yang.model.api.LeafSchemaNode;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;

import com.google.common.collect.Iterables;

public class NnToJsonIdentityrefTest extends AbstractBodyReaderTest {

    private static SchemaContext schemaContext;
    private NormalizedNodeJsonBodyWriter jsonBodyWriter;

    public NnToJsonIdentityrefTest() throws NoSuchFieldException,
            SecurityException {
        super();
        jsonBodyWriter = new NormalizedNodeJsonBodyWriter();
    }

    @BeforeClass
    public static void initialization() {
        schemaContext = schemaContextLoader("/nn-to-json/identityref",
                schemaContext);
        controllerContext.setSchemas(schemaContext);
    }

    @Test
    public void identityrefToJsonTest() {
        QName identref = QName.create("identityref:module", "2013-12-2",
                "name_test");

        NormalizedNodeContext normalizedNodeContext = prepareNNC(identref);

        final OutputStream output = new ByteArrayOutputStream();
        try {
            jsonBodyWriter.writeTo(normalizedNodeContext, null, null, null,
                    mediaType, null, output);
        } catch (WebApplicationException | IOException e) {
            e.printStackTrace();
        }
        String json = NormalizedNodes.toStringTree(normalizedNodeContext
                .getData());

        assertNotNull(json);

        assertTrue(json
                .contains("lf1 (identityref:module?revision=2013-12-02)name_test"));
    }

    @Test(expected = ClassCastException.class)
    public void identityrefToJsonWithoutQNameTest() {
        String value = "not q name value";

        NormalizedNodeContext normalizedNodeContext = prepareNNC(value);

        final OutputStream output = new ByteArrayOutputStream();
        try {
            jsonBodyWriter.writeTo(normalizedNodeContext, null, null, null,
                    mediaType, null, output);
        } catch (WebApplicationException | IOException e) {
            e.printStackTrace();
        }
    }

    private NormalizedNodeContext prepareNNC(Object value) {
        String namespace = "identityref:module";
        String revision = "2013-12-2";
        QName cont = QName.create(namespace, revision, "cont");
        QName cont1 = QName.create(namespace, revision, "cont1");
        QName lf1 = QName.create(namespace, revision, "lf1");

        DataSchemaNode contSchemaNode = schemaContext.getDataChildByName(cont);
        DataContainerNodeAttrBuilder<NodeIdentifier, ContainerNode> dataContainerNodeAttrBuilder = Builders
                .containerBuilder((ContainerSchemaNode) contSchemaNode);

        DataSchemaNode cont1SchemaNode = ((ContainerSchemaNode) contSchemaNode)
                .getDataChildByName(cont1);

        DataContainerNodeAttrBuilder<NodeIdentifier, ContainerNode> dataContainer1NodeAttrBuilder = Builders
                .containerBuilder((ContainerSchemaNode) cont1SchemaNode);

        List<DataSchemaNode> instanceLf1_m1 = ControllerContext
                .findInstanceDataChildrenByName(
                        (DataNodeContainer) cont1SchemaNode, lf1.getLocalName());
        DataSchemaNode schemaLf1_m1 = Iterables.getFirst(instanceLf1_m1, null);

        dataContainer1NodeAttrBuilder.withChild(Builders
                .leafBuilder((LeafSchemaNode) schemaLf1_m1).withValue(value)
                .build());

        dataContainerNodeAttrBuilder.withChild(dataContainer1NodeAttrBuilder
                .build());

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
