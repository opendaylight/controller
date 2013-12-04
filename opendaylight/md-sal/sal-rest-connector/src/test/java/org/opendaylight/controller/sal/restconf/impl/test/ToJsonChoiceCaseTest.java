package org.opendaylight.controller.sal.restconf.impl.test;

import static org.junit.Assert.assertTrue;

import java.io.IOException;

import javax.ws.rs.WebApplicationException;

import org.junit.*;

public class ToJsonChoiceCaseTest extends YangAndXmlAndDataSchemaLoader {

    @BeforeClass
    public static void initialization() {
        dataLoad("/yang-to-json-conversion/choice");
    }

    /**
     * Test when some data are in one case node and other in another. This isn't
     * correct. Next Json validator should return error because nodes has to be
     * from one case below concrete choice.
     * 
     */
    @Test
    public void nodeSchemasOnVariousChoiceCasePathTest() {
        try {
            TestUtils.writeCompNodeWithSchemaContextToJson(
                    TestUtils.loadCompositeNode("/yang-to-json-conversion/choice/xml/data_various_path_err.xml"),
                    "/yang-to-json-conversion/choice/xml", modules, dataSchemaNode);
        } catch (WebApplicationException | IOException e) {
            // shouldn't end here
            assertTrue(false);
        }
    }

    /**
     * Test when some data are in one case node and other in another.
     * Additionally data are loadef from various choices. This isn't correct.
     * Next Json validator should return error because nodes has to be from one
     * case below concrete choice.
     * 
     */
    @Test
    public void nodeSchemasOnVariousChoiceCasePathAndMultipleChoicesTest() {
        try {
            TestUtils
                    .writeCompNodeWithSchemaContextToJson(
                            TestUtils
                                    .loadCompositeNode("/yang-to-json-conversion/choice/xml/data_more_choices_same_level_various_paths_err.xml"),
                            "/yang-to-json-conversion/choice/xml", modules, dataSchemaNode);
        } catch (WebApplicationException | IOException e) {
            // shouldn't end here
            assertTrue(false);
        }
    }

    /**
     * Test when second level data are red first, then first and at the end
     * third level. Level represents pass through couple choice-case
     */

    @Test
    public void nodeSchemasWithRandomOrderAccordingLevel() {
        try {
            TestUtils.writeCompNodeWithSchemaContextToJson(
                    TestUtils.loadCompositeNode("/yang-to-json-conversion/choice/xml/data_random_level.xml"),
                    "/yang-to-json-conversion/choice/xml", modules, dataSchemaNode);

        } catch (WebApplicationException | IOException e) {
            // shouldn't end here
            assertTrue(false);
        }
    }

    /**
     * Test when element from no first case is used
     */
    @Test
    public void nodeSchemasNotInFirstCase() {
        try {
            TestUtils.writeCompNodeWithSchemaContextToJson(
                    TestUtils.loadCompositeNode("/yang-to-json-conversion/choice/xml/data_no_first_case.xml"),
                    "/yang-to-json-conversion/choice/xml", modules, dataSchemaNode);
        } catch (WebApplicationException | IOException e) {
            // shouldn't end here
            assertTrue(false);
        }
    }

    /**
     * Test when element in case is list
     */
    @Test
    public void nodeSchemaAsList() {
        try {
            TestUtils.writeCompNodeWithSchemaContextToJson(
                    TestUtils.loadCompositeNode("/yang-to-json-conversion/choice/xml/data_list.xml"),
                    "/yang-to-json-conversion/choice/xml", modules, dataSchemaNode);
        } catch (WebApplicationException | IOException e) {
            // shouldn't end here
            assertTrue(false);
        }
    }

    /**
     * Test when element in case is container
     */
    @Test
    public void nodeSchemaAsContainer() {
        try {
            TestUtils.writeCompNodeWithSchemaContextToJson(
                    TestUtils.loadCompositeNode("/yang-to-json-conversion/choice/xml/data_container.xml"),
                    "/yang-to-json-conversion/choice/xml", modules, dataSchemaNode);
        } catch (WebApplicationException | IOException e) {
            // shouldn't end here
            assertTrue(false);
        }
    }

    /**
     * Test when element in case is leaflist
     */
    @Test
    public void nodeSchemaAsLeafList() {
        try {
            TestUtils.writeCompNodeWithSchemaContextToJson(
                    TestUtils.loadCompositeNode("/yang-to-json-conversion/choice/xml/data_leaflist.xml"),
                    "/yang-to-json-conversion/choice/xml", modules, dataSchemaNode);
        } catch (WebApplicationException | IOException e) {
            // shouldn't end here
            assertTrue(false);
        }
    }

    /**
     * 
     */
    @Test
    public void nodeSchemasInMultipleChoicesTest() {
        try {
            TestUtils
                    .writeCompNodeWithSchemaContextToJson(TestUtils
                            .loadCompositeNode("/yang-to-json-conversion/choice/xml/data_more_choices_same_level.xml"),
                            "/yang-to-json-conversion/choice/xml", modules, dataSchemaNode);
        } catch (WebApplicationException | IOException e) {
            // shouldn't end here
            assertTrue(false);
        }
    }

    /**
     * Test whether is possible to find data schema for node which is specified
     * as dirrect subnode of choice (case without CASE key word)
     */
    @Test
    public void nodeSchemasInCaseNotDefinedWithCaseKeyword() {
        try {
            TestUtils.writeCompNodeWithSchemaContextToJson(TestUtils
                    .loadCompositeNode("/yang-to-json-conversion/choice/xml/data_case_defined_without_case.xml"),
                    "/yang-to-json-conversion/choice/xml", modules, dataSchemaNode);
        } catch (WebApplicationException | IOException e) {
            // shouldn't end here
            assertTrue(false);
        }
    }

    /**
     * Test of multiple use of choices
     */
    @Test
    public void nodeSchemasInThreeChoicesAtSameLevel() {
        try {
            TestUtils.writeCompNodeWithSchemaContextToJson(TestUtils
                    .loadCompositeNode("/yang-to-json-conversion/choice/xml/data_three_choices_same_level.xml"),
                    "/yang-to-json-conversion/choice/xml", modules, dataSchemaNode);
        } catch (WebApplicationException | IOException e) {
            // shouldn't end here
            assertTrue(false);
        }
    }
}
