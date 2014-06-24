/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.restconf.impl.json.to.cnsn.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.util.List;
import java.util.Set;
import org.junit.Test;
import org.opendaylight.controller.sal.rest.impl.JsonToCompositeNodeProvider;
import org.opendaylight.controller.sal.restconf.impl.RestconfDocumentedException;
import org.opendaylight.controller.sal.restconf.impl.RestconfError;
import org.opendaylight.controller.sal.restconf.impl.RestconfError.ErrorTag;
import org.opendaylight.controller.sal.restconf.impl.RestconfError.ErrorType;
import org.opendaylight.controller.sal.restconf.impl.test.TestUtils;
import org.opendaylight.yangtools.yang.data.api.CompositeNode;
import org.opendaylight.yangtools.yang.model.api.Module;

public class Bug1204Test {

    /**
     * If more then one data element with equal name exists where container or
     * leaf schema node should be present the RestconfDocumentedException has to
     * be raised
     */
    @Test
    public void multipleEqualNameDataNodeTestForContainer() {
        multipleEqualNameDataNodeTest("/bug1204/equal-name-data-for-container.json", ErrorType.APPLICATION,
                ErrorTag.BAD_ELEMENT);
    }

    @Test
    public void multipleEqualNameDataNodeTestForLeaf() {
        multipleEqualNameDataNodeTest("/bug1204/equal-name-data-for-leaf.json", ErrorType.PROTOCOL,
                ErrorTag.MALFORMED_MESSAGE);
    }

    private void multipleEqualNameDataNodeTest(String jsonPath, ErrorType errorType, ErrorTag errorTag) {
        try {
            CompositeNode compositeNode = TestUtils.readInputToCnSn(jsonPath, false,
                    JsonToCompositeNodeProvider.INSTANCE);
            assertNotNull(compositeNode);

            Set<Module> modules = null;
            modules = TestUtils.loadModulesFrom("/bug1204/yang");
            assertNotNull(modules);

            TestUtils.normalizeCompositeNode(compositeNode, modules, "bug-1204" + ":" + "cont");
            fail("Exception RestconfDocumentedException should be raised");
        } catch (RestconfDocumentedException e) {
            List<RestconfError> errors = e.getErrors();
            assertNotNull(errors);

            assertEquals(1, errors.size());

            RestconfError restconfError = errors.get(0);

            assertEquals(errorType, restconfError.getErrorType());
            assertEquals(errorTag, restconfError.getErrorTag());
        }
    }

}
