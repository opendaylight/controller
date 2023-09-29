/*
 * Copyright (c) 2017 Pantheon Technologies s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.node.utils.stream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.xml.transform.dom.DOMSource;
import org.junit.Test;
import org.opendaylight.yangtools.util.xml.UntrustedXML;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifierWithPredicates;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeWithValue;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;
import org.opendaylight.yangtools.yang.data.api.schema.LeafNode;
import org.opendaylight.yangtools.yang.data.api.schema.LeafSetEntryNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.impl.schema.Builders;
import org.opendaylight.yangtools.yang.data.impl.schema.ImmutableNodes;
import org.xmlunit.builder.DiffBuilder;

public class SerializationUtilsTest {
    private static final QName CONTAINER1 = QName.create("ns-1", "2017-03-17", "container1");

    @Test
    public void testSerializeDeserializeNodes() throws Exception {
        final var normalizedNode = createNormalizedNode();
        final var bytes = serialize(normalizedNode);
        assertEquals(10556, bytes.length);
        assertEquals(normalizedNode, deserialize(bytes));
    }

    @Test
    public void testSerializeDeserializeAnyXmlNode() throws Exception {
        final var parse = UntrustedXML.newDocumentBuilder().parse(
            new ByteArrayInputStream("<xml><data/></xml>".getBytes(StandardCharsets.UTF_8)));
        final var anyXmlNode = Builders.anyXmlBuilder()
            .withNodeIdentifier(id("anyXmlNode"))
            .withValue(new DOMSource(parse))
            .build();
        final byte[] bytes = serialize(anyXmlNode);
        assertEquals(113, bytes.length);

        final var diff = DiffBuilder.compare(anyXmlNode.body().getNode())
            // FIXME: why all this magic?
            .withTest(((DOMSource) deserialize(bytes).body()).getNode().getOwnerDocument())
            .checkForSimilar()
            .build();
        assertFalse(diff.toString(), diff.hasDifferences());
    }

    @Test
    public void testSerializeDeserializePath() throws IOException {
        final var path = YangInstanceIdentifier.builder()
            .node(id("container1"))
            .node(listId("list1", "keyName1", "keyValue1"))
            .node(leafSetId("leafSer1", "leafSetValue1"))
            .build();

        final var bos = new ByteArrayOutputStream();
        try (var out = new DataOutputStream(bos)) {
            SerializationUtils.writePath(out, path);
        }

        final var bytes = bos.toByteArray();
        assertEquals(105, bytes.length);

        assertEquals(path, SerializationUtils.readPath(new DataInputStream(new ByteArrayInputStream(bytes))));
    }

    @Test
    public void testSerializeDeserializePathAndNode() throws IOException {
        final var path = YangInstanceIdentifier.of(id("container1"));
        final var node = createNormalizedNode();

        final var bos = new ByteArrayOutputStream();
        try (var out = new DataOutputStream(bos)) {
            SerializationUtils.writeNodeAndPath(out, path, node);
        }

        final byte[] bytes = bos.toByteArray();
        assertEquals(10558, bytes.length);

        final var applierCalled = new AtomicBoolean(false);
        try (var in = new DataInputStream(new ByteArrayInputStream(bytes))) {
            SerializationUtils.readNodeAndPath(in, applierCalled, (instance, deserializedPath, deserializedNode) -> {
                assertEquals(path, deserializedPath);
                assertEquals(node, deserializedNode);
                applierCalled.set(true);
            });
        }
        assertTrue(applierCalled.get());
    }

    private static NormalizedNode deserialize(final byte[] bytes) throws Exception {
        return SerializationUtils.readNormalizedNode(new DataInputStream(new ByteArrayInputStream(bytes)))
            .orElseThrow();
    }

    private static byte[] serialize(final NormalizedNode node) throws Exception {
        final var bos = new ByteArrayOutputStream();
        SerializationUtils.writeNormalizedNode(new DataOutputStream(bos), node);
        return bos.toByteArray();
    }

    private static ContainerNode createNormalizedNode() {
        final var stringLeaf = createLeaf("stringLeaf", "stringValue");
        final var entry1 = Builders.mapEntryBuilder()
            .withNodeIdentifier(listId("mapNode", "key", "key1"))
            .withChild(stringLeaf)
            .build();
        final var entry2 = Builders.mapEntryBuilder()
            .withNodeIdentifier(listId("mapNode", "key", "key2"))
            .withChild(stringLeaf)
            .build();

        return Builders.containerBuilder()
                .withNodeIdentifier(new NodeIdentifier(CONTAINER1))
                .withChild(createLeaf("booleanLeaf", true))
                .withChild(createLeaf("byteLeaf", (byte) 0))
                .withChild(createLeaf("shortLeaf", (short) 55))
                .withChild(createLeaf("intLeaf", 11))
                .withChild(createLeaf("longLeaf", 151515L))
                .withChild(stringLeaf)
                .withChild(createLeaf("longStringLeaf", "0123456789".repeat(1000)))
                .withChild(createLeaf("stringLeaf", QName.create("base", "qName")))
                .withChild(createLeaf("stringLeaf", YangInstanceIdentifier.of()))
                .withChild(Builders.mapBuilder()
                    .withNodeIdentifier(id("mapNode"))
                    .withChild(entry1)
                    .withChild(entry2)
                    .build())
                .withChild(Builders.orderedMapBuilder()
                    .withNodeIdentifier(id("orderedMapNode"))
                    .withChild(entry2)
                    .withChild(entry1)
                    .build())
                .withChild(Builders.unkeyedListBuilder()
                    .withNodeIdentifier(id("unkeyedList"))
                    .withChild(Builders.unkeyedListEntryBuilder()
                        .withNodeIdentifier(id("unkeyedList"))
                        .withChild(stringLeaf)
                        .build())
                    .withChild(Builders.unkeyedListEntryBuilder()
                        .withNodeIdentifier(id("unkeyedList"))
                        .withChild(stringLeaf)
                        .build())
                    .build())
                .withChild(Builders.leafSetBuilder()
                    .withNodeIdentifier(id("leafSetNode"))
                    .withChild(createLeafSetEntry("leafSetNode", "leafSetValue1"))
                    .withChild(createLeafSetEntry("leafSetNode", "leafSetValue2"))
                    .build())
                .withChild(Builders.orderedLeafSetBuilder()
                    .withNodeIdentifier(id("orderedLeafSetNode"))
                    .withChild(createLeafSetEntry("orderedLeafSetNode", "value1"))
                    .withChild(createLeafSetEntry("orderedLeafSetNode", "value2"))
                    .build())
                .withChild(createLeaf("aug1", "aug1Value"))
                .withChild(createLeaf("aug2", "aug2Value"))
                .withChild(Builders.choiceBuilder()
                    .withNodeIdentifier(id("choiceNode"))
                    .withChild(createLeaf("choiceLeaf", 12))
                    .build())
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

    private static NodeIdentifier id(final String name) {
        return new NodeIdentifier(QName.create(CONTAINER1, name));
    }

    private static NodeIdentifierWithPredicates listId(final String listName, final String keyName,
            final Object keyValue) {
        return NodeIdentifierWithPredicates.of(QName.create(CONTAINER1, listName), QName.create(CONTAINER1, keyName),
            keyValue);
    }

    private static <T> NodeWithValue<T> leafSetId(final String node, final T value) {
        return new NodeWithValue<>(QName.create(CONTAINER1, node), value);
    }
}
