/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.restconf.impl.test;

import javax.ws.rs.ext.MessageBodyReader;
import org.junit.Test;
import org.opendaylight.controller.sal.restconf.impl.RestconfError.ErrorTag;
import org.opendaylight.controller.sal.restconf.impl.RestconfError.ErrorType;
import org.opendaylight.yangtools.yang.data.api.Node;

/**
 * If more then one data element with equal name exists where container or leaf schema node should be present the
 * RestconfDocumentedException has to be raised
 *
 * Tests for BUG 1204
 */
public class MultipleEqualNamesForDataNodesTest {

    @Test
    public void multipleEqualNameDataNodeTestForContainerJsonTest() {
//        multipleEqualNameDataNodeTest("/equal-data-node-names/equal-name-data-for-container.json",
//                ErrorType.APPLICATION, ErrorTag.BAD_ELEMENT, JsonToCompositeNodeProvider.INSTANCE);
    }

    @Test
    public void multipleEqualNameDataNodeTestForLeafJsonTest() {
//        multipleEqualNameDataNodeTest("/equal-data-node-names/equal-name-data-for-leaf.json", ErrorType.PROTOCOL,
//                ErrorTag.MALFORMED_MESSAGE, JsonToCompositeNodeProvider.INSTANCE);
    }

    @Test
    public void multipleEqualNameDataNodeTestForContainerXmlTest() {
//        multipleEqualNameDataNodeTest("/equal-data-node-names/equal-name-data-for-container.xml",
//                ErrorType.APPLICATION, ErrorTag.BAD_ELEMENT, XmlToCompositeNodeProvider.INSTANCE);
    }

    @Test
    public void multipleEqualNameDataNodeTestForLeafXmlTest() {
//        multipleEqualNameDataNodeTest("/equal-data-node-names/equal-name-data-for-leaf.xml", ErrorType.APPLICATION,
//                ErrorTag.BAD_ELEMENT, XmlToCompositeNodeProvider.INSTANCE);
    }

    private void multipleEqualNameDataNodeTest(final String path, final ErrorType errorType, final ErrorTag errorTag,
            final MessageBodyReader<Node<?>> messageBodyReader) {
//        try {
//            Node<?> node = TestUtils.readInputToCnSn(path, false, messageBodyReader);
//            assertNotNull(node);
//
//            Set<Module> modules = null;
//            modules = TestUtils.loadModulesFrom("/equal-data-node-names/yang");
//            assertNotNull(modules);
//
//            TestUtils.normalizeCompositeNode(node, modules, "equal-data-node-names" + ":" + "cont");
//            fail("Exception RestconfDocumentedException should be raised");
//        } catch (RestconfDocumentedException e) {
//            List<RestconfError> errors = e.getErrors();
//            assertNotNull(errors);
//
//            assertEquals(1, errors.size());
//
//            RestconfError restconfError = errors.get(0);
//
//            assertEquals(errorType, restconfError.getErrorType());
//            assertEquals(errorTag, restconfError.getErrorTag());
//        }
    }

}
