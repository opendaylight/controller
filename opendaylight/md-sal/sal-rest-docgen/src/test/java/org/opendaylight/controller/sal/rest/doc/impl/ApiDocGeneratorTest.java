package org.opendaylight.controller.sal.rest.doc.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.util.Arrays;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;

import javax.ws.rs.core.UriInfo;

import junit.framework.Assert;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.controller.sal.core.api.model.SchemaService;
import org.opendaylight.controller.sal.rest.doc.swagger.Api;
import org.opendaylight.controller.sal.rest.doc.swagger.ApiDeclaration;
import org.opendaylight.controller.sal.rest.doc.swagger.Operation;
import org.opendaylight.controller.sal.rest.doc.swagger.Resource;
import org.opendaylight.controller.sal.rest.doc.swagger.ResourceList;
import org.opendaylight.yangtools.yang.model.api.Module;

import com.google.common.base.Preconditions;

/**
 *
 */
public class ApiDocGeneratorTest {

    public static final String HTTP_HOST = "http://host";
    private ApiDocGenerator generator;
    private DocGenTestHelper helper;

    @Before
    public void setUp() throws Exception {
        generator = new ApiDocGenerator();
        helper = new DocGenTestHelper();
        helper.setUp();
    }

    @After
    public void after() throws Exception {
    }

    /**
     * Method: getApiDeclaration(String module, String revision, UriInfo
     * uriInfo)
     */
    @Test
    public void testGetModuleDoc() throws Exception {
        Preconditions.checkArgument(helper.getModules() != null, "No modules found");

        for (Entry<File, Module> m : helper.getModules().entrySet()) {
            if (m.getKey().getAbsolutePath().endsWith("toaster_short.yang")) {
                ApiDeclaration doc = generator.getSwaggerDocSpec(m.getValue(),
                        "http://localhost:8080/restconf", "");
                validateToaster(doc);
                Assert.assertNotNull(doc);
            }
        }
    }

    @Test
    public void testEdgeCases() throws Exception {
        Preconditions.checkArgument(helper.getModules() != null, "No modules found");

        for (Entry<File, Module> m : helper.getModules().entrySet()) {
            if (m.getKey().getAbsolutePath().endsWith("toaster.yang")) {
                ApiDeclaration doc = generator.getSwaggerDocSpec(m.getValue(),
                        "http://localhost:8080/restconf", "");
                Assert.assertNotNull(doc);

                //testing bugs.opendaylight.org bug 1290. UnionType model type.
                String jsonString = doc.getModels().toString();
                assertTrue(
                        jsonString.contains( "testUnion\":{\"type\":\"integer or string\",\"required\":false}" ) );
            }
        }
    }

    private void validateToaster(ApiDeclaration doc) throws Exception {
        Set<String> expectedUrls = new TreeSet<>(Arrays.asList(new String[] {
                "/config/toaster2:toaster/", "/operational/toaster2:toaster/",
                "/operations/toaster2:cancel-toast", "/operations/toaster2:make-toast",
                "/operations/toaster2:restock-toaster" }));

        Set<String> actualUrls = new TreeSet<>();

        Api configApi = null;
        for (Api api : doc.getApis()) {
            actualUrls.add(api.getPath());
            if (api.getPath().contains("/config/toaster2:toaster/")) {
                configApi = api;
            }
        }

        boolean containsAll = actualUrls.containsAll(expectedUrls);
        if (!containsAll) {
            expectedUrls.removeAll(actualUrls);
            fail("Missing expected urls: " + expectedUrls);
        }

        Set<String> expectedConfigMethods = new TreeSet<>(Arrays.asList(new String[] { "GET",
                "PUT", "DELETE" }));
        Set<String> actualConfigMethods = new TreeSet<>();
        for (Operation oper : configApi.getOperations()) {
            actualConfigMethods.add(oper.getMethod());
        }

        containsAll = actualConfigMethods.containsAll(expectedConfigMethods);
        if (!containsAll) {
            expectedConfigMethods.removeAll(actualConfigMethods);
            fail("Missing expected method on config API: " + expectedConfigMethods);
        }

        // TODO: we should really do some more validation of the
        // documentation...
        /**
         * Missing validation: Explicit validation of URLs, and their methods
         * Input / output models.
         */
    }

    @Test
    public void testGetResourceListing() throws Exception {
        UriInfo info = helper.createMockUriInfo(HTTP_HOST);
        SchemaService mockSchemaService = helper.createMockSchemaService();

        generator.setSchemaService(mockSchemaService);

        ResourceList resourceListing = generator.getResourceListing(info);

        Resource toaster = null;
        Resource toaster2 = null;
        for (Resource r : resourceListing.getApis()) {
            String path = r.getPath();
            if (path.contains("toaster2")) {
                toaster2 = r;
            } else if (path.contains("toaster")) {
                toaster = r;
            }
        }

        assertNotNull(toaster2);
        assertNotNull(toaster);

        assertEquals(HTTP_HOST + "/toaster(2009-11-20)", toaster.getPath());
        assertEquals(HTTP_HOST + "/toaster2(2009-11-20)", toaster2.getPath());
    }

}
