/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.restconf.impl.test;

import static org.junit.Assert.assertNotNull;
import com.google.common.collect.Iterables;
import java.util.List;
import javax.ws.rs.core.MediaType;
import org.junit.BeforeClass;
import org.junit.Test;
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

public class NormalizeNodeTest extends AbstractBodyReaderTest {

    private static SchemaContext schemaContext;

    public NormalizeNodeTest() throws NoSuchFieldException, SecurityException {
        super();
    }

    @BeforeClass
    public static void initialization() {
        schemaContext = schemaContextLoader("/normalize-node/yang/",
                schemaContext);
        controllerContext.setSchemas(schemaContext);
    }

    @Test(expected = NullPointerException.class)
    public void namespaceNotNullAndInvalidNamespaceAndNoModuleNameTest() {
        prepareNNC("wrongnamespace");
    }

    @Test(expected = NullPointerException.class)
    public void namespaceNullTest() {
        prepareNNC(null);
    }

    @Test
    public void namespaceValidNamespaceTest() {
        assertNotNull(prepareNNC("normalize:node:module"));
    }

    @Test(expected = NullPointerException.class)
    public void notValidNamespace() {
        prepareNNC("normalize-node-module");
    }

    private NormalizedNodeContext prepareNNC(final String namespace) {
        final QName cont = QName.create(namespace, "2014-01-09", "cont");
        final QName lf1 = QName.create(namespace, "2014-01-09", "lf1");

        final DataSchemaNode schemaCont = schemaContext.getDataChildByName(cont);

        final DataContainerNodeAttrBuilder<NodeIdentifier, ContainerNode> dataCont = Builders
                .containerBuilder((ContainerSchemaNode) schemaCont);

        final List<DataSchemaNode> instanceLf1 = ControllerContext
                .findInstanceDataChildrenByName((DataNodeContainer) schemaCont,
                        lf1.getLocalName());
        final DataSchemaNode lf1Schema = Iterables.getFirst(instanceLf1, null);

        dataCont.withChild(Builders.leafBuilder((LeafSchemaNode) lf1Schema)
                .withValue(45).build());

        final NormalizedNodeContext testNormalizedNodeContext = new NormalizedNodeContext(
                new InstanceIdentifierContext<DataSchemaNode>(null, schemaCont,
                        null, schemaContext), dataCont.build());

        return testNormalizedNodeContext;
    }

    @Override
    protected MediaType getMediaType() {
        return null;
    }
}
