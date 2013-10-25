package org.opendaylight.controller.sal.restconf.impl.test;

import static org.junit.Assert.*;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.Set;

import org.junit.BeforeClass;
import org.junit.Test;
import org.opendaylight.controller.sal.restconf.impl.ControllerContext;
import org.opendaylight.controller.sal.restconf.impl.JsonMapper;
import org.opendaylight.yangtools.yang.data.api.CompositeNode;
import org.opendaylight.yangtools.yang.model.api.DataSchemaNode;
import org.opendaylight.yangtools.yang.model.api.Module;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;

public class JsonMapperTest {

    private static final ControllerContext controllerContext = new ControllerContext();

    @BeforeClass
    public static void init() throws FileNotFoundException {
        Set<Module> allModules = TestUtils.loadModules(JsonMapperTest.class.getResource("/full-versions/yangs").getPath());
        assertEquals(4, allModules.size());
        SchemaContext schemaContext = TestUtils.loadSchemaContext(allModules);
        controllerContext.setSchemas(schemaContext);
    }

    @Test
    public void test() throws FileNotFoundException {
        InputStream xmlStream = JsonMapperTest.class.getResourceAsStream("/parts/ietf-interfaces_interfaces.xml");
        CompositeNode loadedCompositeNode = TestUtils.loadCompositeNode(xmlStream);
        DataSchemaNode loadedSchemaNode = controllerContext.toInstanceIdentifier("ietf-interfaces:interfaces/interface/eth0").getSchemaNode();
        JsonMapper jsonMapper = new JsonMapper();
        String json = jsonMapper.convert(loadedSchemaNode, loadedCompositeNode);
    }

}
