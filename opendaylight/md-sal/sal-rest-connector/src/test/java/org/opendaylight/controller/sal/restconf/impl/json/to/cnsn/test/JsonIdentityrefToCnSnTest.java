/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.restconf.impl.json.to.cnsn.test;

import org.junit.BeforeClass;
import org.junit.Test;
import org.opendaylight.controller.sal.restconf.impl.test.YangAndXmlAndDataSchemaLoader;

public class JsonIdentityrefToCnSnTest extends YangAndXmlAndDataSchemaLoader {

    @BeforeClass
    public static void initialize() {
        dataLoad("/json-to-cnsn/identityref", 2, "identityref-module", "cont");
    }

    @Test
    public void jsonIdentityrefToCompositeNode() {
//        Node<?> node = TestUtils.readInputToCnSn("/json-to-cnsn/identityref/json/data.json", false,
//                JsonToCompositeNodeProvider.INSTANCE);
//        assertNotNull(node);
//
//        TestUtils.normalizeCompositeNode(node, modules, searchedModuleName + ":" + searchedDataSchemaName);
//
//        assertEquals("cont", node.getNodeType().getLocalName());
//
//        assert(node instanceof CompositeNode);
//        List<Node<?>> childs = ((CompositeNode)node).getValue();
//        assertEquals(1, childs.size());
//        Node<?> nd = childs.iterator().next();
//        assertTrue(nd instanceof CompositeNode);
//        assertEquals("cont1", nd.getNodeType().getLocalName());
//
//        childs = ((CompositeNode) nd).getValue();
//        assertEquals(4, childs.size());
//        SimpleNode<?> lf11 = null;
//        SimpleNode<?> lf12 = null;
//        SimpleNode<?> lf13 = null;
//        SimpleNode<?> lf14 = null;
//        for (Node<?> child : childs) {
//            assertTrue(child instanceof SimpleNode);
//            if (child.getNodeType().getLocalName().equals("lf11")) {
//                lf11 = (SimpleNode<?>) child;
//            } else if (child.getNodeType().getLocalName().equals("lf12")) {
//                lf12 = (SimpleNode<?>) child;
//            } else if (child.getNodeType().getLocalName().equals("lf13")) {
//                lf13 = (SimpleNode<?>) child;
//            } else if (child.getNodeType().getLocalName().equals("lf14")) {
//                lf14 = (SimpleNode<?>) child;
//            }
//        }
//
//        assertTrue(lf11.getValue() instanceof QName);
//        assertEquals("iden", ((QName) lf11.getValue()).getLocalName());
//        assertEquals("identity:module", ((QName) lf11.getValue()).getNamespace().toString());
//
//        assertTrue(lf12.getValue() instanceof QName);
//        assertEquals("iden_local", ((QName) lf12.getValue()).getLocalName());
//        assertEquals("identityref:module", ((QName) lf12.getValue()).getNamespace().toString());
//
//        assertTrue(lf13.getValue() instanceof QName);
//        assertEquals("iden_local", ((QName) lf13.getValue()).getLocalName());
//        assertEquals("identityref:module", ((QName) lf13.getValue()).getNamespace().toString());
//
//        assertTrue(lf14.getValue() instanceof QName);
//        assertEquals("iden_local", ((QName) lf14.getValue()).getLocalName());
//        assertEquals("identity:module", ((QName) lf14.getValue()).getNamespace().toString());
    }

}
