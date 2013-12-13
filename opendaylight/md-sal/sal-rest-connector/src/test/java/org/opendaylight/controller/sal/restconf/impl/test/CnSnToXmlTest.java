package org.opendaylight.controller.sal.restconf.impl.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.StringWriter;
import java.util.Set;

import javax.activation.UnsupportedDataTypeException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.junit.BeforeClass;
import org.junit.Test;
import org.opendaylight.controller.sal.rest.impl.XmlMapper;
import org.opendaylight.yangtools.yang.data.api.CompositeNode;
import org.opendaylight.yangtools.yang.data.api.ModifyAction;
import org.opendaylight.yangtools.yang.data.api.MutableCompositeNode;
import org.opendaylight.yangtools.yang.data.api.MutableSimpleNode;
import org.opendaylight.yangtools.yang.data.impl.NodeFactory;
import org.opendaylight.yangtools.yang.model.api.DataNodeContainer;
import org.opendaylight.yangtools.yang.model.api.DataSchemaNode;
import org.opendaylight.yangtools.yang.model.api.Module;
import org.w3c.dom.Document;

public class CnSnToXmlTest {

    private static Set<Module> modules;
    private static DataSchemaNode dataSchemaNode;

    @BeforeClass
    public static void initialization() {
        modules = TestUtils.resolveModules("/cnsn-to-xml/identityref");
        assertEquals(2, modules.size());
        Module module = TestUtils.resolveModule("identityref-module", modules);
        assertNotNull(module);
        dataSchemaNode = TestUtils.resolveDataSchemaNode(module, "cont");
        assertNotNull(dataSchemaNode);

    }

    @Test
    public void compositeNodeToXMLTest() {
        XmlMapper xmlMapper = new XmlMapper();
        String xmlString = null;
        if (dataSchemaNode instanceof DataNodeContainer) {
            try {
                Document doc = xmlMapper.write(prepareData(), (DataNodeContainer) dataSchemaNode);
                DOMSource domSource = new DOMSource(doc);
                StringWriter writer = new StringWriter();
                StreamResult result = new StreamResult(writer);
                TransformerFactory tf = TransformerFactory.newInstance();
                Transformer transformer = tf.newTransformer();
                transformer.transform(domSource, result);
                xmlString = writer.toString();

            } catch (UnsupportedDataTypeException | TransformerException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        assertNotNull(xmlString);
        assertTrue(xmlString.contains("<lf1 xmlns:x=\"identity:module\">x:iden</lf1>"));

    }

    private CompositeNode prepareData() {
        MutableCompositeNode cont = NodeFactory.createMutableCompositeNode(
                TestUtils.buildQName("cont", "identityref:module", "2013-12-2"), null, null, ModifyAction.CREATE, null);
        MutableCompositeNode cont1 = NodeFactory
                .createMutableCompositeNode(TestUtils.buildQName("cont1", "identityref:module", "2013-12-2"), cont,
                        null, ModifyAction.CREATE, null);
        cont.getChildren().add(cont1);

        MutableSimpleNode<Object> lf11 = NodeFactory.createMutableSimpleNode(
                TestUtils.buildQName("lf1", "identityref:module", "2013-12-2"), cont1,
                TestUtils.buildQName("iden", "identity:module", "2013-12-2"), ModifyAction.CREATE, null);
        cont1.getChildren().add(lf11);
        cont1.init();
        cont.init();

        return cont;
    }

}
