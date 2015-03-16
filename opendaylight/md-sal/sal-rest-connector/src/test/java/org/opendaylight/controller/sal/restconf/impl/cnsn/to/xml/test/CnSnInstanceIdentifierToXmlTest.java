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

/**
 *
 * CnSn = Composite node and Simple node data structure Class contains test of serializing simple nodes data values
 * according data types from YANG schema to XML file
 *
 */
public class CnSnInstanceIdentifierToXmlTest extends YangAndXmlAndDataSchemaLoader {

    @BeforeClass
    public static void initialization() throws URISyntaxException {
        dataLoad("/instanceidentifier/yang", 4, "instance-identifier-module", "cont");
    }

    @Test
    public void snAsYangInstanceIdentifier() throws WebApplicationException, IOException, URISyntaxException {
//        CompositeNode cnSnData = prepareCnStructForYangData();
//        String xmlOutput = TestUtils.writeCompNodeWithSchemaContextToOutput(cnSnData, modules, dataSchemaNode,
//                StructuredDataToXmlProvider.INSTANCE);
//        assertNotNull(xmlOutput);
    }

//    private CompositeNode prepareCnStructForYangData() throws URISyntaxException {
//        CompositeNodeWrapper cont = new CompositeNodeWrapper(new URI("instance:identifier:module"), "cont");
//        CompositeNodeWrapper cont1 = new CompositeNodeWrapper(new URI("augment:module"), "cont1");
//        cont.addValue(cont1);
//        SimpleNodeWrapper lf11 = new SimpleNodeWrapper(new URI("augment:augment:module"), "lf11", "/cont/cont1/lf12");
//        SimpleNodeWrapper lf12 = new SimpleNodeWrapper(new URI("augment:augment:module"), "lf12", "lf12 value");
//        cont1.addValue(lf11);
//        cont1.addValue(lf12);
//        cont.unwrap();
//        return cont;
//    }

}
