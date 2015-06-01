/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.restconf.impl.nn.to.xml.test;

import static org.junit.Assert.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.xml.transform.TransformerFactoryConfigurationError;

import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mockito;
import org.opendaylight.controller.sal.rest.impl.NormalizedNodeXmlBodyWriter;
import org.opendaylight.controller.sal.rest.impl.test.providers.AbstractBodyReaderTest;
import org.opendaylight.controller.sal.restconf.impl.ControllerContext;
import org.opendaylight.controller.sal.restconf.impl.InstanceIdentifierContext;
import org.opendaylight.controller.sal.restconf.impl.NormalizedNodeContext;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;
import org.opendaylight.yangtools.yang.data.impl.codec.TypeDefinitionAwareCodec;
import org.opendaylight.yangtools.yang.data.impl.schema.Builders;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.api.DataContainerNodeAttrBuilder;
import org.opendaylight.yangtools.yang.model.api.ContainerSchemaNode;
import org.opendaylight.yangtools.yang.model.api.DataNodeContainer;
import org.opendaylight.yangtools.yang.model.api.DataSchemaNode;
import org.opendaylight.yangtools.yang.model.api.LeafSchemaNode;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
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

import com.google.common.base.Optional;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

public class NnToXmlTest extends AbstractBodyReaderTest {

    private final NormalizedNodeXmlBodyWriter xmlBodyWriter;
    private static SchemaContext schemaContext;

    public NnToXmlTest() throws NoSuchFieldException, SecurityException {
        super();
        xmlBodyWriter = new NormalizedNodeXmlBodyWriter();
    }

    @BeforeClass
    public static void initialization() {
        schemaContext = schemaContextLoader("/nn-to-xml/yang", schemaContext);
        controllerContext.setSchemas(schemaContext);
    }

    @Test
    public void nnAsYangIdentityrefToXMLTest() {
        NormalizedNodeContext normalizedNodeContext = prepareIdrefData(null,
                true);
        serializeToXml(normalizedNodeContext,
                "<lf11 xmlns:x=\"referenced:module\">x:iden</lf11>");
    }

    @Test
    public void nnAsYangIdentityrefWithQNamePrefixToXMLTest() {
        NormalizedNodeContext normalizedNodeContext = prepareIdrefData(
                "prefix", true);
        serializeToXml(normalizedNodeContext, "<lf11 xmlns",
                "=\"referenced:module\">", ":iden</lf11>");
    }

    @Test
    public void nnAsYangIdentityrefWithPrefixToXMLTest() {
        NormalizedNodeContext normalizedNodeContext = prepareIdrefData(
                "prefix", false);
        serializeToXml(normalizedNodeContext, "<lf11>no qname value</lf11>");
    }

    @Test
    public void nnAsYangLeafrefWithPrefixToXMLTest() {
        NormalizedNodeContext normalizedNodeContext = prepareLeafrefData();
        serializeToXml(normalizedNodeContext, "<lfBoolean>true</lfBoolean>",
                "<lfLfref>true</lfLfref>");
    }

    @Test
    public void nnAsYangStringToXmlTest() {
        NormalizedNodeContext normalizedNodeContext = prepareNNC(
                TypeDefinitionAwareCodec.from(StringType.getInstance())
                        .deserialize("lfStr value"), "lfStr");
        serializeToXml(normalizedNodeContext, "<lfStr>lfStr value</lfStr>");
    }

    @Test
    public void nnAsYangInt8ToXmlTest() {
        final String elName = "lfInt8";

        NormalizedNodeContext normalizedNodeContext = prepareNNC(
                TypeDefinitionAwareCodec.from(Int8.getInstance()).deserialize(
                        "14"), elName);
        serializeToXml(normalizedNodeContext, "<" + elName + ">14</" + elName
                + ">");
    }

    @Test
    public void nnAsYangInt16ToXmlTest() {
        final String elName = "lfInt16";

        NormalizedNodeContext normalizedNodeContext = prepareNNC(
                TypeDefinitionAwareCodec.from(Int16.getInstance()).deserialize(
                        "3000"), elName);
        serializeToXml(normalizedNodeContext, "<" + elName + ">3000</" + elName
                + ">");
    }

    @Test
    public void nnAsYangInt32ToXmlTest() {
        final String elName = "lfInt32";

        NormalizedNodeContext normalizedNodeContext = prepareNNC(
                TypeDefinitionAwareCodec.from(Int32.getInstance()).deserialize(
                        "201234"), elName);
        serializeToXml(normalizedNodeContext, "<" + elName + ">201234</"
                + elName + ">");
    }

    @Test
    public void nnAsYangInt64ToXmlTest() {
        final String elName = "lfInt64";

        NormalizedNodeContext normalizedNodeContext = prepareNNC(
                TypeDefinitionAwareCodec.from(Int64.getInstance()).deserialize(
                        "5123456789"), elName);
        serializeToXml(normalizedNodeContext, "<" + elName + ">5123456789</"
                + elName + ">");
    }

    @Test
    public void nnAsYangUint8ToXmlTest() {
        final String elName = "lfUint8";

        NormalizedNodeContext normalizedNodeContext = prepareNNC(
                TypeDefinitionAwareCodec.from(Uint8.getInstance()).deserialize(
                        "200"), elName);
        serializeToXml(normalizedNodeContext, "<" + elName + ">200</" + elName
                + ">");
    }

    @Test
    public void snAsYangUint16ToXmlTest() {
        final String elName = "lfUint16";

        NormalizedNodeContext normalizedNodeContext = prepareNNC(
                TypeDefinitionAwareCodec.from(Uint16.getInstance())
                        .deserialize("4000"), elName);
        serializeToXml(normalizedNodeContext, "<" + elName + ">4000</" + elName
                + ">");
    }

    @Test
    public void nnAsYangUint32ToXmlTest() {
        final String elName = "lfUint32";

        NormalizedNodeContext normalizedNodeContext = prepareNNC(
                TypeDefinitionAwareCodec.from(Uint32.getInstance())
                        .deserialize("4123456789"), elName);
        serializeToXml(normalizedNodeContext, "<" + elName + ">4123456789</"
                + elName + ">");
    }

    @Test
    public void snAsYangUint64ToXmlTest() {
        final String elName = "lfUint64";
        NormalizedNodeContext normalizedNodeContext = prepareNNC(
                TypeDefinitionAwareCodec.from(Uint64.getInstance())
                        .deserialize("5123456789"), elName);
        serializeToXml(normalizedNodeContext, "<" + elName + ">5123456789</"
                + elName + ">");
    }

    @Test
    public void nnAsYangBinaryToXmlTest() {
        final String elName = "lfBinary";
        NormalizedNodeContext normalizedNodeContext = prepareNNC(
                TypeDefinitionAwareCodec
                        .from(BinaryType.getInstance())
                        .deserialize(
                                "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz01234567"),
                elName);
        serializeToXml(
                normalizedNodeContext,
                "<"
                        + elName
                        + ">ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz01234567</"
                        + elName + ">");
    }

    @Test
    public void nnAsYangBitsToXmlTest() {
        final BitsTypeDefinition.Bit mockBit1 = Mockito
                .mock(BitsTypeDefinition.Bit.class);
        Mockito.when(mockBit1.getName()).thenReturn("one");
        final BitsTypeDefinition.Bit mockBit2 = Mockito
                .mock(BitsTypeDefinition.Bit.class);
        Mockito.when(mockBit2.getName()).thenReturn("two");
        final List<BitsTypeDefinition.Bit> bitList = Lists.newArrayList(
                mockBit1, mockBit2);

        final String elName = "lfBits";
        NormalizedNodeContext normalizedNodeContext = prepareNNC(
                TypeDefinitionAwareCodec
                        .from(BitsType.create(Mockito.mock(SchemaPath.class),
                                bitList)).deserialize("one two"), elName);
        serializeToXml(normalizedNodeContext, "<" + elName + ">one two</"
                + elName + ">");
    }

    @Test
    public void nnAsYangEnumerationToXmlTest() {
        final EnumTypeDefinition.EnumPair mockEnum = Mockito
                .mock(EnumTypeDefinition.EnumPair.class);
        Mockito.when(mockEnum.getName()).thenReturn("enum2");
        final List<EnumPair> enumList = Lists.newArrayList(mockEnum);

        final String elName = "lfEnumeration";
        NormalizedNodeContext normalizedNodeContext = prepareNNC(
                TypeDefinitionAwareCodec
                        .from(EnumerationType.create(
                                Mockito.mock(SchemaPath.class), enumList,
                                Optional.<EnumTypeDefinition.EnumPair> absent()))
                        .deserialize("enum2"), elName);
        serializeToXml(normalizedNodeContext, "<" + elName + ">enum2</"
                + elName + ">");
    }

    @Test
    public void nnAsYangEmptyToXmlTest() {
        final String elName = "lfEmpty";
        NormalizedNodeContext normalizedNodeContext = prepareNNC(
                TypeDefinitionAwareCodec.from(EmptyType.getInstance())
                        .deserialize(null), elName);
        serializeToXml(normalizedNodeContext, "<" + elName + "></" + elName
                + ">");
    }

    @Test
    public void nnAsYangBooleanToXmlTest() {
        final String elName = "lfBoolean";
        NormalizedNodeContext normalizedNodeContext = prepareNNC(
                TypeDefinitionAwareCodec.from(BooleanType.getInstance())
                        .deserialize("str"), elName);
        serializeToXml(normalizedNodeContext, "<" + elName + ">false</"
                + elName + ">");

        normalizedNodeContext = prepareNNC(
                TypeDefinitionAwareCodec.from(BooleanType.getInstance())
                        .deserialize("true"), elName);
        serializeToXml(normalizedNodeContext, "<" + elName + ">true</" + elName
                + ">");
    }

    @Test
    public void nnAsYangUnionToXmlTest() {

        final BitsTypeDefinition.Bit mockBit1 = Mockito
                .mock(BitsTypeDefinition.Bit.class);
        Mockito.when(mockBit1.getName()).thenReturn("first");
        final BitsTypeDefinition.Bit mockBit2 = Mockito
                .mock(BitsTypeDefinition.Bit.class);
        Mockito.when(mockBit2.getName()).thenReturn("second");
        final List<BitsTypeDefinition.Bit> bitList = Lists.newArrayList(
                mockBit1, mockBit2);

        final List<TypeDefinition<?>> types = Lists
                .<TypeDefinition<?>> newArrayList(Int8.getInstance(), BitsType
                        .create(Mockito.mock(SchemaPath.class), bitList),
                        BooleanType.getInstance());
        final UnionType unionType = UnionType.create(types);

        final String elName = "lfUnion";
        final String int8 = "15";

        NormalizedNodeContext normalizedNodeContext = prepareNNC(
                TypeDefinitionAwareCodec.from(unionType).deserialize(int8),
                elName);
        serializeToXml(normalizedNodeContext, "<" + elName + ">15</" + elName
                + ">");

        final String bits = "first second";
        normalizedNodeContext = prepareNNC(
                TypeDefinitionAwareCodec.from(unionType).deserialize(bits),
                elName);
        serializeToXml(normalizedNodeContext, "<" + elName + ">first second</"
                + elName + ">");

        final String bool = "str";
        normalizedNodeContext = prepareNNC(
                TypeDefinitionAwareCodec.from(unionType).deserialize(bool),
                elName);
        serializeToXml(normalizedNodeContext, "<" + elName + ">str</" + elName
                + ">");
    }

    private NormalizedNodeContext prepareNNC(Object object, String name) {
        QName cont = QName.create("basic:module", "2013-12-2", "cont");
        QName lf = QName.create("basic:module", "2013-12-2", name);

        DataSchemaNode contSchema = schemaContext.getDataChildByName(cont);

        DataContainerNodeAttrBuilder<NodeIdentifier, ContainerNode> contData = Builders
                .containerBuilder((ContainerSchemaNode) contSchema);

        List<DataSchemaNode> instanceLf = ControllerContext
                .findInstanceDataChildrenByName((DataNodeContainer) contSchema,
                        lf.getLocalName());
        DataSchemaNode schemaLf = Iterables.getFirst(instanceLf, null);

        contData.withChild(Builders.leafBuilder((LeafSchemaNode) schemaLf)
                .withValue(object).build());

        NormalizedNodeContext testNormalizedNodeContext = new NormalizedNodeContext(
                new InstanceIdentifierContext<DataSchemaNode>(null, contSchema,
                        null, schemaContext), contData.build());

        return testNormalizedNodeContext;
    }

    private void serializeToXml(
            final NormalizedNodeContext normalizedNodeContext,
            final String... xmlRepresentation)
            throws TransformerFactoryConfigurationError {
        final OutputStream output = new ByteArrayOutputStream();
        try {
            xmlBodyWriter.writeTo(normalizedNodeContext, null, null, null,
                    mediaType, null, output);
        } catch (WebApplicationException | IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        for (int i = 0; i < xmlRepresentation.length; i++) {
            assertTrue(output.toString().contains(xmlRepresentation[i]));
        }
    }

    private NormalizedNodeContext prepareLeafrefData() {
        QName cont = QName.create("basic:module", "2013-12-2", "cont");
        QName lfBoolean = QName
                .create("basic:module", "2013-12-2", "lfBoolean");
        QName lfLfref = QName.create("basic:module", "2013-12-2", "lfLfref");

        DataSchemaNode contSchema = schemaContext.getDataChildByName(cont);

        DataContainerNodeAttrBuilder<NodeIdentifier, ContainerNode> contData = Builders
                .containerBuilder((ContainerSchemaNode) contSchema);

        List<DataSchemaNode> instanceLf = ControllerContext
                .findInstanceDataChildrenByName((DataNodeContainer) contSchema,
                        lfBoolean.getLocalName());
        DataSchemaNode schemaLf = Iterables.getFirst(instanceLf, null);

        contData.withChild(Builders.leafBuilder((LeafSchemaNode) schemaLf)
                .withValue(Boolean.TRUE).build());

        instanceLf = ControllerContext.findInstanceDataChildrenByName(
                (DataNodeContainer) contSchema, lfLfref.getLocalName());
        schemaLf = Iterables.getFirst(instanceLf, null);

        contData.withChild(Builders.leafBuilder((LeafSchemaNode) schemaLf)
                .withValue("true").build());

        NormalizedNodeContext testNormalizedNodeContext = new NormalizedNodeContext(
                new InstanceIdentifierContext<DataSchemaNode>(null, contSchema,
                        null, schemaContext), contData.build());

        return testNormalizedNodeContext;
    }

    private NormalizedNodeContext prepareIdrefData(String prefix,
            boolean valueAsQName) {
        QName cont = QName.create("basic:module", "2013-12-2", "cont");
        QName cont1 = QName.create("basic:module", "2013-12-2", "cont1");
        QName lf11 = QName.create("basic:module", "2013-12-2", "lf11");

        DataSchemaNode contSchema = schemaContext.getDataChildByName(cont);

        DataContainerNodeAttrBuilder<NodeIdentifier, ContainerNode> contData = Builders
                .containerBuilder((ContainerSchemaNode) contSchema);

        DataSchemaNode cont1Schema = ((ContainerSchemaNode) contSchema)
                .getDataChildByName(cont1);

        DataContainerNodeAttrBuilder<NodeIdentifier, ContainerNode> cont1Data = Builders
                .containerBuilder((ContainerSchemaNode) cont1Schema);

        Object value = null;
        if (valueAsQName) {
            value = QName.create("referenced:module", "2013-12-2", "iden");
        } else {
            value = "no qname value";
        }

        List<DataSchemaNode> instanceLf = ControllerContext
                .findInstanceDataChildrenByName(
                        (DataNodeContainer) cont1Schema, lf11.getLocalName());
        DataSchemaNode schemaLf = Iterables.getFirst(instanceLf, null);

        cont1Data.withChild(Builders.leafBuilder((LeafSchemaNode) schemaLf)
                .withValue(value).build());

        contData.withChild(cont1Data.build());

        NormalizedNodeContext testNormalizedNodeContext = new NormalizedNodeContext(
                new InstanceIdentifierContext<DataSchemaNode>(null, contSchema,
                        null, schemaContext), contData.build());
        return testNormalizedNodeContext;
    }

    @Override
    protected MediaType getMediaType() {
        // TODO Auto-generated method stub
        return null;
    }
}
