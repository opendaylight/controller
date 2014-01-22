package org.opendaylight.controller.sal.restconf.impl.xml.to.cnsn.test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

import java.util.Set;

import org.junit.Ignore;
import org.junit.Test;
import org.opendaylight.controller.sal.rest.impl.XmlToCompositeNodeProvider;
import org.opendaylight.controller.sal.restconf.impl.ResponseException;
import org.opendaylight.controller.sal.restconf.impl.test.TestUtils;
import org.opendaylight.yangtools.yang.data.api.CompositeNode;
import org.opendaylight.yangtools.yang.model.api.Module;

public class XmlAugmentedElementToCnSnTest {

    @Ignore
    @Test
    public void loadDataAugmentedSchemaMoreEqualNamesTest() {
        boolean exceptionCaught = false;
        try {
            loadAndNormalizeData("/common/augment/xml/dataa.xml", "/common/augment/yang", "main","cont");
            loadAndNormalizeData("/common/augment/xml/datab.xml", "/common/augment/yang", "main","cont");
        } catch (ResponseException e) {
            exceptionCaught = true;
        }
        
        assertFalse(exceptionCaught);        
    }
    
    
    private void loadAndNormalizeData(String xmlPath, String yangPath, String topLevelElementName, String moduleName) {
        CompositeNode compNode = TestUtils.readInputToCnSn(xmlPath, false,
                XmlToCompositeNodeProvider.INSTANCE);
        assertNotNull(compNode);
        Set<Module> modules = TestUtils.loadModulesFrom(yangPath);

        assertNotNull(modules);
        TestUtils.normalizeCompositeNode(compNode, modules, topLevelElementName + ":" + moduleName);
    }


}
