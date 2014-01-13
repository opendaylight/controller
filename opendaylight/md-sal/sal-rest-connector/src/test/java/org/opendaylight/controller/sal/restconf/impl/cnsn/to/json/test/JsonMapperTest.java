package org.opendaylight.controller.sal.restconf.impl.cnsn.to.json.test;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Set;
import javax.ws.rs.WebApplicationException;
import org.junit.BeforeClass;
import org.junit.Test;
import org.opendaylight.controller.sal.rest.impl.StructuredDataToJsonProvider;
import org.opendaylight.controller.sal.rest.impl.XmlToCompositeNodeProvider;
import org.opendaylight.controller.sal.restconf.impl.test.TestUtils;
import org.opendaylight.yangtools.yang.data.api.CompositeNode;
import org.opendaylight.yangtools.yang.model.api.DataSchemaNode;
import org.opendaylight.yangtools.yang.model.api.Module;

public class JsonMapperTest {

    private static CompositeNode compositeNode;
    private static Set<Module> modules;

    @BeforeClass
    public static void init() throws FileNotFoundException {
        modules = TestUtils.loadModulesFrom("/cnsn-to-json/json_mapper_test");
        compositeNode = TestUtils.readInputToCnSn("/cnsn-to-json/json_mapper_test/xml/data.xml",
                XmlToCompositeNodeProvider.INSTANCE);
    }

    @Test
    public void writeTest() throws IOException {
        String jsonOutput = "";
        Module module = TestUtils.resolveModule(null, modules);
        DataSchemaNode dataSchemaNode = TestUtils.resolveDataSchemaNode("user", module);
        TestUtils.normalizeCompositeNode(compositeNode, modules, "modul:user/name");
        try {
            jsonOutput = TestUtils.writeCompNodeWithSchemaContextToOutput(compositeNode, modules, dataSchemaNode,
                    StructuredDataToJsonProvider.INSTANCE);
        } catch (WebApplicationException | IOException e) {
            assertTrue(false);
        }
        assertNotNull(jsonOutput);
    }
}