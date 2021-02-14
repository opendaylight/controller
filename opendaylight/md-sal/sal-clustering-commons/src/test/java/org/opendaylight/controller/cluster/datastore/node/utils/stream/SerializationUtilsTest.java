/*
 * Copyright (c) 2017 Pantheon Technologies s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.node.utils.stream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.google.common.collect.ImmutableSet;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import javax.xml.transform.dom.DOMSource;
import org.custommonkey.xmlunit.Diff;
import org.custommonkey.xmlunit.XMLUnit;
import org.junit.Test;
import org.opendaylight.yangtools.util.xml.UntrustedXML;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.AugmentationIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.AugmentationNode;
import org.opendaylight.yangtools.yang.data.api.schema.ChoiceNode;
import org.opendaylight.yangtools.yang.data.api.schema.DOMSourceAnyxmlNode;
import org.opendaylight.yangtools.yang.data.api.schema.LeafNode;
import org.opendaylight.yangtools.yang.data.api.schema.LeafSetEntryNode;
import org.opendaylight.yangtools.yang.data.api.schema.LeafSetNode;
import org.opendaylight.yangtools.yang.data.api.schema.MapEntryNode;
import org.opendaylight.yangtools.yang.data.api.schema.MapNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.api.schema.UnkeyedListEntryNode;
import org.opendaylight.yangtools.yang.data.api.schema.UnkeyedListNode;
import org.opendaylight.yangtools.yang.data.api.schema.UserMapNode;
import org.opendaylight.yangtools.yang.data.impl.schema.Builders;
import org.opendaylight.yangtools.yang.data.impl.schema.ImmutableNodes;
import org.w3c.dom.Document;

public class SerializationUtilsTest {

    private static final QName CONTAINER_Q_NAME = QName.create("ns-1", "2017-03-17", "container1");

    @Test
    public void testSerializeDeserializeNodes() throws IOException {
        final NormalizedNode normalizedNode = createNormalizedNode();
        final byte[] bytes = serializeNormalizedNode(normalizedNode);
        assertEquals(10564, bytes.length);
        assertEquals(normalizedNode, deserializeNormalizedNode(bytes));
    }

    @Test
    public void testSerializeDeserializeAnyXmlNode() throws Exception {
        final ByteArrayInputStream is =
                new ByteArrayInputStream("<xml><data/></xml>".getBytes(Charset.defaultCharset()));
        final Document parse = UntrustedXML.newDocumentBuilder().parse(is);
        final DOMSourceAnyxmlNode anyXmlNode = Builders.anyXmlBuilder()
                  .withNodeIdentifier(id("anyXmlNode"))
                .withValue(new DOMSource(parse))
                .build();
        final byte[] bytes = serializeNormalizedNode(anyXmlNode);
        assertEquals(113, bytes.length);
        final NormalizedNode deserialized = deserializeNormalizedNode(bytes);
        final DOMSource value = (DOMSource) deserialized.body();
        final Diff diff = XMLUnit.compareXML((Document) anyXmlNode.body().getNode(),
                value.getNode().getOwnerDocument());
        assertTrue(diff.toString(), diff.similar());
    }

    @Test
    public void testSerializeDeserializePath() throws IOException {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final DataOutput out = new DataOutputStream(bos);
        final YangInstanceIdentifier path = YangInstanceIdentifier.builder()
                .node(id("container1"))
                .node(autmentationId("list1", "list2"))
                .node(listId("list1", "keyName1", "keyValue1"))
                .node(leafSetId("leafSer1", "leafSetValue1"))
                .build();
        SerializationUtils.writePath(out, path);

        final byte[] bytes = bos.toByteArray();
        assertEquals(119, bytes.length);

        final YangInstanceIdentifier deserialized =
                SerializationUtils.readPath(new DataInputStream(new ByteArrayInputStream(bytes)));
        assertEquals(path, deserialized);
    }

    @Test
    public void testSerializeDeserializePathAndNode() throws IOException {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final DataOutput out = new DataOutputStream(bos);
        final NormalizedNode node = createNormalizedNode();
        final YangInstanceIdentifier path = YangInstanceIdentifier.create(id("container1"));
        SerializationUtils.writeNodeAndPath(out, path, node);

        final byte[] bytes = bos.toByteArray();
        assertEquals(10566, bytes.length);

        final DataInputStream in = new DataInputStream(new ByteArrayInputStream(bytes));
        final AtomicBoolean applierCalled = new AtomicBoolean(false);
        SerializationUtils.readNodeAndPath(in, applierCalled, (instance, deserializedPath, deserializedNode) -> {
            assertEquals(path, deserializedPath);
            assertEquals(node, deserializedNode);
            applierCalled.set(true);
        });
        assertTrue(applierCalled.get());
    }

    @Test
    public void testSerializeDeserializeAugmentNoref() throws IOException {
        final YangInstanceIdentifier expected = YangInstanceIdentifier.create(
            AugmentationIdentifier.create(ImmutableSet.of(
                QName.create("foo", "leaf1"),
                QName.create("bar", "leaf2"))));

        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final DataOutput out = new DataOutputStream(bos);
        SerializationUtils.writePath(out, expected);

        final byte[] bytes = bos.toByteArray();
        assertEquals(37, bytes.length);

        final DataInputStream in = new DataInputStream(new ByteArrayInputStream(bytes));
        final YangInstanceIdentifier read = SerializationUtils.readPath(in);
        assertEquals(expected, read);
    }

    private static NormalizedNode deserializeNormalizedNode(final byte[] bytes) throws IOException {
        return SerializationUtils.readNormalizedNode(new DataInputStream(new ByteArrayInputStream(bytes))).get();
    }

    private static byte[] serializeNormalizedNode(final NormalizedNode node) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        SerializationUtils.writeNormalizedNode(new DataOutputStream(bos), node);
        return bos.toByteArray();
    }

    private static NormalizedNode createNormalizedNode() {
        final LeafSetNode<Object> leafSetNode = Builders.leafSetBuilder()
                .withNodeIdentifier(id("leafSetNode"))
                .withChild(createLeafSetEntry("leafSetNode", "leafSetValue1"))
                .withChild(createLeafSetEntry("leafSetNode", "leafSetValue2"))
                .build();
        final LeafSetNode<Object> orderedLeafSetNode = Builders.orderedLeafSetBuilder()
                .withNodeIdentifier(id("orderedLeafSetNode"))
                .withChild(createLeafSetEntry("orderedLeafSetNode", "value1"))
                .withChild(createLeafSetEntry("orderedLeafSetNode", "value2"))
                .build();
        final LeafNode<Boolean> booleanLeaf = createLeaf("booleanLeaf", true);
        final LeafNode<Byte> byteLeaf = createLeaf("byteLeaf", (byte) 0);
        final LeafNode<Short> shortLeaf = createLeaf("shortLeaf", (short) 55);
        final LeafNode<Integer> intLeaf = createLeaf("intLeaf", 11);
        final LeafNode<Long> longLeaf = createLeaf("longLeaf", 151515L);
        final LeafNode<String> stringLeaf = createLeaf("stringLeaf", "stringValue");
        final LeafNode<String> longStringLeaf = createLeaf("longStringLeaf", getLongString());
        final LeafNode<QName> qNameLeaf = createLeaf("stringLeaf", QName.create("base", "qName"));
        final LeafNode<YangInstanceIdentifier> idLeaf = createLeaf("stringLeaf", YangInstanceIdentifier.empty());
        final MapEntryNode entry1 = Builders.mapEntryBuilder()
                .withNodeIdentifier(listId("mapNode", "key", "key1"))
                .withChild(stringLeaf)
                .build();
        final MapEntryNode entry2 = Builders.mapEntryBuilder()
                .withNodeIdentifier(listId("mapNode", "key", "key2"))
                .withChild(stringLeaf)
                .build();
        final MapNode mapNode = Builders.mapBuilder()
                .withNodeIdentifier(id("mapNode"))
                .withChild(entry1)
                .withChild(entry2)
                .build();
        final UserMapNode orderedMapNode = Builders.orderedMapBuilder()
                .withNodeIdentifier(id("orderedMapNode"))
                .withChild(entry2)
                .withChild(entry1)
                .build();
        final UnkeyedListEntryNode unkeyedListEntry1 = Builders.unkeyedListEntryBuilder()
                .withNodeIdentifier(id("unkeyedList"))
                .withChild(stringLeaf)
                .build();
        final UnkeyedListEntryNode unkeyedListEntry2 = Builders.unkeyedListEntryBuilder()
                .withNodeIdentifier(id("unkeyedList"))
                .withChild(stringLeaf)
                .build();
        final UnkeyedListNode unkeyedListNode = Builders.unkeyedListBuilder()
                .withNodeIdentifier(id("unkeyedList"))
                .withChild(unkeyedListEntry1)
                .withChild(unkeyedListEntry2)
                .build();
        final ImmutableSet<QName> childNames =
                ImmutableSet.of(QName.create(CONTAINER_Q_NAME, "aug1"), QName.create(CONTAINER_Q_NAME, "aug1"));
        final AugmentationNode augmentationNode = Builders.augmentationBuilder()
                .withNodeIdentifier(new YangInstanceIdentifier.AugmentationIdentifier(childNames))
                .withChild(createLeaf("aug1", "aug1Value"))
                .withChild(createLeaf("aug2", "aug2Value"))
                .build();
        final ChoiceNode choiceNode = Builders.choiceBuilder()
                .withNodeIdentifier(id("choiceNode"))
                .withChild(createLeaf("choiceLeaf", 12))
                .build();
        return Builders.containerBuilder()
                .withNodeIdentifier(new YangInstanceIdentifier.NodeIdentifier(CONTAINER_Q_NAME))
                .withChild(booleanLeaf)
                .withChild(byteLeaf)
                .withChild(shortLeaf)
                .withChild(intLeaf)
                .withChild(longLeaf)
                .withChild(stringLeaf)
                .withChild(longStringLeaf)
                .withChild(qNameLeaf)
                .withChild(idLeaf)
                .withChild(mapNode)
                .withChild(orderedMapNode)
                .withChild(unkeyedListNode)
                .withChild(leafSetNode)
                .withChild(orderedLeafSetNode)
                .withChild(augmentationNode)
                .withChild(choiceNode)
                .build();
    }

    private static <T> LeafNode<T> createLeaf(final String name, final T value) {
        return ImmutableNodes.leafNode(id(name), value);
    }

    private static LeafSetEntryNode<Object> createLeafSetEntry(final String leafSet, final String value) {
        return Builders.leafSetEntryBuilder()
                .withNodeIdentifier(leafSetId(leafSet, value))
                .withValue(value)
                .build();
    }

    private static YangInstanceIdentifier.NodeIdentifier id(final String name) {
        return new YangInstanceIdentifier.NodeIdentifier(QName.create(CONTAINER_Q_NAME, name));
    }

    private static YangInstanceIdentifier.NodeIdentifierWithPredicates listId(final String listName,
                                                                              final String keyName,
                                                                              final Object keyValue) {
        return YangInstanceIdentifier.NodeIdentifierWithPredicates.of(QName.create(CONTAINER_Q_NAME, listName),
                QName.create(CONTAINER_Q_NAME, keyName), keyValue);
    }

    private static <T> YangInstanceIdentifier.NodeWithValue<T> leafSetId(final String node, final T value) {
        return new YangInstanceIdentifier.NodeWithValue<>(QName.create(CONTAINER_Q_NAME, node), value);
    }

    private static YangInstanceIdentifier.AugmentationIdentifier autmentationId(final String... nodes) {
        final Set<QName> qNames = Arrays.stream(nodes)
                .map(node -> QName.create(CONTAINER_Q_NAME, node))
                .collect(Collectors.toSet());
        return new YangInstanceIdentifier.AugmentationIdentifier(qNames);
    }

    private static String getLongString() {
        final StringBuilder builder = new StringBuilder(10000);
        for (int i = 0; i < 1000; i++) {
            builder.append("0123456789");
        }
        return builder.toString();
    }
}
