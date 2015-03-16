/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.restconf.impl.test;

import org.junit.BeforeClass;
import org.junit.Test;

public class NormalizeNodeTest extends YangAndXmlAndDataSchemaLoader {

    @BeforeClass
    public static void initialization() {
        dataLoad("/normalize-node/yang/");
    }

//    @Test(expected = RestconfDocumentedException.class)
//    public void namespaceNotNullAndInvalidNamespaceAndNoModuleNameTest() {

//        TestUtils.normalizeCompositeNode(prepareCnSn("wrongnamespace"), modules, schemaNodePath);
//    }

    @Test
    public void namespaceNullTest() {

//        TestUtils.normalizeCompositeNode(prepareCnSn(null), modules, schemaNodePath);
    }

    @Test
    public void namespaceValidNamespaceTest() {

//        TestUtils.normalizeCompositeNode(prepareCnSn("normalize:node:module"), modules, schemaNodePath);
    }

    @Test
    public void namespaceValidModuleNameTest() {

//        TestUtils.normalizeCompositeNode(prepareCnSn("normalize-node-module"), modules, schemaNodePath);
    }

//    private CompositeNode prepareCnSn(final String namespace) {
//        URI uri = null;
//        if (namespace != null) {
//            try {
//                uri = new URI(namespace);
//            } catch (URISyntaxException e) {
//            }
//            assertNotNull(uri);
//        }
//
//        SimpleNodeWrapper lf1 = new SimpleNodeWrapper(uri, "lf1", 43);
//        CompositeNodeWrapper cont = new CompositeNodeWrapper(uri, "cont");
//        cont.addValue(lf1);
//
//        return cont;
//    }

}
