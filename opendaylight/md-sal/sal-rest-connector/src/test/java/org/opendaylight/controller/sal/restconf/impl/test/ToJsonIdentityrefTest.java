package org.opendaylight.controller.sal.restconf.impl.test;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.ws.rs.WebApplicationException;

import org.junit.*;
import org.opendaylight.controller.sal.restconf.impl.test.TestUtils;
import org.opendaylight.controller.sal.restconf.impl.test.YangAndXmlAndDataSchemaLoader;
import org.opendaylight.yangtools.yang.data.api.*;
import org.opendaylight.yangtools.yang.data.impl.NodeFactory;

public class ToJsonIdentityrefTest extends YangAndXmlAndDataSchemaLoader {

    @BeforeClass
    public static void initialization() {
        dataLoad("/yang-to-json-conversion/identityref", 2, "identityref-module", "cont");
    }

    @Ignore
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
        MutableCompositeNode cont1 = NodeFactory.createMutableCompositeNode(TestUtils.buildQName("cont1"), cont, null,
                ModifyAction.CREATE, null);
        cont.getChildren().add(cont1);

        MutableSimpleNode<?> lf1 = NodeFactory.createMutableSimpleNode(TestUtils.buildQName("lf1"), cont1,
                TestUtils.buildQName("name_test", "identityref:module", "2013-12-2"), ModifyAction.CREATE, null);
        cont1.getChildren().add(lf1);
        cont1.init();
        cont.init();

        return cont;
    }

}
