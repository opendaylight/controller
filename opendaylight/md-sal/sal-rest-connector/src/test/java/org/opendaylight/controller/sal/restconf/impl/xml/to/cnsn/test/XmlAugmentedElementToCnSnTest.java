/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.restconf.impl.xml.to.cnsn.test;

import static org.junit.Assert.assertNotNull;

import java.util.Set;

import org.junit.Test;
import org.opendaylight.controller.sal.rest.impl.XmlToCompositeNodeProvider;
import org.opendaylight.controller.sal.restconf.impl.test.TestUtils;
import org.opendaylight.yangtools.yang.data.api.CompositeNode;
import org.opendaylight.yangtools.yang.model.api.Module;

public class XmlAugmentedElementToCnSnTest {

    @Test
    public void loadDataAugmentedSchemaMoreEqualNamesTest() {
        loadAndNormalizeData("/common/augment/xml/dataa.xml", "/common/augment/yang", "main","cont");
        loadAndNormalizeData("/common/augment/xml/datab.xml", "/common/augment/yang", "main","cont");
    }
    
    private void loadAndNormalizeData(String xmlPath, String yangPath, String topLevelElementName, String moduleName) {
        CompositeNode compNode = TestUtils.readInputToCnSn(xmlPath, false,
                XmlToCompositeNodeProvider.INSTANCE);
        assertNotNull(compNode);
        Set<Module> modules = TestUtils.loadModulesFrom(yangPath);

        assertNotNull(modules);
        TestUtils.normalizeCompositeNode(compNode, modules, topLevelElementName + ":" + moduleName);
    }


}
