package org.opendaylight.controller.sal.restconf.impl.cnsn.to.xml.test;

import static org.junit.Assert.*;

import java.io.StringWriter;
import java.util.Set;

import javax.activation.UnsupportedDataTypeException;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.junit.BeforeClass;
import org.junit.Test;
import org.opendaylight.controller.sal.rest.impl.XmlMapper;
import org.opendaylight.controller.sal.restconf.impl.test.TestUtils;
import org.opendaylight.yangtools.yang.data.api.*;
import org.opendaylight.yangtools.yang.data.impl.NodeFactory;
import org.opendaylight.yangtools.yang.data.impl.codec.TypeDefinitionAwareCodec;
import org.opendaylight.yangtools.yang.model.api.*;
import org.w3c.dom.Document;

/**
 * 
 * CnSn = Composite node and Simple node data structure Class contains test of
 * serializing simple nodes data values according data types from YANG schema to
 * XML file
 * 
 */
public class CnSnToXmlTest {

    private static Set<Module> modules;
    private static DataSchemaNode dataSchemaNode;

    @BeforeClass
    public static void initialization() {
        modules = TestUtils.resolveModules("/cnsn-to-xml/yang");
        assertEquals(2, modules.size());
        Module module = TestUtils.resolveModule("basic-module", modules);
        assertNotNull(module);
        dataSchemaNode = TestUtils.resolveDataSchemaNode(module, "cont");
        assertNotNull(dataSchemaNode);

    }

    @Test
    public void snAsYangIdentityrefToXMLTest() {
        serializeToXml(prepareIdentityrefData(), "<lf11 xmlns:x=\"referenced:module\">x:iden</lf11>");
    }

    @Test
    public void snAsYangStringToXmlTest() {
        serializeToXml(
                prepareCnStructForYangData(TypeDefinitionAwareCodec.STRING_DEFAULT_CODEC.deserialize("lfStr value"),
                        "lfStr"), "<lfStr>lfStr value</lfStr>");
    }

    @Test
    public void snAsYangInt8ToXmlTest() {
        String elName = "lfInt8";
        serializeToXml(
                prepareCnStructForYangData(TypeDefinitionAwareCodec.INT8_DEFAULT_CODEC.deserialize("14"), elName), "<"
                        + elName + ">14</" + elName + ">");
    }

    @Test
    public void snAsYangInt16ToXmlTest() {
        String elName = "lfInt16";
        serializeToXml(
                prepareCnStructForYangData(TypeDefinitionAwareCodec.INT16_DEFAULT_CODEC.deserialize("3000"), elName),
                "<" + elName + ">3000</" + elName + ">");
    }

    @Test
    public void snAsYangInt32ToXmlTest() {
        String elName = "lfInt32";
        serializeToXml(
                prepareCnStructForYangData(TypeDefinitionAwareCodec.INT32_DEFAULT_CODEC.deserialize("201234"), elName),
                "<" + elName + ">201234</" + elName + ">");
    }

    @Test
    public void snAsYangInt64ToXmlTest() {
        String elName = "lfInt64";
        serializeToXml(
                prepareCnStructForYangData(TypeDefinitionAwareCodec.INT64_DEFAULT_CODEC.deserialize("5123456789"),
                        elName), "<" + elName + ">5123456789</" + elName + ">");
    }

    @Test
    public void snAsYangUint8ToXmlTest() {
        String elName = "lfUint8";
        serializeToXml(
                prepareCnStructForYangData(TypeDefinitionAwareCodec.UINT8_DEFAULT_CODEC.deserialize("200"), elName),
                "<" + elName + ">200</" + elName + ">");
    }

    @Test
    public void snAsYangUint16ToXmlTest() {
        String elName = "lfUint16";
        serializeToXml(
                prepareCnStructForYangData(TypeDefinitionAwareCodec.UINT16_DEFAULT_CODEC.deserialize("4000"), elName),
                "<" + elName + ">4000</" + elName + ">");
    }

    @Test
    public void snAsYangUint32ToXmlTest() {
        String elName = "lfUint32";
        serializeToXml(
                prepareCnStructForYangData(TypeDefinitionAwareCodec.UINT32_DEFAULT_CODEC.deserialize("4123456789"),
                        elName), "<" + elName + ">4123456789</" + elName + ">");
    }

    @Test
    public void snAsYangUint64ToXmlTest() {
        String elName = "lfUint64";
        serializeToXml(
                prepareCnStructForYangData(TypeDefinitionAwareCodec.UINT64_DEFAULT_CODEC.deserialize("5123456789"),
                        elName), "<" + elName + ">5123456789</" + elName + ">");
    }

    @Test
    public void snAsYangBinaryToXmlTest() {
        String elName = "lfBinary";
        serializeToXml(
                prepareCnStructForYangData(
                        TypeDefinitionAwareCodec.BINARY_DEFAULT_CODEC
                                .deserialize("ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz01234567"),
                        elName), "<" + elName + ">ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz01234567</"
                        + elName + ">");
    }

    @Test
    public void snAsYangBitsToXmlTest() {
        String elName = "lfBits";
        serializeToXml(
                prepareCnStructForYangData(TypeDefinitionAwareCodec.BITS_DEFAULT_CODEC.deserialize("one two"), elName),
                "<" + elName + ">one two</" + elName + ">", "<" + elName + ">two one</" + elName + ">");
    }

    @Test
    public void snAsYangEnumerationToXmlTest() {
        String elName = "lfEnumeration";
        serializeToXml(
                prepareCnStructForYangData(TypeDefinitionAwareCodec.ENUMERATION_DEFAULT_CODEC.deserialize("enum2"),
                        elName), "<" + elName + ">enum2</" + elName + ">");
    }

    @Test
    public void snAsYangEmptyToXmlTest() {
        String elName = "lfEmpty";
        serializeToXml(
                prepareCnStructForYangData(TypeDefinitionAwareCodec.EMPTY_DEFAULT_CODEC.deserialize(null), elName), "<"
                        + elName + "/>");
    }

    @Test
    public void snAsYangBooleanToXmlTest() {
        String elName = "lfBoolean";
        serializeToXml(
                prepareCnStructForYangData(TypeDefinitionAwareCodec.BOOLEAN_DEFAULT_CODEC.deserialize("str"), elName),
                "<" + elName + ">false</" + elName + ">");
        serializeToXml(
                prepareCnStructForYangData(TypeDefinitionAwareCodec.BOOLEAN_DEFAULT_CODEC.deserialize("true"), elName),
                "<" + elName + ">true</" + elName + ">");
    }

    @Test
    public void snAsYangUnionToXmlTest() {
        String elName = "lfUnion";
        String int8 = "15";
        serializeToXml(
                prepareCnStructForYangData(TypeDefinitionAwareCodec.UNION_DEFAULT_CODEC.deserialize(int8), elName), "<"
                        + elName + ">15</" + elName + ">");

        String bits = "first second";
        serializeToXml(
                prepareCnStructForYangData(TypeDefinitionAwareCodec.UNION_DEFAULT_CODEC.deserialize(bits), elName), "<"
                        + elName + ">first second</" + elName + ">");

        String bool = "str";
        serializeToXml(
                prepareCnStructForYangData(TypeDefinitionAwareCodec.UNION_DEFAULT_CODEC.deserialize(bool), elName), "<"
                        + elName + ">str</" + elName + ">");
    }

    private void serializeToXml(CompositeNode compositeNode, String... xmlRepresentation)
            throws TransformerFactoryConfigurationError {
        XmlMapper xmlMapper = new XmlMapper();
        String xmlString = null;
        if (dataSchemaNode instanceof DataNodeContainer) {
            try {
                Document doc = xmlMapper.write(compositeNode, (DataNodeContainer) dataSchemaNode);
                DOMSource domSource = new DOMSource(doc);
                StringWriter writer = new StringWriter();
                StreamResult result = new StreamResult(writer);
                TransformerFactory tf = TransformerFactory.newInstance();
                Transformer transformer = tf.newTransformer();
                transformer.transform(domSource, result);
                xmlString = writer.toString();
            } catch (UnsupportedDataTypeException | TransformerException e) {
            }
        }
        assertNotNull(xmlMapper);
        boolean containSearchedStr = false;
        String strRepresentation = "";
        for (String searchedStr : xmlRepresentation) {
            if (xmlString.contains(searchedStr)) {
                containSearchedStr = true;
                break;
            }
            strRepresentation = strRepresentation + "[" + searchedStr + "]";
        }
        assertTrue("At least one of specified strings " + strRepresentation + " wasn't found.", containSearchedStr);

    }

    private CompositeNode prepareIdentityrefData() {
        MutableCompositeNode cont = NodeFactory.createMutableCompositeNode(
                TestUtils.buildQName("cont", "basic:module", "2013-12-2"), null, null, ModifyAction.CREATE, null);
        MutableCompositeNode cont1 = NodeFactory.createMutableCompositeNode(
                TestUtils.buildQName("cont1", "basic:module", "2013-12-2"), cont, null, ModifyAction.CREATE, null);
        cont.getChildren().add(cont1);

        MutableSimpleNode<Object> lf11 = NodeFactory.createMutableSimpleNode(
                TestUtils.buildQName("lf11", "basic:module", "2013-12-2"), cont1,
                TestUtils.buildQName("iden", "referenced:module", "2013-12-2"), ModifyAction.CREATE, null);
        cont1.getChildren().add(lf11);
        cont1.init();
        cont.init();

        return cont;
    }

    private CompositeNode prepareCnStructForYangData(Object data, String leafName) {
        MutableCompositeNode cont = NodeFactory.createMutableCompositeNode(TestUtils.buildQName("cont"), null, null,
                ModifyAction.CREATE, null);

        MutableSimpleNode<Object> lf1 = NodeFactory.createMutableSimpleNode(TestUtils.buildQName(leafName), cont, data,
                ModifyAction.CREATE, null);
        cont.getChildren().add(lf1);
        cont.init();

        return cont;
    }

}
