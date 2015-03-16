/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.restconf.impl.cnsn.to.xml.test;

import org.junit.BeforeClass;
import org.junit.Test;
import org.opendaylight.controller.sal.restconf.impl.test.YangAndXmlAndDataSchemaLoader;

/**
 *
 * CnSn = Composite node and Simple node data structure Class contains test of serializing simple nodes data values
 * according data types from YANG schema to XML file
 *
 */
public class CnSnToXmlWithChoiceTest extends YangAndXmlAndDataSchemaLoader {
    @BeforeClass
    public static void initialization() {
        dataLoad("/cnsn-to-xml/choice", 1, "module-with-choice", "cont");
    }

    @Test
    public void cnSnToXmlWithYangChoice() {
//        String xmlOutput = "";
//        try {
//            xmlOutput = TestUtils.writeCompNodeWithSchemaContextToOutput(
//                    prepareCnStructForYangData("lf1", "String data1"), modules, dataSchemaNode,
//                    StructuredDataToXmlProvider.INSTANCE);
//        } catch (WebApplicationException | IOException e) {
//        }
//
//        assertTrue(xmlOutput.contains("<lf1>String data1</lf1>"));
//
//        try {
//            xmlOutput = TestUtils.writeCompNodeWithSchemaContextToOutput(
//                    prepareCnStructForYangData("lf2", "String data2"), modules, dataSchemaNode,
//                    StructuredDataToXmlProvider.INSTANCE);
//        } catch (WebApplicationException | IOException e) {
//        }
//        assertTrue(xmlOutput.contains("<lf2>String data2</lf2>"));

    }

//    private CompositeNode prepareCnStructForYangData(final String lfName, final Object data) {
//        MutableCompositeNode cont = NodeFactory.createMutableCompositeNode(TestUtils.buildQName("cont"), null, null,
//                ModifyAction.CREATE, null);
//
//        MutableSimpleNode<Object> lf1 = NodeFactory.createMutableSimpleNode(TestUtils.buildQName(lfName), cont, data,
//                ModifyAction.CREATE, null);
//        cont.getValue().add(lf1);
//        cont.init();
//
//        return cont;
//    }

}
