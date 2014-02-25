/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.dom.broker.util.operations;

import java.util.Arrays;
import java.util.Collection;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.opendaylight.yangtools.yang.model.api.ContainerSchemaNode;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

@RunWith(Parameterized.class)
public class YangDataOperationsNegativeTest extends YangDataOperationsTest{

    private static final String XML_NEG_FOLDER_NAME = "/xmls-negative";
    private final Class<? extends DataModificationException> expected;

    @Parameterized.Parameters()
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][] {

                // Container
                { "/containerTest_createContainer", DataModificationException.DataMissingException.class },
        });
    }

    public YangDataOperationsNegativeTest(String testDir, Class<? extends DataModificationException> e) throws Exception {
        super(testDir);
        this.expected = e;
    }

    @Override
    protected String getXmlFolderName() {
        return XML_NEG_FOLDER_NAME;
    }

    @Test
    public void testModification() throws Exception {
        try {
            DataOperations.modify((ContainerSchemaNode) containerNode,
                    currentConfig.orNull(), modification.orNull(), modifyAction);
            fail("Negative test for " + testDirName + "should have failed");
        } catch (DataModificationException e) {
            assertEquals(e.getClass(), expected);
        }

    }
}
