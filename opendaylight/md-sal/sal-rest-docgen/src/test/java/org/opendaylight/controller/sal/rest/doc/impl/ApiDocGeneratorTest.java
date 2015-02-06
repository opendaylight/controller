package org.opendaylight.controller.sal.rest.doc.impl;

import com.google.common.base.Preconditions;
import java.io.File;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;
import javax.ws.rs.core.UriInfo;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.controller.sal.core.api.model.SchemaService;
import org.opendaylight.controller.sal.rest.doc.swagger.Api;
import org.opendaylight.controller.sal.rest.doc.swagger.ApiDeclaration;
import org.opendaylight.controller.sal.rest.doc.swagger.Operation;
import org.opendaylight.controller.sal.rest.doc.swagger.Parameter;
import org.opendaylight.controller.sal.rest.doc.swagger.Resource;
import org.opendaylight.controller.sal.rest.doc.swagger.ResourceList;
import org.opendaylight.yangtools.yang.model.api.Module;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.opendaylight.yangtools.yang.parser.impl.YangParserImpl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.fail;

/**
 *
 */
public class ApiDocGeneratorTest {

    public static final String HTTP_HOST = "http://host";
    private ApiDocGenerator generator;
    private DocGenTestHelper helper;
    private SchemaContext schemaContext;

    @Before
    public void setUp() throws Exception {
        generator = new ApiDocGenerator();
        helper = new DocGenTestHelper();
        helper.setUp();
        schemaContext = new YangParserImpl().resolveSchemaContext(new HashSet<Module>(helper.getModules().values()));
    }

    @After
    public void after() throws Exception {
    }

    /**
     * Method: getApiDeclaration(String module, String revision, UriInfo uriInfo)
     */
    @Test
    public void testGetModuleDoc() throws Exception {
        Preconditions.checkArgument(helper.getModules() != null, "No modules found");

        for (Entry<File, Module> m : helper.getModules().entrySet()) {
            if (m.getKey().getAbsolutePath().endsWith("toaster_short.yang")) {
                ApiDeclaration doc = generator.getSwaggerDocSpec(m.getValue(), "http://localhost:8080/restconf", "",
                        schemaContext);
                validateToaster(doc);
                validateTosterDocContainsModulePrefixes(doc);
                validateSwaggerModules(doc);
                validateSwaggerApisForPost(doc);
            }
        }
    }

  @Test
  public void testHiddenChildren() throws Exception {
    Preconditions.checkArgument(helper.getModules() != null, "No modules found");

    for (Entry<File, Module> m : helper.getModules().entrySet()) {
      if (m.getKey().getAbsolutePath().endsWith("hidden.yang")) {
        ApiDeclaration doc = generator.getSwaggerDocSpec(m.getValue(), "http://localhost:8080/restconf", "",
                schemaContext);

        List<Api> apis = doc.getApis();
        for(Api api: apis){
          assertFalse("hidden-list should have been hidden", (api.getPath().contains("hidden-list")));
          assertFalse("hidden-augmented should have been hidden", (api.getPath().contains("hidden-augmented")));
          assertFalse("augmented_container should have been hidden", (api.getPath().contains("augmented_container")));
        }
      }
    }
  }


  /**
     * Validate whether ApiDelcaration contains Apis with concrete path and whether this Apis contain specified POST
     * operations.
     */
    private void validateSwaggerApisForPost(final ApiDeclaration doc) {
        // two POST URI with concrete schema name in summary
        Api lstApi = findApi("/config/toaster2:lst/", doc);
        assertNotNull("Api /config/toaster2:lst/ wasn't found", lstApi);
        assertTrue("POST for cont1 in lst is missing",
                findOperation(lstApi.getOperations(), "POST", "(config)lstPOST", "(config)lst1", "(config)cont1"));

        Api cont1Api = findApi("/config/toaster2:lst/cont1/", doc);
        assertNotNull("Api /config/toaster2:lst/cont1/ wasn't found", cont1Api);
        assertTrue("POST for cont11 in cont1 is missing",
                findOperation(cont1Api.getOperations(), "POST", "(config)cont1POST", "(config)cont11", "(config)lst11"));

        // no POST URI
        Api cont11Api = findApi("/config/toaster2:lst/cont1/cont11/", doc);
        assertNotNull("Api /config/toaster2:lst/cont1/cont11/ wasn't found", cont11Api);
        assertTrue("POST operation shouldn't be present.", findOperations(cont11Api.getOperations(), "POST").isEmpty());

    }

    /**
     * Tries to find operation with name {@code operationName} and with summary {@code summary}
     */
    private boolean findOperation(List<Operation> operations, String operationName, String type,
            String... searchedParameters) {
        Set<Operation> filteredOperations = findOperations(operations, operationName);
        for (Operation operation : filteredOperations) {
            if (operation.getType().equals(type)) {
                List<Parameter> parameters = operation.getParameters();
                return containAllParameters(parameters, searchedParameters);
            }
        }
        return false;
    }

    private Set<Operation> findOperations(final List<Operation> operations, final String operationName) {
        final Set<Operation> filteredOperations = new HashSet<>();
        for (Operation operation : operations) {
            if (operation.getMethod().equals(operationName)) {
                filteredOperations.add(operation);
            }
        }
        return filteredOperations;
    }

    private boolean containAllParameters(final List<Parameter> searchedIns, String[] searchedWhats) {
        for (String searchedWhat : searchedWhats) {
            boolean parameterFound = false;
            for (Parameter searchedIn : searchedIns) {
                if (searchedIn.getType().equals(searchedWhat)) {
                    parameterFound = true;
                }
            }
            if (!parameterFound) {
                return false;
            }
        }
        return true;
    }

    /**
     * Tries to find {@code Api} with path {@code path}
     */
    private Api findApi(final String path, final ApiDeclaration doc) {
        for (Api api : doc.getApis()) {
            if (api.getPath().equals(path)) {
                return api;
            }
        }
        return null;
    }

    /**
     * Validates whether doc {@code doc} contains concrete specified models.
     */
    private void validateSwaggerModules(ApiDeclaration doc) {
        JSONObject models = doc.getModels();
        assertNotNull(models);
        try {
            JSONObject configLst = models.getJSONObject("(config)lst");
            assertNotNull(configLst);

            containsReferences(configLst, "lst1");
            containsReferences(configLst, "cont1");

            JSONObject configLst1 = models.getJSONObject("(config)lst1");
            assertNotNull(configLst1);

            JSONObject configCont1 = models.getJSONObject("(config)cont1");
            assertNotNull(configCont1);

            containsReferences(configCont1, "cont11");
            containsReferences(configCont1, "lst11");

            JSONObject configCont11 = models.getJSONObject("(config)cont11");
            assertNotNull(configCont11);

            JSONObject configLst11 = models.getJSONObject("(config)lst11");
            assertNotNull(configLst11);
        } catch (JSONException e) {
            fail("JSONException wasn't expected");
        }

    }

    /**
     * Checks whether object {@code mainObject} contains in properties/items key $ref with concrete value.
     */
    private void containsReferences(final JSONObject mainObject, final String childObject) throws JSONException {
        JSONObject properties = mainObject.getJSONObject("properties");
        assertNotNull(properties);

        JSONObject nodeInProperties = properties.getJSONObject(childObject);
        assertNotNull(nodeInProperties);

        JSONObject itemsInNodeInProperties = nodeInProperties.getJSONObject("items");
        assertNotNull(itemsInNodeInProperties);

        String itemRef = itemsInNodeInProperties.getString("$ref");
        assertEquals("(config)" + childObject, itemRef);
    }

    @Test
    public void testEdgeCases() throws Exception {
        Preconditions.checkArgument(helper.getModules() != null, "No modules found");

        for (Entry<File, Module> m : helper.getModules().entrySet()) {
            if (m.getKey().getAbsolutePath().endsWith("toaster.yang")) {
                ApiDeclaration doc = generator.getSwaggerDocSpec(m.getValue(), "http://localhost:8080/restconf", "",
                        schemaContext);
                assertNotNull(doc);

                // testing bugs.opendaylight.org bug 1290. UnionType model type.
                String jsonString = doc.getModels().toString();
                assertTrue(jsonString.contains("testUnion\":{\"type\":\"integer or string\",\"required\":false}"));
            }
        }
    }

    /**
     * Tests whether from yang files are generated all required paths for HTTP operations (GET, DELETE, PUT, POST)
     *
     * If container | list is augmented then in path there should be specified module name followed with collon (e. g.
     * "/config/module1:element1/element2/module2:element3")
     *
     * @param doc
     * @throws Exception
     */
    private void validateToaster(ApiDeclaration doc) throws Exception {
        Set<String> expectedUrls = new TreeSet<>(Arrays.asList(new String[] { "/config/toaster2:toaster/",
                "/operational/toaster2:toaster/", "/operations/toaster2:cancel-toast",
                "/operations/toaster2:make-toast", "/operations/toaster2:restock-toaster",
                "/config/toaster2:toaster/toasterSlot/{slotId}/toaster-augmented:slotInfo/" }));

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

        Set<String> expectedConfigMethods = new TreeSet<>(Arrays.asList(new String[] { "GET", "PUT", "DELETE" }));
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
         * Missing validation: Explicit validation of URLs, and their methods Input / output models.
         */
    }

    @Test
    public void testGetResourceListing() throws Exception {
        UriInfo info = helper.createMockUriInfo(HTTP_HOST);
        SchemaService mockSchemaService = helper.createMockSchemaService(schemaContext);

        generator.setSchemaService(mockSchemaService);

        ResourceList resourceListing = generator.getResourceListing(info);

        Resource toaster = null;
        Resource toaster2 = null;
        Resource hidden_completely = null;
        Resource children_hidden = null;

      for (Resource r : resourceListing.getApis()) {
            String path = r.getPath();
            if (path.contains("toaster2")) {
                toaster2 = r;
            } else if (path.contains("toaster")) {
                toaster = r;
            } else if (path.contains("hidden_completely")) {
              hidden_completely = r;
            } else if (path.contains("hidden")) {
              children_hidden = r;
            }
        }

        assertNotNull(toaster2);
        assertNotNull(toaster);
        assertNotNull(children_hidden);
        assertNull(hidden_completely);

        assertEquals(HTTP_HOST + "/toaster(2009-11-20)", toaster.getPath());
        assertEquals(HTTP_HOST + "/toaster2(2009-11-20)", toaster2.getPath());
    }

    private void validateTosterDocContainsModulePrefixes(ApiDeclaration doc) {
        JSONObject topLevelJson = doc.getModels();
        try {
            JSONObject configToaster = topLevelJson.getJSONObject("(config)toaster");
            assertNotNull("(config)toaster JSON object missing", configToaster);
            // without module prefix
            containsProperties(configToaster, "toasterSlot");

            JSONObject toasterSlot = topLevelJson.getJSONObject("(config)toasterSlot");
            assertNotNull("(config)toasterSlot JSON object missing", toasterSlot);
            // with module prefix
            containsProperties(toasterSlot, "toaster-augmented:slotInfo");

        } catch (JSONException e) {
            fail("Json exception while reading JSON object. Original message " + e.getMessage());
        }
    }

    private void containsProperties(final JSONObject jsonObject, final String... properties) throws JSONException {
        for (String property : properties) {
            JSONObject propertiesObject = jsonObject.getJSONObject("properties");
            assertNotNull("Properties object missing in ", propertiesObject);
            JSONObject concretePropertyObject = propertiesObject.getJSONObject(property);
            assertNotNull(property + " is missing", concretePropertyObject);
        }
    }
}
