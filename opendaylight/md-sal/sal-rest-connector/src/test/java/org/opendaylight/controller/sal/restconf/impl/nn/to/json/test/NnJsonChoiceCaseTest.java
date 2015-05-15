package org.opendaylight.controller.sal.restconf.impl.nn.to.json.test;

import static org.junit.Assert.assertTrue;
import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import javax.ws.rs.core.MediaType;
import org.junit.BeforeClass;
import org.junit.Test;
import org.opendaylight.controller.md.sal.rest.common.TestRestconfUtils;
import org.opendaylight.controller.sal.rest.impl.NormalizedNodeJsonBodyWriter;
import org.opendaylight.controller.sal.rest.impl.test.providers.AbstractBodyReaderTest;
import org.opendaylight.controller.sal.restconf.impl.NormalizedNodeContext;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;

public class NnJsonChoiceCaseTest extends AbstractBodyReaderTest {

    private static SchemaContext schemaContext;
    private final NormalizedNodeJsonBodyWriter jsonBodyWriter;

    public NnJsonChoiceCaseTest() throws NoSuchFieldException,
            SecurityException {
        super();
        jsonBodyWriter = new NormalizedNodeJsonBodyWriter();
    }

    @BeforeClass
    public static void initialization() {
        schemaContext = schemaContextLoader("/nn-to-json/choice", schemaContext);
        controllerContext.setSchemas(schemaContext);
    }

    /**
     * Test when some data are in one case node and other in another. This isn't
     * correct. Next Json validator should return error because nodes has to be
     * from one case below concrete choice.
     */
    @Test(expected = NullPointerException.class)
    public void nodeSchemasOnVariousChoiceCasePathTest() throws Exception {
        getJson("/nn-to-json/choice/xml/data_various_path_err.xml");
    }

    /**
     * Test when some data are in one case node and other in another.
     * Additionally data are loadef from various choices. This isn't correct.
     * Next Json validator should return error because nodes has to be from one
     * case below concrete choice.
     */
    @Test(expected = NullPointerException.class)
    public void nodeSchemasOnVariousChoiceCasePathAndMultipleChoicesTest()
            throws Exception {
        getJson("/nn-to-json/choice/xml/data_more_choices_same_level_various_paths_err.xml");
    }

    /**
     * Test when second level data are red first, then first and at the end
     * third level. Level represents pass through couple choice-case
     */

    @Test
    public void nodeSchemasWithRandomOrderAccordingLevel() throws Exception {
        final String json = getJson("/nn-to-json/choice/xml/data_random_level.xml");

        assertTrue(json.contains("cont"));
        assertTrue(json.contains("lf1" + '"' + ':' + '"' + "lf1 val" + '"'
                + ','));
        assertTrue(json.contains("lf1aaa" + '"' + ':' + '"' + "lf1aaa val"
                + '"' + ','));
        assertTrue(json.contains("lf1aa" + '"' + ':' + '"' + "lf1aa val" + '"'
                + ','));
        assertTrue(json.contains("lf1a" + '"' + ':' + "121}}"));

    }

    /**
     * Test when element from no first case is used
     */
    @Test
    public void nodeSchemasNotInFirstCase() throws Exception {
        final String json = getJson("/nn-to-json/choice/xml/data_no_first_case.xml");

        assertTrue(json.contains("cont"));
        assertTrue(json.contains("lf1" + '"' + ':' + '"' + "lf1 val" + '"'
                + ','));
        assertTrue(json.contains("lf1ab" + '"' + ':' + '"' + "lf1ab val" + '"'
                + ','));
        assertTrue(json.contains("lf1a" + '"' + ':' + "121}}"));
    }

    /**
     * Test when element in case is list
     */
    @Test
    public void nodeSchemaAsList() throws Exception {
        final String json = getJson("/nn-to-json/choice/xml/data_list.xml");

        assertTrue(json.contains("cont"));
        assertTrue(json.contains("lst1b" + '"' + ':' + "[{"));
        assertTrue(json.contains("lf11b" + '"' + ':' + '"' + "lf11b_1 val"
                + '"' + "},{" + '"' + "lf11b" + '"' + ':' + '"' + "lf11b_2 val"
                + '"' + "}]}}"));
    }

    /**
     * Test when element in case is container
     */
    @Test
    public void nodeSchemaAsContainer() throws Exception {
        final String json = getJson("/nn-to-json/choice/xml/data_container.xml");

        assertTrue(json.contains("cont"));
        assertTrue(json.contains("cont1c" + '"' + ':' + "{"));
        assertTrue(json.contains("lf11c" + '"' + ':' + '"' + "lf11c val" + '"'
                + "}}}"));
    }

    /**
     * Test when element in case is leaflist
     */
    @Test
    public void nodeSchemaAsLeafList() throws Exception {
        final String json = getJson("/nn-to-json/choice/xml/data_leaflist.xml");

        assertTrue(json.contains("cont"));
        assertTrue(json.contains("lflst1d" + '"' + ':' + "["));
        assertTrue(json.contains("lflst1d_1 val" + '"' + ',' + '"'
                + "lflst1d_2 val" + '"' + "]}}"));
    }

    /**
     *
     */
    @Test
    public void nodeSchemasInMultipleChoicesTest() throws Exception {
        final String json = getJson("/nn-to-json/choice/xml/data_more_choices_same_level.xml");

        assertTrue(json.contains("cont"));
        assertTrue(json.contains("lf2b" + '"' + ':' + '"' + "lf2b value" + '"'
                + ','));
        assertTrue(json.contains("cont1c" + '"' + ':' + "{"));
        assertTrue(json.contains("lf11c" + '"' + ':' + '"' + "lf11c val"
                + '"' + "}}"));
    }

    /**
     * Test whether is possible to find data schema for node which is specified
     * as dirrect subnode of choice (case without CASE key word)
     */
    @Test
    public void nodeSchemasInCaseNotDefinedWithCaseKeyword() throws Exception {
        final String json = getJson("/nn-to-json/choice/xml/data_case_defined_without_case.xml");

        assertTrue(json.contains("cont"));
        assertTrue(json.contains("lf2b" + '"' + ':' + '"' + "lf2b val" + '"'
                + ','));
        assertTrue(json.contains("e1" + '"' + ':' + "45}}"));
    }

    /**
     * Test of multiple use of choices
     */
    @Test
    public void nodeSchemasInThreeChoicesAtSameLevel() throws Exception {
        final String json = getJson("/nn-to-json/choice/xml/data_three_choices_same_level.xml");

        assertTrue(json.contains("cont"));
        assertTrue(json.contains("lf2b" + '"' + ':' + '"' + "lf2b value"));
        assertTrue(json.contains("lst4a" + '"' + ':' + "[{"));
        assertTrue(json.contains("lf4ab" + '"' + ':' + "33},{" + '"' + "lf4ab"
                + '"' + ':' + "33},{"));
        assertTrue(json.contains("lf4ab" + '"' + ':' + "37}],"));
        assertTrue(json.contains("lf1aaa" + '"' + ':' + '"' + "lf1aaa value"
                + '"' + "}}"));
    }

    private String getJson(final String xmlPath) throws Exception {
        final String uri = "choice-case-test:cont";
        final NormalizedNodeContext testNN = TestRestconfUtils
                .loadNormalizedContextFromXmlFile(xmlPath, uri);

        final OutputStream output = new ByteArrayOutputStream();
        jsonBodyWriter.writeTo(testNN, null, null, null, mediaType, null,
                output);

        return output.toString();
    }

    @Override
    protected MediaType getMediaType() {
        return null;
    }
}
