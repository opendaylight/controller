/*
 * Copyright (c) 2014, 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.datastore.node.utils.serialization;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.opendaylight.controller.cluster.datastore.util.TestModel;
import org.opendaylight.controller.protobuff.messages.common.NormalizedNodeMessages;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.AugmentationIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifierWithPredicates;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeWithValue;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.PathArgument;
import java.net.URI;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class PathArgumentSerializerTest{

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Test
    public void testSerializeNullContext(){
        expectedException.expect(NullPointerException.class);
        expectedException.expectMessage("context should not be null");

        PathArgumentSerializer.serialize(null, mock(PathArgument.class));
    }

    @Test
    public void testSerializeNullPathArgument(){
        expectedException.expect(NullPointerException.class);
        expectedException.expectMessage("pathArgument should not be null");

        PathArgumentSerializer.serialize(mock(QNameSerializationContext.class), null);

    }

    @Test
    public void testDeSerializeNullContext(){
        expectedException.expect(NullPointerException.class);
        expectedException.expectMessage("context should not be null");

        PathArgumentSerializer.deSerialize(null, NormalizedNodeMessages.PathArgument.getDefaultInstance());

    }

    @Test
    public void testDeSerializeNullPathArgument(){
        expectedException.expect(NullPointerException.class);
        expectedException.expectMessage("pathArgument should not be null");

        PathArgumentSerializer.deSerialize(mock(QNameDeSerializationContext.class), null);

    }

    @Test
    public void testSerializeNodeIdentifier(){
        QNameSerializationContext serializationContext = mock(QNameSerializationContext.class);

        when(serializationContext.addLocalName(anyString())).thenReturn(5);
        when(serializationContext.addNamespace(any(URI.class))).thenReturn(10);
        when(serializationContext.addRevision(any(Date.class))).thenReturn(11);

        NormalizedNodeMessages.PathArgument actual = PathArgumentSerializer
            .serialize(serializationContext,
                new NodeIdentifier(
                    TestModel.TEST_QNAME));

        assertEquals(PathArgumentType.NODE_IDENTIFIER.ordinal(), actual.getIntType());
        assertEquals(5, actual.getNodeType().getLocalName());
        assertEquals(10, actual.getNodeType().getNamespace());
        assertEquals(11, actual.getNodeType().getRevision());


    }

    @Test
    public void testSerializeNodeIdentifierWithValue(){
        QNameSerializationContext serializationContext = mock(QNameSerializationContext.class);

        when(serializationContext.addLocalName(anyString())).thenReturn(5);
        when(serializationContext.addNamespace(any(URI.class))).thenReturn(10);
        when(serializationContext.addRevision(any(Date.class))).thenReturn(11);

        NormalizedNodeMessages.PathArgument actual = PathArgumentSerializer
            .serialize(serializationContext,
                new NodeWithValue<>(
                    TestModel.TEST_QNAME, "foo"));

        assertEquals(PathArgumentType.NODE_IDENTIFIER_WITH_VALUE.ordinal(), actual.getIntType());
        assertEquals(5, actual.getNodeType().getLocalName());
        assertEquals(10, actual.getNodeType().getNamespace());
        assertEquals(11, actual.getNodeType().getRevision());
        assertEquals("foo", actual.getAttribute(0).getValue());


    }

    @Test
    public void testSerializeNodeIdentifierWithPredicates(){
        QNameSerializationContext serializationContext = mock(QNameSerializationContext.class);

        when(serializationContext.addLocalName("test")).thenReturn(5);
        when(serializationContext.addLocalName("child-name")).thenReturn(55);

        when(serializationContext.addNamespace(TestModel.TEST_QNAME.getNamespace())).thenReturn(
            10);
        when(serializationContext.addNamespace(TestModel.CHILD_NAME_QNAME.getNamespace())).thenReturn(66);

        when(serializationContext.addRevision(TestModel.TEST_QNAME.getRevision())).thenReturn(
            11);
        when(serializationContext.addRevision(TestModel.CHILD_NAME_QNAME.getRevision())).thenReturn(77);

        Map<QName, Object> predicates = new HashMap<>();

        predicates.put(TestModel.CHILD_NAME_QNAME, "foobar");

        NormalizedNodeMessages.PathArgument actual = PathArgumentSerializer
            .serialize(serializationContext,
                new NodeIdentifierWithPredicates(TestModel.TEST_QNAME, predicates));

        assertEquals(PathArgumentType.NODE_IDENTIFIER_WITH_PREDICATES.ordinal(), actual.getIntType());
        assertEquals(5, actual.getNodeType().getLocalName());
        assertEquals(10, actual.getNodeType().getNamespace());
        assertEquals(11, actual.getNodeType().getRevision());

        assertEquals(55, actual.getAttribute(0).getName().getLocalName());
        assertEquals(66, actual.getAttribute(0).getName().getNamespace());
        assertEquals(77, actual.getAttribute(0).getName().getRevision());

        assertEquals("foobar", actual.getAttribute(0).getValue());


    }

    @Test
    public void testSerializeAugmentationIdentifier(){
        QNameSerializationContext serializationContext = mock(QNameSerializationContext.class);

        when(serializationContext.addLocalName(anyString())).thenReturn(55);
        when(serializationContext.addNamespace(any(URI.class))).thenReturn(66);
        when(serializationContext.addRevision(any(Date.class))).thenReturn(77);

        NormalizedNodeMessages.PathArgument actual = PathArgumentSerializer
            .serialize(serializationContext,
                new AugmentationIdentifier(ImmutableSet.of(TestModel.TEST_QNAME)));

        assertEquals(PathArgumentType.AUGMENTATION_IDENTIFIER.ordinal(), actual.getIntType());

        assertEquals(55, actual.getAttribute(0).getName().getLocalName());
        assertEquals(66, actual.getAttribute(0).getName().getNamespace());
        assertEquals(77, actual.getAttribute(0).getName().getRevision());

    }

    @Test
    public void testDeSerializeNodeIdentifier(){

        NormalizedNodeMessages.Node.Builder nodeBuilder = NormalizedNodeMessages.Node.newBuilder();
        NormalizedNodeMessages.PathArgument.Builder pathBuilder = NormalizedNodeMessages.PathArgument.newBuilder();
        NormalizedNodeMessages.QName.Builder qNameBuilder = NormalizedNodeMessages.QName.newBuilder();

        qNameBuilder.setNamespace(0);
        qNameBuilder.setRevision(1);
        qNameBuilder.setLocalName(2);

        pathBuilder.setNodeType(qNameBuilder);
        pathBuilder.setIntType(PathArgumentType.NODE_IDENTIFIER.ordinal());

        nodeBuilder.addCode(TestModel.TEST_QNAME.getNamespace().toString());
        nodeBuilder.addCode(TestModel.TEST_QNAME.getFormattedRevision());
        nodeBuilder.addCode(TestModel.TEST_QNAME.getLocalName());

        PathArgument pathArgument = NormalizedNodeSerializer.deSerialize(nodeBuilder.build(), pathBuilder.build());

        assertEquals(new NodeIdentifier(TestModel.TEST_QNAME), pathArgument);

    }

    @Test
    public void testDeSerializeNodeWithValue(){
        NormalizedNodeMessages.Node.Builder nodeBuilder = NormalizedNodeMessages.Node.newBuilder();
        NormalizedNodeMessages.PathArgument.Builder pathBuilder = NormalizedNodeMessages.PathArgument.newBuilder();
        NormalizedNodeMessages.QName.Builder qNameBuilder = NormalizedNodeMessages.QName.newBuilder();

        qNameBuilder.setNamespace(0);
        qNameBuilder.setRevision(1);
        qNameBuilder.setLocalName(2);

        pathBuilder.setNodeType(qNameBuilder);
        pathBuilder.setIntType(PathArgumentType.NODE_IDENTIFIER_WITH_VALUE.ordinal());
        pathBuilder.addAttribute(
            NormalizedNodeMessages.PathArgumentAttribute.newBuilder()
                .setValue("foo").setType(ValueType.STRING_TYPE.ordinal()));

        nodeBuilder.addCode(TestModel.TEST_QNAME.getNamespace().toString());
        nodeBuilder.addCode(TestModel.TEST_QNAME.getFormattedRevision());
        nodeBuilder.addCode(TestModel.TEST_QNAME.getLocalName());

        PathArgument pathArgument = NormalizedNodeSerializer.deSerialize(nodeBuilder.build(), pathBuilder.build());

        assertEquals(new NodeWithValue<>(TestModel.TEST_QNAME, "foo"), pathArgument);

    }
    @Test
    public void testDeSerializeNodeIdentifierWithPredicates(){
        NormalizedNodeMessages.Node.Builder nodeBuilder = NormalizedNodeMessages.Node.newBuilder();
        NormalizedNodeMessages.PathArgument.Builder pathBuilder = NormalizedNodeMessages.PathArgument.newBuilder();
        NormalizedNodeMessages.QName.Builder qNameBuilder = NormalizedNodeMessages.QName.newBuilder();

        qNameBuilder.setNamespace(0);
        qNameBuilder.setRevision(1);
        qNameBuilder.setLocalName(2);

        pathBuilder.setNodeType(qNameBuilder);
        pathBuilder.setIntType(PathArgumentType.NODE_IDENTIFIER_WITH_PREDICATES.ordinal());
        pathBuilder.addAttribute(NormalizedNodeMessages.PathArgumentAttribute.newBuilder().setName(qNameBuilder).setValue(
            "foo").setType(ValueType.STRING_TYPE.ordinal()));

        nodeBuilder.addCode(TestModel.TEST_QNAME.getNamespace().toString());
        nodeBuilder.addCode(TestModel.TEST_QNAME.getFormattedRevision());
        nodeBuilder.addCode(TestModel.TEST_QNAME.getLocalName());

        PathArgument pathArgument = NormalizedNodeSerializer.deSerialize(nodeBuilder.build(), pathBuilder.build());

        assertEquals(new NodeIdentifierWithPredicates(TestModel.TEST_QNAME,
            ImmutableMap.<QName, Object>of(TestModel.TEST_QNAME, "foo")), pathArgument);

    }
    @Test
    public void testDeSerializeNodeAugmentationIdentifier(){
        NormalizedNodeMessages.Node.Builder nodeBuilder = NormalizedNodeMessages.Node.newBuilder();
        NormalizedNodeMessages.PathArgument.Builder pathBuilder = NormalizedNodeMessages.PathArgument.newBuilder();
        NormalizedNodeMessages.QName.Builder qNameBuilder = NormalizedNodeMessages.QName.newBuilder();

        qNameBuilder.setNamespace(0);
        qNameBuilder.setRevision(1);
        qNameBuilder.setLocalName(2);

        pathBuilder.setIntType(PathArgumentType.AUGMENTATION_IDENTIFIER.ordinal());
        pathBuilder.addAttribute(NormalizedNodeMessages.PathArgumentAttribute.newBuilder().setName(qNameBuilder).setType(ValueType.STRING_TYPE.ordinal()));

        nodeBuilder.addCode(TestModel.TEST_QNAME.getNamespace().toString());
        nodeBuilder.addCode(TestModel.TEST_QNAME.getFormattedRevision());
        nodeBuilder.addCode(TestModel.TEST_QNAME.getLocalName());

        PathArgument pathArgument = NormalizedNodeSerializer.deSerialize(nodeBuilder.build(), pathBuilder.build());

        assertEquals(new AugmentationIdentifier(ImmutableSet.of(TestModel.TEST_QNAME)), pathArgument);
    }
}
