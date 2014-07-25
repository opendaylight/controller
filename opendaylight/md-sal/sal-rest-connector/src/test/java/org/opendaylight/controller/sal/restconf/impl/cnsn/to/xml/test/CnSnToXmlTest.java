/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.restconf.impl.cnsn.to.xml.test;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import java.io.IOException;
import java.util.List;
import javax.ws.rs.WebApplicationException;
import javax.xml.transform.TransformerFactoryConfigurationError;
import org.junit.BeforeClass;
import org.junit.Test;
import org.opendaylight.controller.sal.rest.impl.StructuredDataToXmlProvider;
import org.opendaylight.controller.sal.restconf.impl.test.TestUtils;
import org.opendaylight.controller.sal.restconf.impl.test.YangAndXmlAndDataSchemaLoader;
import org.opendaylight.yangtools.yang.data.api.CompositeNode;
import org.opendaylight.yangtools.yang.data.api.ModifyAction;
import org.opendaylight.yangtools.yang.data.api.MutableCompositeNode;
import org.opendaylight.yangtools.yang.data.api.MutableSimpleNode;
import org.opendaylight.yangtools.yang.data.impl.NodeFactory;
import org.opendaylight.yangtools.yang.data.impl.codec.TypeDefinitionAwareCodec;
import org.opendaylight.yangtools.yang.model.api.SchemaPath;
import org.opendaylight.yangtools.yang.model.api.TypeDefinition;
import org.opendaylight.yangtools.yang.model.api.type.BitsTypeDefinition;
import org.opendaylight.yangtools.yang.model.api.type.EnumTypeDefinition;
import org.opendaylight.yangtools.yang.model.api.type.EnumTypeDefinition.EnumPair;
import org.opendaylight.yangtools.yang.model.util.BinaryType;
import org.opendaylight.yangtools.yang.model.util.BitsType;
import org.opendaylight.yangtools.yang.model.util.BooleanType;
import org.opendaylight.yangtools.yang.model.util.EmptyType;
import org.opendaylight.yangtools.yang.model.util.EnumerationType;
import org.opendaylight.yangtools.yang.model.util.Int16;
import org.opendaylight.yangtools.yang.model.util.Int32;
import org.opendaylight.yangtools.yang.model.util.Int64;
import org.opendaylight.yangtools.yang.model.util.Int8;
import org.opendaylight.yangtools.yang.model.util.StringType;
import org.opendaylight.yangtools.yang.model.util.Uint16;
import org.opendaylight.yangtools.yang.model.util.Uint32;
import org.opendaylight.yangtools.yang.model.util.Uint64;
import org.opendaylight.yangtools.yang.model.util.Uint8;
import org.opendaylight.yangtools.yang.model.util.UnionType;

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
        serializeToXml(prepareIdentityrefData(null, true), "<lf11 xmlns:x=\"referenced:module\">x:iden</lf11>");
    }

    @Test
    public void snAsYangIdentityrefWithQNamePrefixToXMLTest() {
        serializeToXml(prepareIdentityrefData("prefix", true),
                "<lf11 xmlns:prefix=\"referenced:module\">prefix:iden</lf11>");
    }

    @Test
    public void snAsYangIdentityrefWithPrefixToXMLTest() {
        serializeToXml(prepareIdentityrefData("prefix", false), "<lf11>no qname value</lf11>");
    }

    @Test
    public void snAsYangLeafrefWithPrefixToXMLTest() {
        serializeToXml(prepareLeafrefData(), "<lfBoolean>true</lfBoolean>", "<lfLfref>true</lfLfref>");
    }

    @Test
    public void snAsYangStringToXmlTest() {
        serializeToXml(
                prepareCnStructForYangData(
                        TypeDefinitionAwareCodec.from(StringType.getInstance()).deserialize("lfStr value"), "lfStr"),
                "<lfStr>lfStr value</lfStr>");
    }

    @Test
    public void snAsYangInt8ToXmlTest() {
        String elName = "lfInt8";
        serializeToXml(
                prepareCnStructForYangData(TypeDefinitionAwareCodec.from(Int8.getInstance()).deserialize("14"), elName),
                "<" + elName + ">14</" + elName + ">");
    }

    @Test
    public void snAsYangInt16ToXmlTest() {
        String elName = "lfInt16";
        serializeToXml(
                prepareCnStructForYangData(TypeDefinitionAwareCodec.from(Int16.getInstance()).deserialize("3000"),
                        elName), "<" + elName + ">3000</" + elName + ">");
    }

    @Test
    public void snAsYangInt32ToXmlTest() {
        String elName = "lfInt32";
        serializeToXml(
                prepareCnStructForYangData(TypeDefinitionAwareCodec.from(Int32.getInstance()).deserialize("201234"),
                        elName), "<" + elName + ">201234</" + elName + ">");
    }

    @Test
    public void snAsYangInt64ToXmlTest() {
        String elName = "lfInt64";
        serializeToXml(
                prepareCnStructForYangData(
                        TypeDefinitionAwareCodec.from(Int64.getInstance()).deserialize("5123456789"), elName), "<"
                        + elName + ">5123456789</" + elName + ">");
    }

    @Test
    public void snAsYangUint8ToXmlTest() {
        String elName = "lfUint8";
        serializeToXml(
                prepareCnStructForYangData(TypeDefinitionAwareCodec.from(Uint8.getInstance()).deserialize("200"),
                        elName), "<" + elName + ">200</" + elName + ">");
    }

    @Test
    public void snAsYangUint16ToXmlTest() {
        String elName = "lfUint16";
        serializeToXml(
                prepareCnStructForYangData(TypeDefinitionAwareCodec.from(Uint16.getInstance()).deserialize("4000"),
                        elName), "<" + elName + ">4000</" + elName + ">");
    }

    @Test
    public void snAsYangUint32ToXmlTest() {
        String elName = "lfUint32";
        serializeToXml(
                prepareCnStructForYangData(TypeDefinitionAwareCodec.from(Uint32.getInstance())
                        .deserialize("4123456789"), elName), "<" + elName + ">4123456789</" + elName + ">");
    }

    @Test
    public void snAsYangUint64ToXmlTest() {
        String elName = "lfUint64";
        serializeToXml(
                prepareCnStructForYangData(TypeDefinitionAwareCodec.from(Uint64.getInstance())
                        .deserialize("5123456789"), elName), "<" + elName + ">5123456789</" + elName + ">");
    }

    @Test
    public void snAsYangBinaryToXmlTest() {
        String elName = "lfBinary";
        serializeToXml(
                prepareCnStructForYangData(
                        TypeDefinitionAwareCodec.from(BinaryType.getInstance()).deserialize(
                                "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz01234567"), elName), "<" + elName
                        + ">ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz01234567</" + elName + ">");
    }

    @Test
    public void snAsYangBitsToXmlTest() {
        BitsTypeDefinition.Bit mockBit1 = mock(BitsTypeDefinition.Bit.class);
        when(mockBit1.getName()).thenReturn("one");
        BitsTypeDefinition.Bit mockBit2 = mock(BitsTypeDefinition.Bit.class);
        when(mockBit2.getName()).thenReturn("two");
        List<BitsTypeDefinition.Bit> bitList = Lists.newArrayList(mockBit1, mockBit2);

        String elName = "lfBits";
        serializeToXml(
                prepareCnStructForYangData(
                        TypeDefinitionAwareCodec.from(BitsType.create(mock(SchemaPath.class), bitList)).deserialize(
                                "one two"), elName), "<" + elName + ">one two</" + elName + ">", "<" + elName
                        + ">two one</" + elName + ">");
    }

    @Test
    public void snAsYangEnumerationToXmlTest() {
        EnumTypeDefinition.EnumPair mockEnum = mock(EnumTypeDefinition.EnumPair.class);
        when(mockEnum.getName()).thenReturn("enum2");
        List<EnumPair> enumList = Lists.newArrayList(mockEnum);

        String elName = "lfEnumeration";
        serializeToXml(
                prepareCnStructForYangData(
                        TypeDefinitionAwareCodec.from(
                                EnumerationType.create(mock(SchemaPath.class), enumList,
                                        Optional.<EnumTypeDefinition.EnumPair> absent())).deserialize("enum2"), elName),
                "<" + elName + ">enum2</" + elName + ">");
    }

    @Test
    public void snAsYangEmptyToXmlTest() {
        String elName = "lfEmpty";
        serializeToXml(
                prepareCnStructForYangData(TypeDefinitionAwareCodec.from(EmptyType.getInstance()).deserialize(null),
                        elName), "<" + elName + "/>");
    }

    @Test
    public void snAsYangBooleanToXmlTest() {
        String elName = "lfBoolean";
        serializeToXml(
                prepareCnStructForYangData(TypeDefinitionAwareCodec.from(BooleanType.getInstance()).deserialize("str"),
                        elName), "<" + elName + ">false</" + elName + ">");
        serializeToXml(
                prepareCnStructForYangData(
                        TypeDefinitionAwareCodec.from(BooleanType.getInstance()).deserialize("true"), elName), "<"
                        + elName + ">true</" + elName + ">");
    }

    @Test
    public void snAsYangUnionToXmlTest() {

        BitsTypeDefinition.Bit mockBit1 = mock(BitsTypeDefinition.Bit.class);
        when(mockBit1.getName()).thenReturn("first");
        BitsTypeDefinition.Bit mockBit2 = mock(BitsTypeDefinition.Bit.class);
        when(mockBit2.getName()).thenReturn("second");
        List<BitsTypeDefinition.Bit> bitList = Lists.newArrayList(mockBit1, mockBit2);

        List<TypeDefinition<?>> types = Lists.<TypeDefinition<?>> newArrayList(Int8.getInstance(),
                BitsType.create(mock(SchemaPath.class), bitList), BooleanType.getInstance());
        UnionType unionType = UnionType.create(types);

        String elName = "lfUnion";
        String int8 = "15";
        serializeToXml(prepareCnStructForYangData(TypeDefinitionAwareCodec.from(unionType).deserialize(int8), elName),
                "<" + elName + ">15</" + elName + ">");

        String bits = "first second";
        serializeToXml(prepareCnStructForYangData(TypeDefinitionAwareCodec.from(unionType).deserialize(bits), elName),
                "<" + elName + ">first second</" + elName + ">");

        String bool = "str";
        serializeToXml(prepareCnStructForYangData(TypeDefinitionAwareCodec.from(unionType).deserialize(bool), elName),
                "<" + elName + ">str</" + elName + ">");
    }

    private void serializeToXml(final CompositeNode compositeNode, final String... xmlRepresentation)
            throws TransformerFactoryConfigurationError {
        String xmlString = "";
        try {
            xmlString = TestUtils.writeCompNodeWithSchemaContextToOutput(compositeNode, modules, dataSchemaNode,
                    StructuredDataToXmlProvider.INSTANCE);
        } catch (WebApplicationException | IOException e) {
        }
        assertNotNull(xmlString);
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

    private CompositeNode prepareIdentityrefData(final String prefix, final boolean valueAsQName) {
        MutableCompositeNode cont = NodeFactory.createMutableCompositeNode(
                TestUtils.buildQName("cont", "basic:module", "2013-12-2"), null, null, ModifyAction.CREATE, null);
        MutableCompositeNode cont1 = NodeFactory.createMutableCompositeNode(
                TestUtils.buildQName("cont1", "basic:module", "2013-12-2"), cont, null, ModifyAction.CREATE, null);
        cont.getValue().add(cont1);

        Object value = null;
        if (valueAsQName) {
            value = TestUtils.buildQName("iden", "referenced:module", "2013-12-2", prefix);
        } else {
            value = "no qname value";
        }
        MutableSimpleNode<Object> lf11 = NodeFactory.createMutableSimpleNode(
                TestUtils.buildQName("lf11", "basic:module", "2013-12-2"), cont1, value, ModifyAction.CREATE, null);
        cont1.getValue().add(lf11);
        cont1.init();
        cont.init();

        return cont;
    }

    private CompositeNode prepareCnStructForYangData(final Object data, final String leafName) {
        MutableCompositeNode cont = NodeFactory.createMutableCompositeNode(
                TestUtils.buildQName("cont", "basic:module", "2013-12-2"), null, null, ModifyAction.CREATE, null);

        MutableSimpleNode<Object> lf1 = NodeFactory.createMutableSimpleNode(
                TestUtils.buildQName(leafName, "basic:module", "2013-12-2"), cont, data, ModifyAction.CREATE, null);
        cont.getValue().add(lf1);
        cont.init();

        return cont;
    }

    private CompositeNode prepareLeafrefData() {
        MutableCompositeNode cont = NodeFactory.createMutableCompositeNode(TestUtils.buildQName("cont"), null, null,
                ModifyAction.CREATE, null);

        MutableSimpleNode<Object> lfBoolean = NodeFactory.createMutableSimpleNode(TestUtils.buildQName("lfBoolean"),
                cont, Boolean.TRUE, ModifyAction.CREATE, null);
        MutableSimpleNode<Object> lfLfref = NodeFactory.createMutableSimpleNode(TestUtils.buildQName("lfLfref"), cont,
                "true", ModifyAction.CREATE, null);
        cont.getValue().add(lfBoolean);
        cont.getValue().add(lfLfref);
        cont.init();

        return cont;
    }

}
