/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.restconf.impl.xml.to.cnsn.test;

import org.junit.Test;

public class XmlAugmentedElementToCnSnTest {

    @Test
    public void loadDataAugmentedSchemaMoreEqualNamesTest() {
        loadAndNormalizeData("/common/augment/xml/dataa.xml", "/common/augment/yang", "main", "cont");
        loadAndNormalizeData("/common/augment/xml/datab.xml", "/common/augment/yang", "main", "cont");
    }

    private void loadAndNormalizeData(final String xmlPath, final String yangPath, final String topLevelElementName, final String moduleName) {
//        Node<?> node = TestUtils.readInputToCnSn(xmlPath, false,
//                XmlToCompositeNodeProvider.INSTANCE);
//        assertTrue(node instanceof CompositeNode);
//        CompositeNode cnSn = (CompositeNode)node;
//
//        Set<Module> modules = TestUtils.loadModulesFrom(yangPath);
//
//        assertNotNull(modules);
//        TestUtils.normalizeCompositeNode(cnSn, modules, topLevelElementName + ":" + moduleName);
    }

}
