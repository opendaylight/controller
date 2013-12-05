package org.opendaylight.controller.sal.restconf.impl.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.Set;
import org.opendaylight.yangtools.yang.model.api.*;

public abstract class YangAndXmlAndDataSchemaLoader {

    protected static Set<Module> modules;
    protected static DataSchemaNode dataSchemaNode;

    protected static void dataLoad(String yangPath) {
        dataLoad(yangPath, 1, null, null);
    }

    protected static void dataLoad(String yangPath, int modulesNumber, String moduleName, String dataSchemaName) {
        modules = TestUtils.resolveModules(yangPath);
        assertEquals(modulesNumber, modules.size());
        Module module = TestUtils.resolveModule(moduleName, modules);
        assertNotNull(module);
        dataSchemaNode = TestUtils.resolveDataSchemaNode(module, dataSchemaName);
        assertNotNull(dataSchemaNode);
    }

}
