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
import static org.junit.Assert.fail;

@RunWith(Parameterized.class)
public class YangDataOperationsNegativeTest extends YangDataOperationsTest{

    private static final String XML_NEG_FOLDER_NAME = "/xmls-negative";
    private final Class<? extends DataModificationException> expected;

    @Parameterized.Parameters()
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][] {

                // Container
                { "/containerTest_noneContainerActualMissing", DataModificationException.DataMissingException.class },
                { "/containerTest_createContainerActualPresent", DataModificationException.DataExistsException.class },
                { "/containerTest_deleteContainerActualMissing", DataModificationException.DataMissingException.class },
                // List
                { "/listTest_missingKey", DataModificationException.MissingElementException.class },
                { "/listTest_createListActualPresent", DataModificationException.DataExistsException.class },
                { "/listTest_deleteListActualMissing", DataModificationException.DataMissingException.class },
                { "/listTest_noneListActualMissing", DataModificationException.DataMissingException.class },
                // Leaf
                { "/leafTest_createLeafActualPresent", DataModificationException.DataExistsException.class },
                { "/leafTest_deleteLeafActualMissing", DataModificationException.DataMissingException.class },
                // Leaf list
                { "/leafListTest_createLeafActualPresent", DataModificationException.DataExistsException.class },
                { "/leafListTest_deleteLeafActualMissing", DataModificationException.DataMissingException.class },
                // ChoiceCase
                { "/containerTest_choiceModificationMultipleCasesReferenced", DataModificationException.IllegalChoiceValuesException.class },
                { "/containerTest_choiceModificationUnknownNode", DataModificationException.UnknownNodeException.class },
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
            fail("Negative test for " + testDirName + " should have failed");
        } catch (DataModificationException e) {
            e.printStackTrace();
            assertEquals(e.getClass(), expected);
        }

    }
}
