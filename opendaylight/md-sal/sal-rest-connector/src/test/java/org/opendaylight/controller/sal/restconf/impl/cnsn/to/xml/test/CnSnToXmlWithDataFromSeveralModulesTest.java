/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.restconf.impl.cnsn.to.xml.test;

import java.io.IOException;
import java.net.URISyntaxException;
import javax.ws.rs.WebApplicationException;
import org.junit.BeforeClass;
import org.junit.Test;
import org.opendaylight.controller.sal.restconf.impl.test.YangAndXmlAndDataSchemaLoader;

public class CnSnToXmlWithDataFromSeveralModulesTest extends YangAndXmlAndDataSchemaLoader {

    @BeforeClass
    public static void initialize() {
        dataLoad("/xml-to-cnsn/data-of-several-modules/yang", 2, "module1", "cont_m1");
    }

    @Test
    public void dataFromSeveralModulesToXmlTest() throws WebApplicationException, IOException, URISyntaxException {
//        SchemaContext schemaContext = TestUtils.loadSchemaContext(modules);
//        String output = TestUtils.writeCompNodeWithSchemaContextToOutput(prepareCnSn(), modules, schemaContext,
//                StructuredDataToXmlProvider.INSTANCE);
//
//        // String output =
//        // String.format("<data>" +
//        // "\n<cont_m1>" +
//        // "\n\t<lf1_m1>" +
//        // "\n\t\tlf1 m1 value" +
//        // "\n\t</lf1_m1>" +
//        // "\n</cont_m1>" +
//        // "\n<cont_m2>" +
//        // "\n\t<lf1_m2>" +
//        // "\n\t\tlf1 m2 value" +
//        // "\n\t</lf1_m2>" +
//        // "\n</cont_m2>" +
//        // "\n</data>");
//
//        StringBuilder regex = new StringBuilder();
//        regex.append("^");
//
//        regex.append(".*<data.*");
//        regex.append(".*xmlns=\"urn:ietf:params:xml:ns:netconf:base:1.0\"");
//        regex.append(".*>");
//
//        regex.append(".*<contB_m1.*\\/>");
//        regex.append(".*xmlns=\"module:one\"");
//        regex.append(".*>");
//        regex.append(".*<lf1_m1.*>");
//        regex.append(".*<\\/lf1_m1>");
//        regex.append(".*<\\/cont_m1>");
//
//        regex.append(".*<contB_m2.*/>");
//        regex.append(".*<cont_m2.*");
//        regex.append(".*xmlns=\"module:two\"");
//        regex.append(".*>");
//        regex.append(".*<lf1_m2.*>");
//        regex.append(".*<\\/lf1_m2>");
//        regex.append(".*<\\/cont_m2>");
//
//        regex.append(".*<\\/data.*>");
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
//        CompositeNodeWrapper data = new CompositeNodeWrapper(new URI("urn:ietf:params:xml:ns:netconf:base:1.0"), "data");
//
//        URI uriModule1 = new URI("module:one");
//        CompositeNodeWrapper cont_m1 = new CompositeNodeWrapper(uriModule1, "cont_m1");
//        SimpleNodeWrapper lf1_m1 = new SimpleNodeWrapper(uriModule1, "lf1_m1", "lf1 m1 value");
//        cont_m1.addValue(lf1_m1);
//        CompositeNodeWrapper contB_m1 = new CompositeNodeWrapper(uriModule1, "contB_m1");
//
//        data.addValue(contB_m1);
//        data.addValue(cont_m1);
//
//        URI uriModule2 = new URI("module:two");
//        CompositeNodeWrapper cont_m2 = new CompositeNodeWrapper(uriModule2, "cont_m2");
//        SimpleNodeWrapper lf1_m2 = new SimpleNodeWrapper(uriModule2, "lf1_m2", "lf1 m2 value");
//        cont_m2.addValue(lf1_m2);
//        CompositeNodeWrapper contB_m2 = new CompositeNodeWrapper(uriModule2, "contB_m2");
//        data.addValue(contB_m2);
//        data.addValue(cont_m2);
//        return data;
//    }

}
