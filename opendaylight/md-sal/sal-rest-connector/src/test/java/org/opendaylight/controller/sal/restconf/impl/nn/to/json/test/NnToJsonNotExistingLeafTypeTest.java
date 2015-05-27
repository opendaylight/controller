/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.restconf.impl.nn.to.json.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.opendaylight.controller.sal.restconf.impl.InstanceIdentifierContext;
import org.opendaylight.controller.sal.restconf.impl.NormalizedNodeContext;
import org.opendaylight.controller.sal.restconf.impl.test.DummyType;
import org.opendaylight.controller.sal.restconf.impl.test.TestUtils;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNodes;
import org.opendaylight.yangtools.yang.data.impl.schema.Builders;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.api.DataContainerNodeAttrBuilder;
import org.opendaylight.yangtools.yang.model.api.ContainerSchemaNode;
import org.opendaylight.yangtools.yang.model.api.DataSchemaNode;
import org.opendaylight.yangtools.yang.model.api.LeafSchemaNode;
import org.opendaylight.yangtools.yang.model.api.SchemaPath;
import org.opendaylight.yangtools.yang.parser.builder.impl.ContainerSchemaNodeBuilder;
import org.opendaylight.yangtools.yang.parser.builder.impl.LeafSchemaNodeBuilder;

public class NnToJsonNotExistingLeafTypeTest {

    @Test
    public void incorrectTopLevelElementTest() {
        NormalizedNodeContext normalizedNodeContext = prepareNormalizedNode();
        assertNotNull(normalizedNodeContext);
        assertEquals(normalizedNodeContext.getData().getNodeType()
                .getLocalName(), "cont");

        String output = NormalizedNodes.toStringTree(normalizedNodeContext
                .getData());
        assertNotNull(output);
        assertTrue(output.contains("lf1"));
    }

    private NormalizedNodeContext prepareNormalizedNode() {
        QName lf1 = QName.create("simple:uri", "2012-12-17", "lf1");

        DataSchemaNode contSchemaNode = prepareDataSchemaNode();

        assertTrue(contSchemaNode instanceof ContainerSchemaNode);
        ContainerSchemaNode conContSchemaNode = (ContainerSchemaNode) contSchemaNode;

        DataContainerNodeAttrBuilder<NodeIdentifier, ContainerNode> contContBuilder = Builders
                .containerBuilder(conContSchemaNode);

        DataSchemaNode lf1SchemaNode = conContSchemaNode
                .getDataChildByName(lf1);
        assertTrue(lf1SchemaNode instanceof LeafSchemaNode);

        String lf1String = "";
        contContBuilder.withChild(Builders
                .leafBuilder((LeafSchemaNode) lf1SchemaNode)
                .withValue(lf1String).build());

        NormalizedNodeContext testNormalizedNodeContext = new NormalizedNodeContext(
                new InstanceIdentifierContext<DataSchemaNode>(null,
                        contSchemaNode, null, null), contContBuilder.build());

        return testNormalizedNodeContext;
    }

    private DataSchemaNode prepareDataSchemaNode() {
        ContainerSchemaNodeBuilder contBuild = new ContainerSchemaNodeBuilder(
                "module", 1, TestUtils.buildQName("cont", "simple:uri",
                        "2012-12-17"), SchemaPath.create(true,
                        QName.create("dummy")));
        LeafSchemaNodeBuilder leafBuild = new LeafSchemaNodeBuilder("module",
                2, TestUtils.buildQName("lf1", "simple:uri", "2012-12-17"),
                SchemaPath.create(true, QName.create("dummy")));
        leafBuild.setType(new DummyType());
        leafBuild.setConfiguration(true);

        contBuild.addChildNode(leafBuild);
        return contBuild.build();
    }

}
