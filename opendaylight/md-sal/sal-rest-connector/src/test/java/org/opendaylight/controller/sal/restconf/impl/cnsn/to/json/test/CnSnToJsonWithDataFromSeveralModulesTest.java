/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.restconf.impl.cnsn.to.json.test;

import java.io.IOException;
import java.net.URISyntaxException;
import javax.ws.rs.WebApplicationException;
import org.junit.BeforeClass;
import org.junit.Test;
import org.opendaylight.controller.sal.restconf.impl.test.YangAndXmlAndDataSchemaLoader;

public class CnSnToJsonWithDataFromSeveralModulesTest extends YangAndXmlAndDataSchemaLoader {

    @BeforeClass
    public static void initialize() {
        dataLoad("/xml-to-cnsn/data-of-several-modules/yang", 2, "module1", "cont_m1");
    }

    @Test
    public void dataFromSeveralModulesToJsonTest() throws WebApplicationException, IOException, URISyntaxException {
//        SchemaContext schemaContext = TestUtils.loadSchemaContext(modules);
//        String output = TestUtils.writeCompNodeWithSchemaContextToOutput(prepareCnSn(), modules, schemaContext,
//                StructuredDataToJsonProvider.INSTANCE);
//
//        // String output =
//        // String.format("\"data\"   :   {\n" +
//        // "\t\"cont_m1\"   :  {\n" +
//        // "\t\t\"lf1_m1\"   :  \"lf1 m1 value\"\n" +
//        // "\t}\n" +
//        // "\t\"cont_m2\"   :  {\n" +
//        // "\t\t\"lf1_m2\"   :  \"lf1 m2 value\"\n" +
//        // "\t}\n" +
//        // "}");
//
//        StringBuilder regex = new StringBuilder();
//        regex.append("^");
//
//        regex.append(".*\"data\"");
//        regex.append(".*:");
//        regex.append(".*\\{");
//
//        regex.append(".*\"cont_m1\"");
//        regex.append(".*:");
//        regex.append(".*\\{");
//        regex.append(".*\\}");
//
//        regex.append(".*\"contB_m1\"");
//        regex.append(".*:");
//        regex.append(".*\\{");
//        regex.append(".*\\}");
//
//        regex.append(".*\"cont_m2\"");
//        regex.append(".*:");
//        regex.append(".*\\{");
//        regex.append(".*\\}");
//
//        regex.append(".*\"contB_m2\"");
//        regex.append(".*:");
//        regex.append(".*\\{");
//        regex.append(".*\\}");
//
//        regex.append(".*\\}");
//
//        regex.append(".*");
//        regex.append("$");
//
//        Pattern ptrn = Pattern.compile(regex.toString(), Pattern.DOTALL);
//        Matcher matcher = ptrn.matcher(output);
//
//        assertTrue(matcher.find());

    }

//    private CompositeNode prepareCnSn() throws URISyntaxException {
//        String uri1 = "module:one";
//        String rev1 = "2014-01-17";
//
//        MutableCompositeNode data = NodeFactory.createMutableCompositeNode(
//                TestUtils.buildQName("data", "urn:ietf:params:xml:ns:netconf:base:1.0", "2000-01-01"), null, null,
//                null, null);
//
//        MutableCompositeNode cont_m1 = NodeFactory.createMutableCompositeNode(
//                TestUtils.buildQName("cont_m1", uri1, rev1), data, null, null, null);
//        data.getValue().add(cont_m1);
//
//        MutableSimpleNode<?> lf1_m1 = NodeFactory.createMutableSimpleNode(TestUtils.buildQName("lf1_m1", uri1, rev1),
//                cont_m1, "lf1 m1 value", null, null);
//        cont_m1.getValue().add(lf1_m1);
//        cont_m1.init();
//
//        MutableCompositeNode contB_m1 = NodeFactory.createMutableCompositeNode(
//                TestUtils.buildQName("contB_m1", uri1, rev1), data, null, null, null);
//        data.getValue().add(contB_m1);
//        contB_m1.init();
//
//        String uri2 = "module:two";
//        String rev2 = "2014-01-17";
//        MutableCompositeNode cont_m2 = NodeFactory.createMutableCompositeNode(
//                TestUtils.buildQName("cont_m2", uri2, rev2), data, null, null, null);
//        data.getValue().add(cont_m2);
//
//        MutableSimpleNode<?> lf1_m2 = NodeFactory.createMutableSimpleNode(TestUtils.buildQName("lf1_m2", uri2, rev2),
//                cont_m1, "lf1 m2 value", null, null);
//        cont_m2.getValue().add(lf1_m2);
//        cont_m2.init();
//
//        MutableCompositeNode contB_m2 = NodeFactory.createMutableCompositeNode(
//                TestUtils.buildQName("contB_m2", uri2, rev2), data, null, null, null);
//        data.getValue().add(contB_m2);
//        contB_m2.init();
//
//        data.init();
//        return data;
//    }

}
