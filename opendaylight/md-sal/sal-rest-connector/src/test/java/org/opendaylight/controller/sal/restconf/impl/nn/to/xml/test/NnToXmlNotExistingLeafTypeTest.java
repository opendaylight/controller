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

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;

import org.junit.Test;
import org.opendaylight.controller.sal.rest.impl.NormalizedNodeXmlBodyWriter;
import org.opendaylight.controller.sal.rest.impl.test.providers.AbstractBodyReaderTest;
import org.opendaylight.controller.sal.restconf.impl.InstanceIdentifierContext;
import org.opendaylight.controller.sal.restconf.impl.NormalizedNodeContext;
import org.opendaylight.controller.sal.restconf.impl.test.DummyType;
import org.opendaylight.controller.sal.restconf.impl.test.TestUtils;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;
import org.opendaylight.yangtools.yang.data.impl.schema.Builders;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.api.DataContainerNodeAttrBuilder;
import org.opendaylight.yangtools.yang.model.api.ContainerSchemaNode;
import org.opendaylight.yangtools.yang.model.api.DataSchemaNode;
import org.opendaylight.yangtools.yang.model.api.LeafSchemaNode;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.opendaylight.yangtools.yang.parser.builder.impl.ContainerSchemaNodeBuilder;
import org.opendaylight.yangtools.yang.parser.builder.impl.LeafSchemaNodeBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NnToXmlNotExistingLeafTypeTest extends AbstractBodyReaderTest {

    public NnToXmlNotExistingLeafTypeTest() throws NoSuchFieldException,
            SecurityException {
        super();
        // TODO Auto-generated constructor stub
    }

    private static final Logger LOG = LoggerFactory
            .getLogger(NnToXmlNotExistingLeafTypeTest.class);

    @Test
    public void incorrectTopLevelElementTest() {
        boolean nullPointerExceptionRaised = false;
        NormalizedNodeXmlBodyWriter xmlBodyWriter = new NormalizedNodeXmlBodyWriter();
        OutputStream output = new ByteArrayOutputStream();

        try {
            NormalizedNodeContext normalizedNodeContext = prepareNNC(prepareDataSchemaNode());
            xmlBodyWriter.writeTo(normalizedNodeContext, null, null, null,
                    mediaType, null, output);
        } catch (WebApplicationException | IOException e) {
            LOG.error("WebApplicationException or IOException was raised");
        } catch (NullPointerException e) {
            nullPointerExceptionRaised = true;
        }
        assertTrue(nullPointerExceptionRaised);

    }

    private NormalizedNodeContext prepareNNC(DataSchemaNode dataSchemaNode) {
        QName cont = QName.create("simple:uri", "2012-12-17", "cont");
        QName lf = QName.create("simple:uri", "2012-12-17", "lf1");

        DataSchemaNode contSchema = ((ContainerSchemaNode) dataSchemaNode)
                .getDataChildByName(cont);
        DataContainerNodeAttrBuilder<NodeIdentifier, ContainerNode> dataCont = Builders
                .containerBuilder((ContainerSchemaNode) contSchema);

        DataSchemaNode lfSchema = ((ContainerSchemaNode) dataSchemaNode)
                .getDataChildByName(lf);

        dataCont.withChild(Builders.leafBuilder((LeafSchemaNode) lfSchema)
                .withValue("any value").build());

        NormalizedNodeContext testNormalizedNodeContext = new NormalizedNodeContext(
                new InstanceIdentifierContext<DataSchemaNode>(null, contSchema,
                        null, (SchemaContext) dataSchemaNode), dataCont.build());

        return testNormalizedNodeContext;
    }

    private DataSchemaNode prepareDataSchemaNode() {
        ContainerSchemaNodeBuilder contBuild = new ContainerSchemaNodeBuilder(
                "module", 1, TestUtils.buildQName("cont", "simple:uri",
                        "2012-12-17"), null);
        LeafSchemaNodeBuilder leafBuild = new LeafSchemaNodeBuilder("module",
                2, TestUtils.buildQName("lf1", "simple:uri", "2012-12-17"),
                null);
        leafBuild.setType(new DummyType());
        leafBuild.setConfiguration(true);

        contBuild.addChildNode(leafBuild);
        return contBuild.build();
    }

    @Override
    protected MediaType getMediaType() {
        // TODO Auto-generated method stub
        return null;
    }

}
