/*
 *
 *  Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 */

package org.opendaylight.controller.cluster.datastore.util;

import org.junit.Assert;
import org.junit.Test;
import org.opendaylight.controller.cluster.datastore.node.utils.serialization.NormalizedNodeDeSerializationContext;
import org.opendaylight.controller.cluster.datastore.node.utils.serialization.NormalizedNodeSerializationContext;
import org.opendaylight.controller.protobuff.messages.common.NormalizedNodeMessages;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.common.SimpleDateFormatUtil;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class InstanceIdentifierUtilsTest {

    private static QName TEST_QNAME = QName
            .create("(urn:opendaylight:params:xml:ns:yang:controller:md:sal:dom:store:test?revision=2014-03-13)test");
    private static QName NODE_WITH_VALUE_QNAME = QName
            .create("(urn:opendaylight:params:xml:ns:yang:controller:md:sal:dom:store:test?revision=2014-03-13)value");
    private static QName NODE_WITH_PREDICATES_QNAME = QName
            .create("(urn:opendaylight:params:xml:ns:yang:controller:md:sal:dom:store:test?revision=2014-03-13)pred");
    private static QName NAME_QNAME = QName
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

    private void withValue(Object value) {
        YangInstanceIdentifier.PathArgument p1 = new YangInstanceIdentifier.NodeIdentifier(TEST_QNAME);

        YangInstanceIdentifier.PathArgument p2 =
                new YangInstanceIdentifier.NodeWithValue(NODE_WITH_VALUE_QNAME, value);

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

    private void withPredicates(Object value) {
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
        YangInstanceIdentifier.PathArgument p1 = new YangInstanceIdentifier.AugmentationIdentifier(new HashSet(
                Arrays.asList(TEST_QNAME)));

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
                new YangInstanceIdentifier.NodeWithValue(NODE_WITH_VALUE_QNAME, 1),
                new YangInstanceIdentifier.NodeIdentifierWithPredicates(
                        NODE_WITH_PREDICATES_QNAME, NAME_QNAME, 2));

        YangInstanceIdentifier expected = YangInstanceIdentifier.create(arguments);

        final BiMap<String, Integer> codeMap = HashBiMap.create();
        final AtomicInteger counter = new AtomicInteger();
        NormalizedNodeSerializationContext serializationContext = new NormalizedNodeSerializationContext() {
            @Override
            public int addNamespace(URI namespace) {
                return addTo(namespace.toString());
            }

            @Override
            public int addRevision(Date revision) {
                return addTo(SimpleDateFormatUtil.getRevisionFormat().format(revision));
            }

            @Override
            public int addLocalName(String localName) {
                return addTo(localName);
            }

            private int addTo(String s) {
                Integer i = codeMap.get(s);
                if(i != null) {
                    return i;
                }

                i = counter.getAndIncrement();
                codeMap.put(s, i);
                return i;
            }
        };

        NormalizedNodeDeSerializationContext deserializationContext =
                                                      new NormalizedNodeDeSerializationContext() {
            @Override
            public String getNamespace(int namespace) {
                return codeMap.inverse().get(namespace);
            }

            @Override
            public String getRevision(int revision) {
                return codeMap.inverse().get(revision);
            }

            @Override
            public String getLocalName(int localName) {
                return codeMap.inverse().get(localName);
            }
        };

        NormalizedNodeMessages.InstanceIdentifier instanceIdentifier =
                InstanceIdentifierUtils.toSerializable(expected, serializationContext);

        YangInstanceIdentifier actual = InstanceIdentifierUtils.fromSerializable(
                instanceIdentifier, deserializationContext);

        Assert.assertEquals(expected.getLastPathArgument(), actual.getLastPathArgument());
    }
}
