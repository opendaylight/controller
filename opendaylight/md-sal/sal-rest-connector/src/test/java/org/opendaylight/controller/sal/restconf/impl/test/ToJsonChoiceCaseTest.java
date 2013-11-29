package org.opendaylight.controller.sal.restconf.impl.test;

import static org.junit.Assert.*;

import java.io.IOException;
import java.util.Set;

import javax.activation.UnsupportedDataTypeException;
import javax.ws.rs.WebApplicationException;

import org.junit.*;
import org.opendaylight.yangtools.yang.model.api.*;

public class ToJsonChoiceCaseTest {

    private static Set<Module> modules;
    private static DataSchemaNode dataSchemaNode;

    @BeforeClass
    public static void initialization() {
        modules = TestUtils.resolveModules("/yang-to-json-conversion/choice");
        Module module = TestUtils.resolveModule(null, modules);
        dataSchemaNode = TestUtils.resolveDataSchemaNode(module, null);

    }

    /**
     * Test when some data are in one case node and other in another. Exception
     * expected!!
     */
    @Test
    public void compNodeDataOnVariousChoiceCasePathTest() {
        boolean exceptionCatched = false;
        try {
            TestUtils.writeCompNodeWithSchemaContextToJson(
                    TestUtils.loadCompositeNode("/yang-to-json-conversion/choice/xml/data_various_path.xml"),
                    "/yang-to-json-conversion/choice/xml", modules, dataSchemaNode);
        } catch (UnsupportedDataTypeException e) {
            exceptionCatched = true;

        } catch (WebApplicationException | IOException e) {
            // shouldn't end here
            assertTrue(false);
        }

        assertTrue(exceptionCatched);

    }

    /**
     * Test when second level data are red first, then first and at the end
     * third level. Level represents pass through couple choice-case
     */
    @Ignore
    @Test
    public void compNodeDataWithRandomOrderAccordingLevel() {
        try {
            String jsonOutput = TestUtils.writeCompNodeWithSchemaContextToJson(
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
    @Ignore
    @Test
    public void compNodeDataNoFirstCase() {
        try {
            String jsonOutput = TestUtils.writeCompNodeWithSchemaContextToJson(
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
    @Ignore
    @Test
    public void compNodeDataAsList() {
        try {
            String jsonOutput = TestUtils.writeCompNodeWithSchemaContextToJson(
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
    @Ignore
    @Test
    public void compNodeDataAsContainer() {
        try {
            String jsonOutput = TestUtils.writeCompNodeWithSchemaContextToJson(
                    TestUtils.loadCompositeNode("/yang-to-json-conversion/choice/xml/data_container.xml"),
                    "/yang-to-json-conversion/choice/xml", modules, dataSchemaNode);
        } catch (WebApplicationException | IOException e) {
            // shouldn't end here
            assertTrue(false);
        }
    }

    /**
     * Test when element in case is container
     */
    @Ignore
    @Test
    public void compNodeDataAsLeafList() {
        try {
            String jsonOutput = TestUtils.writeCompNodeWithSchemaContextToJson(
                    TestUtils.loadCompositeNode("/yang-to-json-conversion/choice/xml/data_leaflist.xml"),
                    "/yang-to-json-conversion/choice/xml", modules, dataSchemaNode);
        } catch (WebApplicationException | IOException e) {
            // shouldn't end here
            assertTrue(false);
        }
    }

}
