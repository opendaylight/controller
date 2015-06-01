/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.restconf.impl.nn.to.xml.test;

import static org.junit.Assert.assertTrue;
import com.google.common.base.Optional;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.util.List;
import javax.ws.rs.core.MediaType;
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
    public void nnAsYangIdentityrefToXMLTest() throws Exception {
        final NormalizedNodeContext normalizedNodeContext = prepareIdrefData(null,
                true);
        nnToXml(normalizedNodeContext,
                "<lf11 xmlns:x=\"referenced:module\">x:iden</lf11>");
    }

    @Test
    public void nnAsYangIdentityrefWithQNamePrefixToXMLTest() throws Exception {
        final NormalizedNodeContext normalizedNodeContext = prepareIdrefData(
                "prefix", true);
        nnToXml(normalizedNodeContext, "<lf11 xmlns",
                "=\"referenced:module\">", ":iden</lf11>");
    }

    @Test
    public void nnAsYangIdentityrefWithPrefixToXMLTest() throws Exception {
        final NormalizedNodeContext normalizedNodeContext = prepareIdrefData(
                "prefix", false);
        nnToXml(normalizedNodeContext, "<lf11>no qname value</lf11>");
    }

    @Test
    public void nnAsYangLeafrefWithPrefixToXMLTest() throws Exception {
        final NormalizedNodeContext normalizedNodeContext = prepareLeafrefData();
        nnToXml(normalizedNodeContext, "<lfBoolean>true</lfBoolean>",
                "<lfLfref>true</lfLfref>");
    }

    @Test
    public void nnAsYangStringToXmlTest() throws Exception {
        final NormalizedNodeContext normalizedNodeContext = prepareNNC(
                TypeDefinitionAwareCodec.from(StringType.getInstance())
                        .deserialize("lfStr value"), "lfStr");
        nnToXml(normalizedNodeContext, "<lfStr>lfStr value</lfStr>");
    }

    @Test
    public void nnAsYangInt8ToXmlTest() throws Exception {
        final String elName = "lfInt8";

        final NormalizedNodeContext normalizedNodeContext = prepareNNC(
                TypeDefinitionAwareCodec.from(Int8.getInstance()).deserialize(
                        "14"), elName);
        nnToXml(normalizedNodeContext, "<" + elName + ">14</" + elName
                + ">");
    }

    @Test
    public void nnAsYangInt16ToXmlTest() throws Exception {
        final String elName = "lfInt16";

        final NormalizedNodeContext normalizedNodeContext = prepareNNC(
                TypeDefinitionAwareCodec.from(Int16.getInstance()).deserialize(
                        "3000"), elName);
        nnToXml(normalizedNodeContext, "<" + elName + ">3000</" + elName
                + ">");
    }

    @Test
    public void nnAsYangInt32ToXmlTest() throws Exception {
        final String elName = "lfInt32";

        final NormalizedNodeContext normalizedNodeContext = prepareNNC(
                TypeDefinitionAwareCodec.from(Int32.getInstance()).deserialize(
                        "201234"), elName);
        nnToXml(normalizedNodeContext, "<" + elName + ">201234</"
                + elName + ">");
    }

    @Test
    public void nnAsYangInt64ToXmlTest() throws Exception {
        final String elName = "lfInt64";

        final NormalizedNodeContext normalizedNodeContext = prepareNNC(
                TypeDefinitionAwareCodec.from(Int64.getInstance()).deserialize(
                        "5123456789"), elName);
        nnToXml(normalizedNodeContext, "<" + elName + ">5123456789</"
                + elName + ">");
    }

    @Test
    public void nnAsYangUint8ToXmlTest() throws Exception {
        final String elName = "lfUint8";

        final NormalizedNodeContext normalizedNodeContext = prepareNNC(
                TypeDefinitionAwareCodec.from(Uint8.getInstance()).deserialize(
                        "200"), elName);
        nnToXml(normalizedNodeContext, "<" + elName + ">200</" + elName
                + ">");
    }

    @Test
    public void snAsYangUint16ToXmlTest() throws Exception {
        final String elName = "lfUint16";

        final NormalizedNodeContext normalizedNodeContext = prepareNNC(
                TypeDefinitionAwareCodec.from(Uint16.getInstance())
                        .deserialize("4000"), elName);
        nnToXml(normalizedNodeContext, "<" + elName + ">4000</" + elName
                + ">");
    }

    @Test
    public void nnAsYangUint32ToXmlTest() throws Exception {
        final String elName = "lfUint32";

        final NormalizedNodeContext normalizedNodeContext = prepareNNC(
                TypeDefinitionAwareCodec.from(Uint32.getInstance())
                        .deserialize("4123456789"), elName);
        nnToXml(normalizedNodeContext, "<" + elName + ">4123456789</"
                + elName + ">");
    }

    @Test
    public void snAsYangUint64ToXmlTest() throws Exception {
        final String elName = "lfUint64";
        final NormalizedNodeContext normalizedNodeContext = prepareNNC(
                TypeDefinitionAwareCodec.from(Uint64.getInstance())
                        .deserialize("5123456789"), elName);
        nnToXml(normalizedNodeContext, "<" + elName + ">5123456789</"
                + elName + ">");
    }

    @Test
    public void nnAsYangBinaryToXmlTest() throws Exception {
        final String elName = "lfBinary";
        final NormalizedNodeContext normalizedNodeContext = prepareNNC(
                TypeDefinitionAwareCodec
                        .from(BinaryType.getInstance())
                        .deserialize(
                                "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz01234567"),
                elName);
        nnToXml(
                normalizedNodeContext,
                "<"
                        + elName
                        + ">ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz01234567</"
                        + elName + ">");
    }

    @Test
    public void nnAsYangBitsToXmlTest() throws Exception {
        final BitsTypeDefinition.Bit mockBit1 = Mockito
                .mock(BitsTypeDefinition.Bit.class);
        Mockito.when(mockBit1.getName()).thenReturn("one");
        final BitsTypeDefinition.Bit mockBit2 = Mockito
                .mock(BitsTypeDefinition.Bit.class);
        Mockito.when(mockBit2.getName()).thenReturn("two");
        final List<BitsTypeDefinition.Bit> bitList = Lists.newArrayList(
                mockBit1, mockBit2);

        final String elName = "lfBits";
        final NormalizedNodeContext normalizedNodeContext = prepareNNC(
                TypeDefinitionAwareCodec
                        .from(BitsType.create(Mockito.mock(SchemaPath.class),
                                bitList)).deserialize("one two"), elName);
        nnToXml(normalizedNodeContext, "<" + elName + ">one two</"
                + elName + ">");
    }

    @Test
    public void nnAsYangEnumerationToXmlTest() throws Exception {
        final EnumTypeDefinition.EnumPair mockEnum = Mockito
                .mock(EnumTypeDefinition.EnumPair.class);
        Mockito.when(mockEnum.getName()).thenReturn("enum2");
        final List<EnumPair> enumList = Lists.newArrayList(mockEnum);

        final String elName = "lfEnumeration";
        final NormalizedNodeContext normalizedNodeContext = prepareNNC(
                TypeDefinitionAwareCodec
                        .from(EnumerationType.create(
                                Mockito.mock(SchemaPath.class), enumList,
                                Optional.<EnumTypeDefinition.EnumPair> absent()))
                        .deserialize("enum2"), elName);
        nnToXml(normalizedNodeContext, "<" + elName + ">enum2</"
                + elName + ">");
    }

    @Test
    public void nnAsYangEmptyToXmlTest() throws Exception {
        final String elName = "lfEmpty";
        final NormalizedNodeContext normalizedNodeContext = prepareNNC(
                TypeDefinitionAwareCodec.from(EmptyType.getInstance())
                        .deserialize(null), elName);
        nnToXml(normalizedNodeContext, "<" + elName + "></" + elName
                + ">");
    }

    @Test
    public void nnAsYangBooleanToXmlTest() throws Exception {
        final String elName = "lfBoolean";
        NormalizedNodeContext normalizedNodeContext = prepareNNC(
                TypeDefinitionAwareCodec.from(BooleanType.getInstance())
                        .deserialize("str"), elName);
        nnToXml(normalizedNodeContext, "<" + elName + ">false</"
                + elName + ">");

        normalizedNodeContext = prepareNNC(
                TypeDefinitionAwareCodec.from(BooleanType.getInstance())
                        .deserialize("true"), elName);
        nnToXml(normalizedNodeContext, "<" + elName + ">true</" + elName
                + ">");
    }

    @Test
    public void nnAsYangUnionToXmlTest() throws Exception {

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
        nnToXml(normalizedNodeContext, "<" + elName + ">15</" + elName
                + ">");

        final String bits = "first second";
        normalizedNodeContext = prepareNNC(
                TypeDefinitionAwareCodec.from(unionType).deserialize(bits),
                elName);
        nnToXml(normalizedNodeContext, "<" + elName + ">first second</"
                + elName + ">");

        final String bool = "str";
        normalizedNodeContext = prepareNNC(
                TypeDefinitionAwareCodec.from(unionType).deserialize(bool),
                elName);
        nnToXml(normalizedNodeContext, "<" + elName + ">str</" + elName
                + ">");
    }

    private NormalizedNodeContext prepareNNC(final Object object, final String name) {
        final QName cont = QName.create("basic:module", "2013-12-2", "cont");
        final QName lf = QName.create("basic:module", "2013-12-2", name);

        final DataSchemaNode contSchema = schemaContext.getDataChildByName(cont);

        final DataContainerNodeAttrBuilder<NodeIdentifier, ContainerNode> contData = Builders
                .containerBuilder((ContainerSchemaNode) contSchema);

        final List<DataSchemaNode> instanceLf = ControllerContext
                .findInstanceDataChildrenByName((DataNodeContainer) contSchema,
                        lf.getLocalName());
        final DataSchemaNode schemaLf = Iterables.getFirst(instanceLf, null);

        contData.withChild(Builders.leafBuilder((LeafSchemaNode) schemaLf)
                .withValue(object).build());

        final NormalizedNodeContext testNormalizedNodeContext = new NormalizedNodeContext(
                new InstanceIdentifierContext<DataSchemaNode>(null, contSchema,
                        null, schemaContext), contData.build());

        return testNormalizedNodeContext;
    }

    private void nnToXml(
            final NormalizedNodeContext normalizedNodeContext,
            final String... xmlRepresentation)
 throws Exception {
        final OutputStream output = new ByteArrayOutputStream();
        xmlBodyWriter.writeTo(normalizedNodeContext, null, null, null,
                    mediaType, null, output);

        for (int i = 0; i < xmlRepresentation.length; i++) {
            assertTrue(output.toString().contains(xmlRepresentation[i]));
        }
    }

    private NormalizedNodeContext prepareLeafrefData() {
        final QName cont = QName.create("basic:module", "2013-12-2", "cont");
        final QName lfBoolean = QName
                .create("basic:module", "2013-12-2", "lfBoolean");
        final QName lfLfref = QName.create("basic:module", "2013-12-2", "lfLfref");

        final DataSchemaNode contSchema = schemaContext.getDataChildByName(cont);

        final DataContainerNodeAttrBuilder<NodeIdentifier, ContainerNode> contData = Builders
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

        final NormalizedNodeContext testNormalizedNodeContext = new NormalizedNodeContext(
                new InstanceIdentifierContext<DataSchemaNode>(null, contSchema,
                        null, schemaContext), contData.build());

        return testNormalizedNodeContext;
    }

    private NormalizedNodeContext prepareIdrefData(final String prefix,
            final boolean valueAsQName) {
        final QName cont = QName.create("basic:module", "2013-12-2", "cont");
        final QName cont1 = QName.create("basic:module", "2013-12-2", "cont1");
        final QName lf11 = QName.create("basic:module", "2013-12-2", "lf11");

        final DataSchemaNode contSchema = schemaContext.getDataChildByName(cont);

        final DataContainerNodeAttrBuilder<NodeIdentifier, ContainerNode> contData = Builders
                .containerBuilder((ContainerSchemaNode) contSchema);

        final DataSchemaNode cont1Schema = ((ContainerSchemaNode) contSchema)
                .getDataChildByName(cont1);

        final DataContainerNodeAttrBuilder<NodeIdentifier, ContainerNode> cont1Data = Builders
                .containerBuilder((ContainerSchemaNode) cont1Schema);

        Object value = null;
        if (valueAsQName) {
            value = QName.create("referenced:module", "2013-12-2", "iden");
        } else {
            value = "no qname value";
        }

        final List<DataSchemaNode> instanceLf = ControllerContext
                .findInstanceDataChildrenByName(
                        (DataNodeContainer) cont1Schema, lf11.getLocalName());
        final DataSchemaNode schemaLf = Iterables.getFirst(instanceLf, null);

        cont1Data.withChild(Builders.leafBuilder((LeafSchemaNode) schemaLf)
                .withValue(value).build());

        contData.withChild(cont1Data.build());

        final NormalizedNodeContext testNormalizedNodeContext = new NormalizedNodeContext(
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
