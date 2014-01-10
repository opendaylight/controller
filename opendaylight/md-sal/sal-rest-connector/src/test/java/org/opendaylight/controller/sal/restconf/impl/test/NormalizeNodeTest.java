package org.opendaylight.controller.sal.restconf.impl.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.net.URI;
import java.net.URISyntaxException;

import org.junit.BeforeClass;
import org.junit.Test;
import org.opendaylight.controller.sal.restconf.impl.CompositeNodeWrapper;
import org.opendaylight.controller.sal.restconf.impl.ResponseException;
import org.opendaylight.controller.sal.restconf.impl.SimpleNodeWrapper;
import org.opendaylight.yangtools.yang.data.api.CompositeNode;

public class NormalizeNodeTest extends YangAndXmlAndDataSchemaLoader {

    @BeforeClass
    public static void initialization() {
        dataLoad("/normalize-node/yang/");
    }

    @Test
    public void namespaceNotNullAndInvalidNamespaceAndNoModuleNameTest() {
        String exceptionMessage = null;
        try {
            TestUtils.normalizeCompositeNode(prepareCnSn("wrongnamespace"), modules, schemaNodePath);
        } catch (ResponseException e) {
            exceptionMessage = String.valueOf(e.getResponse().getEntity());
        }
        assertEquals(
                exceptionMessage,
                "Data has bad format.\nIf data is in XML format then namespace for cont should be normalize:node:module.\nIf data is in Json format then module name for cont should be normalize-node-module.");
    }

    @Test
    public void namespaceNullTest() {
        String exceptionMessage = null;
        try {
            TestUtils.normalizeCompositeNode(prepareCnSn(null), modules, schemaNodePath);
        } catch (ResponseException e) {
            exceptionMessage = String.valueOf(e.getResponse().getEntity());
        }
        assertNull(exceptionMessage);
    }

    @Test
    public void namespaceValidNamespaceTest() {
        String exceptionMessage = null;
        try {
            TestUtils.normalizeCompositeNode(prepareCnSn("normalize:node:module"), modules, schemaNodePath);
        } catch (ResponseException e) {
            exceptionMessage = String.valueOf(e.getResponse().getEntity());
        }
        assertNull(exceptionMessage);
    }

    @Test
    public void namespaceValidModuleNameTest() {
        String exceptionMessage = null;
        try {
            TestUtils.normalizeCompositeNode(prepareCnSn("normalize-node-module"), modules, schemaNodePath);
        } catch (ResponseException e) {
            exceptionMessage = String.valueOf(e.getResponse().getEntity());
        }
        assertNull(exceptionMessage);
    }

    private CompositeNode prepareCnSn(String namespace) {
        URI uri = null;
        if (namespace != null) {
            try {
                uri = new URI(namespace);
            } catch (URISyntaxException e) {
            }
            assertNotNull(uri);
        }

        SimpleNodeWrapper lf1 = new SimpleNodeWrapper(uri, "lf1", 43);
        CompositeNodeWrapper cont = new CompositeNodeWrapper(uri, "cont");
        cont.addValue(lf1);

        return cont;
    }

}
