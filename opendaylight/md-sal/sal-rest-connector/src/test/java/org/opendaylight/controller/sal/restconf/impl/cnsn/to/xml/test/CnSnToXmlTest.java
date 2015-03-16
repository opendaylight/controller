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
public class CnSnToXmlTest extends YangAndXmlAndDataSchemaLoader {
    @BeforeClass
    public static void initialization() {
        dataLoad("/cnsn-to-xml/yang", 2, "basic-module", "cont");
    }

    @Test
    public void snAsYangIdentityrefToXMLTest() {
//        serializeToXml(prepareIdentityrefData(null, true), "<lf11 xmlns:x=\"referenced:module\">x:iden</lf11>");
    }

    @Test
    public void snAsYangIdentityrefWithQNamePrefixToXMLTest() {
//        serializeToXml(prepareIdentityrefData("prefix", true),
//                "<lf11 xmlns","=\"referenced:module\">",":iden</lf11>");
    }

    @Test
    public void snAsYangIdentityrefWithPrefixToXMLTest() {
//        serializeToXml(prepareIdentityrefData("prefix", false), "<lf11>no qname value</lf11>");
    }

    @Test
    public void snAsYangLeafrefWithPrefixToXMLTest() {
//        serializeToXml(prepareLeafrefData(), "<lfBoolean>true</lfBoolean>", "<lfLfref>true</lfLfref>");
    }

    @Test
    public void snAsYangStringToXmlTest() {
//        serializeToXml(
//                prepareCnStructForYangData(
//                        TypeDefinitionAwareCodec.from(StringType.getInstance()).deserialize("lfStr value"), "lfStr"),
//                "<lfStr>lfStr value</lfStr>");
    }

    @Test
    public void snAsYangInt8ToXmlTest() {
//        final String elName = "lfInt8";
//        serializeToXml(
//                prepareCnStructForYangData(TypeDefinitionAwareCodec.from(Int8.getInstance()).deserialize("14"), elName),
//                "<" + elName + ">14</" + elName + ">");
    }

    @Test
    public void snAsYangInt16ToXmlTest() {
//        final String elName = "lfInt16";
//        serializeToXml(
//                prepareCnStructForYangData(TypeDefinitionAwareCodec.from(Int16.getInstance()).deserialize("3000"),
//                        elName), "<" + elName + ">3000</" + elName + ">");
    }

    @Test
    public void snAsYangInt32ToXmlTest() {
//        final String elName = "lfInt32";
//        serializeToXml(
//                prepareCnStructForYangData(TypeDefinitionAwareCodec.from(Int32.getInstance()).deserialize("201234"),
//                        elName), "<" + elName + ">201234</" + elName + ">");
    }

    @Test
    public void snAsYangInt64ToXmlTest() {
//        final String elName = "lfInt64";
//        serializeToXml(
//                prepareCnStructForYangData(
//                        TypeDefinitionAwareCodec.from(Int64.getInstance()).deserialize("5123456789"), elName), "<"
//                        + elName + ">5123456789</" + elName + ">");
    }

    @Test
    public void snAsYangUint8ToXmlTest() {
//        final String elName = "lfUint8";
//        serializeToXml(
//                prepareCnStructForYangData(TypeDefinitionAwareCodec.from(Uint8.getInstance()).deserialize("200"),
//                        elName), "<" + elName + ">200</" + elName + ">");
    }

    @Test
    public void snAsYangUint16ToXmlTest() {
//        final String elName = "lfUint16";
//        serializeToXml(
//                prepareCnStructForYangData(TypeDefinitionAwareCodec.from(Uint16.getInstance()).deserialize("4000"),
//                        elName), "<" + elName + ">4000</" + elName + ">");
    }

    @Test
    public void snAsYangUint32ToXmlTest() {
//        final String elName = "lfUint32";
//        serializeToXml(
//                prepareCnStructForYangData(TypeDefinitionAwareCodec.from(Uint32.getInstance())
//                        .deserialize("4123456789"), elName), "<" + elName + ">4123456789</" + elName + ">");
    }

    @Test
    public void snAsYangUint64ToXmlTest() {
//        final String elName = "lfUint64";
//        serializeToXml(
//                prepareCnStructForYangData(TypeDefinitionAwareCodec.from(Uint64.getInstance())
//                        .deserialize("5123456789"), elName), "<" + elName + ">5123456789</" + elName + ">");
    }

    @Test
    public void snAsYangBinaryToXmlTest() {
//        final String elName = "lfBinary";
//        serializeToXml(
//                prepareCnStructForYangData(
//                        TypeDefinitionAwareCodec.from(BinaryType.getInstance()).deserialize(
//                                "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz01234567"), elName), "<" + elName
//                        + ">ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz01234567</" + elName + ">");
    }

    @Test
    public void snAsYangBitsToXmlTest() {
//        final BitsTypeDefinition.Bit mockBit1 = mock(BitsTypeDefinition.Bit.class);
//        when(mockBit1.getName()).thenReturn("one");
//        final BitsTypeDefinition.Bit mockBit2 = mock(BitsTypeDefinition.Bit.class);
//        when(mockBit2.getName()).thenReturn("two");
//        final List<BitsTypeDefinition.Bit> bitList = Lists.newArrayList(mockBit1, mockBit2);
//
//        final String elName = "lfBits";
//        serializeToXml(
//                prepareCnStructForYangData(
//                        TypeDefinitionAwareCodec.from(BitsType.create(mock(SchemaPath.class), bitList)).deserialize(
//                                "one two"), elName), "<" + elName + ">one two</" + elName + ">", "<" + elName
//                        + ">two one</" + elName + ">");
    }

    @Test
    public void snAsYangEnumerationToXmlTest() {
//        final EnumTypeDefinition.EnumPair mockEnum = mock(EnumTypeDefinition.EnumPair.class);
//        when(mockEnum.getName()).thenReturn("enum2");
//        final List<EnumPair> enumList = Lists.newArrayList(mockEnum);
//
//        final String elName = "lfEnumeration";
//        serializeToXml(
//                prepareCnStructForYangData(
//                        TypeDefinitionAwareCodec.from(
//                                EnumerationType.create(mock(SchemaPath.class), enumList,
//                                        Optional.<EnumTypeDefinition.EnumPair> absent())).deserialize("enum2"), elName),
//                "<" + elName + ">enum2</" + elName + ">");
    }

    @Test
    public void snAsYangEmptyToXmlTest() {
//        final String elName = "lfEmpty";
//        serializeToXml(
//                prepareCnStructForYangData(TypeDefinitionAwareCodec.from(EmptyType.getInstance()).deserialize(null),
//                        elName), "<" + elName + "/>");
    }

    @Test
    public void snAsYangBooleanToXmlTest() {
//        final String elName = "lfBoolean";
//        serializeToXml(
//                prepareCnStructForYangData(TypeDefinitionAwareCodec.from(BooleanType.getInstance()).deserialize("str"),
//                        elName), "<" + elName + ">false</" + elName + ">");
//        serializeToXml(
//                prepareCnStructForYangData(
//                        TypeDefinitionAwareCodec.from(BooleanType.getInstance()).deserialize("true"), elName), "<"
//                        + elName + ">true</" + elName + ">");
    }

    @Test
    public void snAsYangUnionToXmlTest() {

//        final BitsTypeDefinition.Bit mockBit1 = mock(BitsTypeDefinition.Bit.class);
//        when(mockBit1.getName()).thenReturn("first");
//        final BitsTypeDefinition.Bit mockBit2 = mock(BitsTypeDefinition.Bit.class);
//        when(mockBit2.getName()).thenReturn("second");
//        final List<BitsTypeDefinition.Bit> bitList = Lists.newArrayList(mockBit1, mockBit2);
//
//        final List<TypeDefinition<?>> types = Lists.<TypeDefinition<?>> newArrayList(Int8.getInstance(),
//                BitsType.create(mock(SchemaPath.class), bitList), BooleanType.getInstance());
//        final UnionType unionType = UnionType.create(types);
//
//        final String elName = "lfUnion";
//        final String int8 = "15";
//        serializeToXml(prepareCnStructForYangData(TypeDefinitionAwareCodec.from(unionType).deserialize(int8), elName),
//                "<" + elName + ">15</" + elName + ">");
//
//        final String bits = "first second";
//        serializeToXml(prepareCnStructForYangData(TypeDefinitionAwareCodec.from(unionType).deserialize(bits), elName),
//                "<" + elName + ">first second</" + elName + ">");
//
//        final String bool = "str";
//        serializeToXml(prepareCnStructForYangData(TypeDefinitionAwareCodec.from(unionType).deserialize(bool), elName),
//                "<" + elName + ">str</" + elName + ">");
    }

//    private void serializeToXml(final CompositeNode compositeNode, final String... xmlRepresentation)
//            throws TransformerFactoryConfigurationError {
//        String xmlString = "";
//        try {
//            xmlString = TestUtils.writeCompNodeWithSchemaContextToOutput(compositeNode, modules, dataSchemaNode,
//                    StructuredDataToXmlProvider.INSTANCE);
//        } catch (WebApplicationException | IOException e) {
//        }
//        assertNotNull(xmlString);
//        boolean containSearchedStr = false;
//        String strRepresentation = "";
//        for (final String searchedStr : xmlRepresentation) {
//            if (xmlString.contains(searchedStr)) {
//                containSearchedStr = true;
//                break;
//            }
//            strRepresentation = strRepresentation + "[" + searchedStr + "]";
//        }
//        assertTrue("At least one of specified strings " + strRepresentation + " wasn't found.", containSearchedStr);
//
//    }

//    private CompositeNode prepareIdentityrefData(final String prefix, final boolean valueAsQName) {
//        final MutableCompositeNode cont = NodeFactory.createMutableCompositeNode(
//                TestUtils.buildQName("cont", "basic:module", "2013-12-2"), null, null, ModifyAction.CREATE, null);
//        final MutableCompositeNode cont1 = NodeFactory.createMutableCompositeNode(
//                TestUtils.buildQName("cont1", "basic:module", "2013-12-2"), cont, null, ModifyAction.CREATE, null);
//        cont.getValue().add(cont1);
//
//        Object value = null;
//        if (valueAsQName) {
//            value = TestUtils.buildQName("iden", "referenced:module", "2013-12-2", prefix);
//        } else {
//            value = "no qname value";
//        }
//        final MutableSimpleNode<Object> lf11 = NodeFactory.createMutableSimpleNode(
//                TestUtils.buildQName("lf11", "basic:module", "2013-12-2"), cont1, value, ModifyAction.CREATE, null);
//        cont1.getValue().add(lf11);
//        cont1.init();
//        cont.init();
//
//        return cont;
//    }

//    private CompositeNode prepareCnStructForYangData(final Object data, final String leafName) {
//        final MutableCompositeNode cont = NodeFactory.createMutableCompositeNode(
//                TestUtils.buildQName("cont", "basic:module", "2013-12-2"), null, null, ModifyAction.CREATE, null);
//
//        final MutableSimpleNode<Object> lf1 = NodeFactory.createMutableSimpleNode(
//                TestUtils.buildQName(leafName, "basic:module", "2013-12-2"), cont, data, ModifyAction.CREATE, null);
//        cont.getValue().add(lf1);
//        cont.init();
//
//        return cont;
//    }

//    private CompositeNode prepareLeafrefData() {
//        final MutableCompositeNode cont = NodeFactory.createMutableCompositeNode(TestUtils.buildQName("cont"), null, null,
//                ModifyAction.CREATE, null);
//
//        final MutableSimpleNode<Object> lfBoolean = NodeFactory.createMutableSimpleNode(TestUtils.buildQName("lfBoolean"),
//                cont, Boolean.TRUE, ModifyAction.CREATE, null);
//        final MutableSimpleNode<Object> lfLfref = NodeFactory.createMutableSimpleNode(TestUtils.buildQName("lfLfref"), cont,
//                "true", ModifyAction.CREATE, null);
//        cont.getValue().add(lfBoolean);
//        cont.getValue().add(lfLfref);
//        cont.init();
//
//        return cont;
//    }

}
