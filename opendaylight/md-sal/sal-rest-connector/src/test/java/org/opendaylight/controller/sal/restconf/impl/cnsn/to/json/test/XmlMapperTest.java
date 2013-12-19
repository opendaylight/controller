package org.opendaylight.controller.sal.restconf.impl.cnsn.to.json.test;

import java.util.Set;

import javax.activation.UnsupportedDataTypeException;

import org.junit.BeforeClass;
import org.junit.Test;
import org.opendaylight.controller.sal.rest.impl.XmlMapper;
import org.opendaylight.controller.sal.restconf.impl.test.TestUtils;
import org.opendaylight.yangtools.yang.data.api.CompositeNode;
import org.opendaylight.yangtools.yang.model.api.DataNodeContainer;
import org.opendaylight.yangtools.yang.model.api.DataSchemaNode;
import org.opendaylight.yangtools.yang.model.api.Module;
import org.w3c.dom.Document;
import static org.junit.Assert.*;

public class XmlMapperTest {

    private static CompositeNode compositeNode;
    private static Set<Module> modules;

    @BeforeClass
    public static void init() {
        compositeNode = TestUtils.loadCompositeNode("/xml-to-cnsn/xml_mapper_test/xml/data-leaf-list.xml");
        modules = TestUtils.resolveModulesFrom("/xml-to-cnsn/xml_mapper_test");
    }

    @Test
    public void translateToXmlAndReturnRootElementTest() throws UnsupportedDataTypeException {
        XmlMapper xmlMapper = new XmlMapper();

        Module module = TestUtils.resolveModule(null, modules);
        DataSchemaNode dataSchemaNode = TestUtils.resolveDataSchemaNode("cont", module);
        TestUtils.normalizeCompositeNode(compositeNode, modules, "modulTest:cont");

        Document doc = xmlMapper.write(compositeNode, (DataNodeContainer) dataSchemaNode);
        assertNotNull(doc);
    }
}
