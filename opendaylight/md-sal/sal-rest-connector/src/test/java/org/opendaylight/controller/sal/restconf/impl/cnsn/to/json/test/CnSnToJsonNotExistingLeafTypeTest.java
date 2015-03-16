/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.restconf.impl.cnsn.to.json.test;

import org.junit.BeforeClass;
import org.opendaylight.controller.sal.restconf.impl.test.DummyType;
import org.opendaylight.controller.sal.restconf.impl.test.TestUtils;
import org.opendaylight.controller.sal.restconf.impl.test.YangAndXmlAndDataSchemaLoader;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.model.api.DataSchemaNode;
import org.opendaylight.yangtools.yang.model.api.SchemaPath;
import org.opendaylight.yangtools.yang.parser.builder.impl.ContainerSchemaNodeBuilder;
import org.opendaylight.yangtools.yang.parser.builder.impl.LeafSchemaNodeBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CnSnToJsonNotExistingLeafTypeTest extends YangAndXmlAndDataSchemaLoader {

    private static final Logger LOG = LoggerFactory.getLogger(CnSnToJsonNotExistingLeafTypeTest.class);

    @BeforeClass
    public static void initialize() {
        dataLoad("/cnsn-to-json/simple-data-types");
    }

    private DataSchemaNode prepareDataSchemaNode() {
        final ContainerSchemaNodeBuilder contBuild = new ContainerSchemaNodeBuilder("module", 1, TestUtils.buildQName("cont",
                "simple:uri", "2012-12-17"), SchemaPath.create(true, QName.create("dummy")));
        final LeafSchemaNodeBuilder leafBuild = new LeafSchemaNodeBuilder("module", 2, TestUtils.buildQName("lf1",
                "simple:uri", "2012-12-17"), SchemaPath.create(true, QName.create("dummy")));
        leafBuild.setType(new DummyType());
        leafBuild.setConfiguration(true);

        contBuild.addChildNode(leafBuild);
        return contBuild.build();
    }

}
