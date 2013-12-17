package org.opendaylight.controller.sal.restconf.impl.cnsn.to.json.test;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.regex.Matcher;

import javax.ws.rs.WebApplicationException;

import org.junit.*;
import org.opendaylight.controller.sal.restconf.impl.test.TestUtils;
import org.opendaylight.controller.sal.restconf.impl.test.YangAndXmlAndDataSchemaLoader;

/**
 * 
 * All tests are commented now because leafref isn't supported now
 * 
 */

public class ToJsonLeafrefType extends YangAndXmlAndDataSchemaLoader {

    @BeforeClass
    public static void initialization() {
        dataLoad("/cnsn-to-json/leafref", 2, "main-module", "cont");
    }

    @Ignore
    @Test
    public void leafrefAbsolutePathToExistingLeafTest() {
        String json = null;
        try {
            json = TestUtils.writeCompNodeWithSchemaContextToJson(
                    TestUtils.loadCompositeNode("/cnsn-to-json/leafref/xml/data_absolut_ref_to_existing_leaf.xml"),
                    modules, dataSchemaNode);
        } catch (WebApplicationException | IOException e) {
            // shouldn't end here
            assertTrue(false);
        }
        assertNotNull(json);
        java.util.regex.Pattern ptrn = java.util.regex.Pattern.compile(".*\"lf3\":\\p{Blank}*true.*",
                java.util.regex.Pattern.DOTALL);
        Matcher mtch = ptrn.matcher(json);
        assertTrue(mtch.matches());
    }

    @Ignore
    @Test
    public void leafrefRelativePathToExistingLeafTest() {
        String json = null;
        try {
            json = TestUtils.writeCompNodeWithSchemaContextToJson(
                    TestUtils.loadCompositeNode("/cnsn-to-json/leafref/xml/data_relativ_ref_to_existing_leaf.xml"),
                    modules, dataSchemaNode);
        } catch (WebApplicationException | IOException e) {
            // shouldn't end here
            assertTrue(false);
        }
        assertNotNull(json);
        java.util.regex.Pattern ptrn = java.util.regex.Pattern.compile(".*\"lf2\":\\p{Blank}*121.*",
                java.util.regex.Pattern.DOTALL);
        Matcher mtch = ptrn.matcher(json);
        assertTrue(mtch.matches());
    }

    /**
     * Tests case when reference to not existing element is present. In this
     * case value from single node is printed as string.
     */
    @Ignore
    @Test
    public void leafrefToNonExistingLeafTest() {
        String json = null;
        try {
            json = TestUtils.writeCompNodeWithSchemaContextToJson(
                    TestUtils.loadCompositeNode("/cnsn-to-json/leafref/xml/data_ref_to_non_existing_leaf.xml"),
                    modules, dataSchemaNode);
        } catch (WebApplicationException | IOException e) {
            // shouldn't end here
            assertTrue(false);
        }
        assertNotNull(json);
        java.util.regex.Pattern ptrn = java.util.regex.Pattern.compile(".*\"lf5\":\\p{Blank}*\"137\".*",
                java.util.regex.Pattern.DOTALL);
        Matcher mtch = ptrn.matcher(json);
        assertTrue(mtch.matches());
    }

    /**
     * Tests case when non leaf element is referenced. In this case value from
     * single node is printed as string.
     */
    @Ignore
    @Test
    public void leafrefToNotLeafTest() {
        String json = null;
        try {
            json = TestUtils.writeCompNodeWithSchemaContextToJson(
                    TestUtils.loadCompositeNode("/cnsn-to-json/leafref/xml/data_ref_to_not_leaf.xml"), modules,
                    dataSchemaNode);
        } catch (WebApplicationException | IOException e) {
            // shouldn't end here
            assertTrue(false);
        }
        assertNotNull(json);
        java.util.regex.Pattern ptrn = java.util.regex.Pattern.compile(
                ".*\"cont-augment-module\\p{Blank}*:\\p{Blank}*lf6\":\\p{Blank}*\"44.33\".*",
                java.util.regex.Pattern.DOTALL);
        Matcher mtch = ptrn.matcher(json);
        assertTrue(mtch.matches());
    }

    /**
     * Tests case when leaflist element is refers to leaf.
     */
    @Ignore
    @Test
    public void leafrefFromLeafListToLeafTest() {
        String json = null;
        try {
            json = TestUtils
                    .writeCompNodeWithSchemaContextToJson(
                            TestUtils
                                    .loadCompositeNode("/cnsn-to-json/leafref/xml/data_relativ_ref_from_leaflist_to_existing_leaf.xml"),
                            modules, dataSchemaNode);
        } catch (WebApplicationException | IOException e) {
            // shouldn't end here
            assertTrue(false);
        }
        assertNotNull(json);
        java.util.regex.Pattern ptrn = java.util.regex.Pattern
                .compile(
                        ".*\"cont-augment-module\\p{Blank}*:\\p{Blank}*lflst1\":\\p{Blank}*.*345,\\p{Space}*346,\\p{Space}*347.*",
                        java.util.regex.Pattern.DOTALL);
        Matcher mtch = ptrn.matcher(json);
        assertTrue(mtch.matches());
    }

    /**
     * Tests case when leaflist element is refers to leaf.
     */
    @Ignore
    @Test
    public void leafrefFromLeafrefToLeafrefTest() {
        String json = null;
        try {
            json = TestUtils.writeCompNodeWithSchemaContextToJson(
                    TestUtils.loadCompositeNode("/cnsn-to-json/leafref/xml/data_from_leafref_to_leafref.xml"), modules,
                    dataSchemaNode);
        } catch (WebApplicationException | IOException e) {
            // shouldn't end here
            assertTrue(false);
        }
        assertNotNull(json);
        java.util.regex.Pattern ptrn = java.util.regex.Pattern.compile(
                ".*\"cont-augment-module\\p{Blank}*:\\p{Blank}*lf7\":\\p{Blank}*200.*", java.util.regex.Pattern.DOTALL);
        Matcher mtch = ptrn.matcher(json);
        assertTrue(mtch.matches());
    }

}
