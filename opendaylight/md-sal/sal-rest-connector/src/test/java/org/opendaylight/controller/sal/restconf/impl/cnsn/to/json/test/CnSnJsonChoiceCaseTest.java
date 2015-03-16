/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.restconf.impl.cnsn.to.json.test;

import org.junit.BeforeClass;
import org.junit.Test;
import org.opendaylight.controller.sal.restconf.impl.test.YangAndXmlAndDataSchemaLoader;

public class CnSnJsonChoiceCaseTest extends YangAndXmlAndDataSchemaLoader {

    @BeforeClass
    public static void initialization() {
        dataLoad("/cnsn-to-json/choice");
    }

    /**
     * Test when some data are in one case node and other in another. This isn't correct. Next Json validator should
     * return error because nodes has to be from one case below concrete choice.
     *
     */
//    @Test(expected=DataValidationException.class)
//    public void nodeSchemasOnVariousChoiceCasePathTest() {
//        testWrapper("/cnsn-to-json/choice/xml/data_various_path_err.xml", "choice-case-test:cont");
//    }

    /**
     * Test when some data are in one case node and other in another. Additionally data are loadef from various choices.
     * This isn't correct. Next Json validator should return error because nodes has to be from one case below concrete
     * choice.
     *
     */
//    @Test(expected=DataValidationException.class)
//    public void nodeSchemasOnVariousChoiceCasePathAndMultipleChoicesTest() {
//        testWrapper("/cnsn-to-json/choice/xml/data_more_choices_same_level_various_paths_err.xml",
//                "choice-case-test:cont");
//    }

    /**
     * Test when second level data are red first, then first and at the end third level. Level represents pass through
     * couple choice-case
     */

    @Test
    public void nodeSchemasWithRandomOrderAccordingLevel() {
        testWrapper("/cnsn-to-json/choice/xml/data_random_level.xml", "choice-case-test:cont");
    }

    /**
     * Test when element from no first case is used
     */
    @Test
    public void nodeSchemasNotInFirstCase() {
        testWrapper("/cnsn-to-json/choice/xml/data_no_first_case.xml", "choice-case-test:cont");
    }

    /**
     * Test when element in case is list
     */
    @Test
    public void nodeSchemaAsList() {
        testWrapper("/cnsn-to-json/choice/xml/data_list.xml", "choice-case-test:cont");
    }

    /**
     * Test when element in case is container
     */
    @Test
    public void nodeSchemaAsContainer() {
        testWrapper("/cnsn-to-json/choice/xml/data_container.xml", "choice-case-test:cont");
    }

    /**
     * Test when element in case is leaflist
     */
    @Test
    public void nodeSchemaAsLeafList() {
        testWrapper("/cnsn-to-json/choice/xml/data_leaflist.xml", "choice-case-test:cont");
    }

    /**
     *
     */
    @Test
    public void nodeSchemasInMultipleChoicesTest() {
        testWrapper("/cnsn-to-json/choice/xml/data_more_choices_same_level.xml", "choice-case-test:cont");
    }

    /**
     * Test whether is possible to find data schema for node which is specified as dirrect subnode of choice (case
     * without CASE key word)
     */
    @Test
    public void nodeSchemasInCaseNotDefinedWithCaseKeyword() {
        testWrapper("/cnsn-to-json/choice/xml/data_case_defined_without_case.xml", "choice-case-test:cont");
    }

    /**
     * Test of multiple use of choices
     */
    @Test
    public void nodeSchemasInThreeChoicesAtSameLevel() {
        testWrapper("/cnsn-to-json/choice/xml/data_three_choices_same_level.xml", "choice-case-test:cont");
    }

    private void testWrapper(final String xmlPath, final String pathToSchemaNode) {
//        Node<?> node = TestUtils.readInputToCnSn(xmlPath, XmlToCompositeNodeProvider.INSTANCE);
//        TestUtils.normalizeCompositeNode(node, modules, pathToSchemaNode);
//        try {
//            TestUtils.writeCompNodeWithSchemaContextToOutput(node, modules, dataSchemaNode,
//                    StructuredDataToJsonProvider.INSTANCE);
//        } catch (WebApplicationException | IOException e) {
//            // shouldn't end here
//            assertTrue(false);
//        }
    }
}
