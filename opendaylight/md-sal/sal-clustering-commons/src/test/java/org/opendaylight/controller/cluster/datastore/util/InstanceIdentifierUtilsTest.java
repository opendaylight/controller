/*
 * Copyright (c) 2014, 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.datastore.util;

import com.google.common.collect.ImmutableSet;
import org.junit.Assert;
import org.junit.Test;
import org.opendaylight.controller.cluster.datastore.node.utils.serialization.QNameDeSerializationContext;
import org.opendaylight.controller.cluster.datastore.node.utils.serialization.QNameDeSerializationContextImpl;
import org.opendaylight.controller.cluster.datastore.node.utils.serialization.QNameSerializationContextImpl;
import org.opendaylight.controller.protobuff.messages.common.NormalizedNodeMessages;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class InstanceIdentifierUtilsTest {

    private static final QName TEST_QNAME = QName
            .create("(urn:opendaylight:params:xml:ns:yang:controller:md:sal:dom:store:test?revision=2014-03-13)test");
    private static final QName NODE_WITH_VALUE_QNAME = QName
            .create("(urn:opendaylight:params:xml:ns:yang:controller:md:sal:dom:store:test?revision=2014-03-13)value");
    private static final QName NODE_WITH_PREDICATES_QNAME = QName
            .create("(urn:opendaylight:params:xml:ns:yang:controller:md:sal:dom:store:test?revision=2014-03-13)pred");
    private static final QName NAME_QNAME = QName
            .create("(urn:opendaylight:params:xml:ns:yang:controller:md:sal:dom:store:test?revision=2014-03-13)name");

    @Test
    public void testSerializationOfNodeIdentifier() {
        YangInstanceIdentifier.PathArgument p1 = new YangInstanceIdentifier.NodeIdentifier(TEST_QNAME);

        List<YangInstanceIdentifier.PathArgument> arguments = new ArrayList<>();

        arguments.add(p1);

        YangInstanceIdentifier expected = YangInstanceIdentifier.create(arguments);

        NormalizedNodeMessages.InstanceIdentifier instanceIdentifier =
                InstanceIdentifierUtils.toSerializable(expected);

        YangInstanceIdentifier actual = InstanceIdentifierUtils.fromSerializable(instanceIdentifier);

        Assert.assertEquals(expected.getLastPathArgument(), actual.getLastPathArgument());
    }

    @Test
    public void testSerializationOfNodeWithValue() {

        withValue((short) 1);
        withValue((long) 2);
        withValue(3);
        withValue(true);

    }

    private static void withValue(Object value) {
        YangInstanceIdentifier.PathArgument p1 = new YangInstanceIdentifier.NodeIdentifier(TEST_QNAME);

        YangInstanceIdentifier.PathArgument p2 =
                new YangInstanceIdentifier.NodeWithValue<>(NODE_WITH_VALUE_QNAME, value);

        List<YangInstanceIdentifier.PathArgument> arguments = new ArrayList<>();

        arguments.add(p1);
        arguments.add(p2);

        YangInstanceIdentifier expected = YangInstanceIdentifier.create(arguments);

        NormalizedNodeMessages.InstanceIdentifier instanceIdentifier =
                InstanceIdentifierUtils.toSerializable(expected);

        YangInstanceIdentifier actual = InstanceIdentifierUtils.fromSerializable(instanceIdentifier);

        Assert.assertEquals(expected.getLastPathArgument(), actual.getLastPathArgument());
    }

    @Test
    public void testSerializationOfNodeIdentifierWithPredicates() {

        withPredicates((short) 1);
        withPredicates((long) 2);
        withPredicates(3);
        withPredicates(true);

    }

    private static void withPredicates(Object value) {
        YangInstanceIdentifier.PathArgument p1 = new YangInstanceIdentifier.NodeIdentifier(TEST_QNAME);

        YangInstanceIdentifier.PathArgument p2 = new YangInstanceIdentifier.NodeIdentifierWithPredicates(
                NODE_WITH_PREDICATES_QNAME, NAME_QNAME, value);

        List<YangInstanceIdentifier.PathArgument> arguments = new ArrayList<>();

        arguments.add(p1);
        arguments.add(p2);

        YangInstanceIdentifier expected = YangInstanceIdentifier.create(arguments);

        NormalizedNodeMessages.InstanceIdentifier instanceIdentifier =
                InstanceIdentifierUtils.toSerializable(expected);

        YangInstanceIdentifier actual = InstanceIdentifierUtils.fromSerializable(instanceIdentifier);

        Assert.assertEquals(expected.getLastPathArgument(), actual.getLastPathArgument());
    }

    @Test
    public void testAugmentationIdentifier() {
        YangInstanceIdentifier.PathArgument p1 = new YangInstanceIdentifier.AugmentationIdentifier(
            ImmutableSet.of(TEST_QNAME));

        List<YangInstanceIdentifier.PathArgument> arguments = new ArrayList<>();

        arguments.add(p1);

        YangInstanceIdentifier expected = YangInstanceIdentifier.create(arguments);

        NormalizedNodeMessages.InstanceIdentifier instanceIdentifier =
                InstanceIdentifierUtils.toSerializable(expected);

        YangInstanceIdentifier actual = InstanceIdentifierUtils.fromSerializable(instanceIdentifier);

        Assert.assertEquals(expected.getLastPathArgument(), actual.getLastPathArgument());

    }

    @Test
    public void testSerializationWithContext() {
        List<YangInstanceIdentifier.PathArgument> arguments =
                                                Arrays.<YangInstanceIdentifier.PathArgument>asList(
                new YangInstanceIdentifier.NodeIdentifier(TEST_QNAME),
                new YangInstanceIdentifier.NodeWithValue<>(NODE_WITH_VALUE_QNAME, 1),
                new YangInstanceIdentifier.NodeIdentifierWithPredicates(
                        NODE_WITH_PREDICATES_QNAME, NAME_QNAME, 2));

        YangInstanceIdentifier expected = YangInstanceIdentifier.create(arguments);

        QNameSerializationContextImpl serializationContext = new QNameSerializationContextImpl();

        NormalizedNodeMessages.InstanceIdentifier instanceIdentifier =
                InstanceIdentifierUtils.toSerializable(expected, serializationContext);

        QNameDeSerializationContext deserializationContext = new QNameDeSerializationContextImpl(
                serializationContext.getCodes());

        YangInstanceIdentifier actual = InstanceIdentifierUtils.fromSerializable(
                instanceIdentifier, deserializationContext);

        Assert.assertEquals(expected.getLastPathArgument(), actual.getLastPathArgument());
    }
}
