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
