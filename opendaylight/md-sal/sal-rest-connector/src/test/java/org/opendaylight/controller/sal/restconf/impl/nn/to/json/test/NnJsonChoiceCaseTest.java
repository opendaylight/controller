package org.opendaylight.controller.sal.restconf.impl.nn.to.json.test;

import static org.junit.Assert.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;

import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.opendaylight.controller.md.sal.rest.common.TestRestconfUtils;
import org.opendaylight.controller.sal.rest.impl.NormalizedNodeJsonBodyWriter;
import org.opendaylight.controller.sal.rest.impl.test.providers.AbstractBodyReaderTest;
import org.opendaylight.controller.sal.restconf.impl.NormalizedNodeContext;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;

@Ignore
public class NnJsonChoiceCaseTest extends AbstractBodyReaderTest {

    private static SchemaContext schemaContext;
    private NormalizedNodeJsonBodyWriter jsonBodyWriter;

    public NnJsonChoiceCaseTest() throws NoSuchFieldException,
            SecurityException {
        super();
        jsonBodyWriter = new NormalizedNodeJsonBodyWriter();
    }

    @BeforeClass
    public static void initialization() {
        // dataLoad("/cnsn-to-json/choice");
        schemaContext = schemaContextLoader("/nn-to-json/choice", schemaContext);
        controllerContext.setSchemas(schemaContext);
    }

    /**
     * Test when some data are in one case node and other in another. This isn't
     * correct. Next Json validator should return error because nodes has to be
     * from one case below concrete choice.
     */
    @Test(expected = NullPointerException.class)
    public void nodeSchemasOnVariousChoiceCasePathTest() {
        testWrapper("/nn-to-json/choice/xml/data_various_path_err.xml",
                "choice-case-test:cont");
    }

    /**
     * Test when some data are in one case node and other in another.
     * Additionally data are loadef from various choices. This isn't correct.
     * Next Json validator should return error because nodes has to be from one
     * case below concrete choice.
     */
    @Test(expected = NullPointerException.class)
    public void nodeSchemasOnVariousChoiceCasePathAndMultipleChoicesTest() {
        testWrapper(
                "/nn-to-json/choice/xml/data_more_choices_same_level_various_paths_err.xml",
                "choice-case-test:cont");
    }

    /**
     * Test when second level data are red first, then first and at the end
     * third level. Level represents pass through couple choice-case
     */

    @Test
    public void nodeSchemasWithRandomOrderAccordingLevel() {
        testWrapper("/nn-to-json/choice/xml/data_random_level.xml",
                "choice-case-test:cont");
    }

    /**
     * Test when element from no first case is used
     */
    @Test
    public void nodeSchemasNotInFirstCase() {
        testWrapper("/nn-to-json/choice/xml/data_no_first_case.xml",
                "choice-case-test:cont");
    }

    /**
     * Test when element in case is list
     */
    @Test
    public void nodeSchemaAsList() {
        testWrapper("/nn-to-json/choice/xml/data_list.xml",
                "choice-case-test:cont");
    }

    /**
     * Test when element in case is container
     */
    @Test
    public void nodeSchemaAsContainer() {
        testWrapper("/nn-to-json/choice/xml/data_container.xml",
                "choice-case-test:cont");
    }

    /**
     * Test when element in case is leaflist
     */
    @Test
    public void nodeSchemaAsLeafList() {
        testWrapper("/nn-to-json/choice/xml/data_leaflist.xml",
                "choice-case-test:cont");
    }

    /**
     *
     */
    @Test
    public void nodeSchemasInMultipleChoicesTest() {
        testWrapper("/nn-to-json/choice/xml/data_more_choices_same_level.xml",
                "choice-case-test:cont");
    }

    /**
     * Test whether is possible to find data schema for node which is specified
     * as dirrect subnode of choice (case without CASE key word)
     */
    @Test
    public void nodeSchemasInCaseNotDefinedWithCaseKeyword() {
        testWrapper(
                "/nn-to-json/choice/xml/data_case_defined_without_case.xml",
                "choice-case-test:cont");
    }

    /**
     * Test of multiple use of choices
     */
    @Test
    public void nodeSchemasInThreeChoicesAtSameLevel() {
        testWrapper("/nn-to-json/choice/xml/data_three_choices_same_level.xml",
                "choice-case-test:cont");
    }

    private void testWrapper(String xmlPath, String uri) {

        final NormalizedNodeContext testNN = TestRestconfUtils
                .loadNormalizedContextFromXmlFile(xmlPath, uri);

        final OutputStream output = new ByteArrayOutputStream();
        try {
            jsonBodyWriter.writeTo(testNN, null, null, null, mediaType, null,
                    output);
        } catch (WebApplicationException | IOException e) {
            // Should not end here
            assertTrue(false);
        }
    }

    @Override
    protected MediaType getMediaType() {
        // TODO Auto-generated method stub
        return null;
    }
}
