/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.restconf.iml.varioustests;

import org.junit.Ignore;
import org.junit.Test;
import org.opendaylight.controller.sal.rest.impl.XmlToCompositeNodeProvider;
import org.opendaylight.controller.sal.restconf.impl.test.TestUtils;
import org.opendaylight.yangtools.yang.data.api.CompositeNode;
import org.w3c.dom.Document;


public class VariousTest {

    @Ignore
    @Test
    public void test() {
        String[] split = "/something:dfsa/s:sda".split("/");
        System.out.println(split.length);
        for (String str : split) {
            System.out.println(">"+str+"<");    
        }        
        
    }
    
    @Test
    public void loadXml() {
        TestUtils.readInputToCnSn("/varioustest/xmldata.xml", XmlToCompositeNodeProvider.INSTANCE);
//        TestUtils.normalizeCompositeNode(compositeNode, modules, schemaNodePath)
    }
    
    @Test
    public void buildXml() {
//        Document doc;
//        doc.createElementNS(namespaceURI, qualifiedName)
    }
    

}
