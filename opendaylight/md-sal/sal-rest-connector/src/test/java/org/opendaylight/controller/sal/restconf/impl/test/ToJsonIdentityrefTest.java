package org.opendaylight.controller.sal.restconf.impl.test;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.regex.*;

import javax.ws.rs.WebApplicationException;

import org.junit.*;
import org.opendaylight.yangtools.yang.data.api.*;
import org.opendaylight.yangtools.yang.data.impl.NodeFactory;

public class ToJsonIdentityrefTest extends YangAndXmlAndDataSchemaLoader {

    @BeforeClass
    public static void initialization() {
        dataLoad("/yang-to-json-conversion/identityref", 2, "identityref-module", "cont");
    }

    @Test
    public void identityrefToJsonTest() {
        String json = null;
        try {
            json = TestUtils
                    .writeCompNodeWithSchemaContextToJson(prepareCompositeNode(), null, modules, dataSchemaNode);
        } catch (WebApplicationException | IOException e) {
            // shouldn't end here
            assertTrue(false);
        }
        assertNotNull(json);
        Pattern ptrn = Pattern.compile(".*\"lf1\"\\p{Space}*:\\p{Space}*\"identityref-module:name_test\".*",
                Pattern.DOTALL);
        Matcher mtch = ptrn.matcher(json);

        assertTrue(mtch.matches());
    }

    private CompositeNode prepareCompositeNode() {
        MutableCompositeNode cont = NodeFactory.createMutableCompositeNode(TestUtils.buildQName("cont"), null, null,
                ModifyAction.CREATE, null);
        MutableSimpleNode<?> lf1 = NodeFactory.createMutableSimpleNode(TestUtils.buildQName("lf1"), cont,
                TestUtils.buildQName("name_test", "identityref:module", "2013-12-2"), ModifyAction.CREATE, null);
        cont.getChildren().add(lf1);
        cont.init();

        return cont;
    }

}
