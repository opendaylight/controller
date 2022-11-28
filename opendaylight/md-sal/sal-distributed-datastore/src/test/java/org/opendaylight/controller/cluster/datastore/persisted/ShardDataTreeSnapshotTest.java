/*
 * Copyright (c) 2016, 2017 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.persisted;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.util.Map;
import java.util.Optional;
import org.junit.Test;
import org.opendaylight.controller.md.cluster.datastore.model.TestModel;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.impl.schema.Builders;
import org.opendaylight.yangtools.yang.data.impl.schema.ImmutableNodes;

/**
 * Unit tests for ShardDataTreeSnapshot.
 *
 * @author Thomas Pantelis
 */
public class ShardDataTreeSnapshotTest {

    @Test
    public void testShardDataTreeSnapshotWithNoMetadata() throws Exception {
        ContainerNode expectedNode = Builders.containerBuilder()
                .withNodeIdentifier(new NodeIdentifier(TestModel.TEST_QNAME))
                .withChild(ImmutableNodes.leafNode(TestModel.DESC_QNAME, "foo")).build();

        MetadataShardDataTreeSnapshot snapshot = new MetadataShardDataTreeSnapshot(expectedNode);
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try (ObjectOutputStream out = new ObjectOutputStream(bos)) {
            snapshot.serialize(out);
        }

        final byte[] bytes = bos.toByteArray();
        assertEquals(236, bytes.length);

        ShardDataTreeSnapshot deserialized;
        try (ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(bytes))) {
            deserialized = ShardDataTreeSnapshot.deserialize(in).getSnapshot();
        }

        Optional<NormalizedNode> actualNode = deserialized.getRootNode();
        assertTrue("rootNode present", actualNode.isPresent());
        assertEquals("rootNode", expectedNode, actualNode.get());
        assertEquals("Deserialized type", MetadataShardDataTreeSnapshot.class, deserialized.getClass());
        assertEquals("Metadata size", 0, ((MetadataShardDataTreeSnapshot)deserialized).getMetadata().size());
    }

    @Test
    public void testShardDataTreeSnapshotWithMetadata() throws Exception {
        ContainerNode expectedNode = Builders.containerBuilder()
                .withNodeIdentifier(new NodeIdentifier(TestModel.TEST_QNAME))
                .withChild(ImmutableNodes.leafNode(TestModel.DESC_QNAME, "foo")).build();

        Map<Class<? extends ShardDataTreeSnapshotMetadata<?>>, ShardDataTreeSnapshotMetadata<?>> expMetadata =
                Map.of(TestShardDataTreeSnapshotMetadata.class, new TestShardDataTreeSnapshotMetadata("test"));
        MetadataShardDataTreeSnapshot snapshot = new MetadataShardDataTreeSnapshot(expectedNode, expMetadata);
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try (ObjectOutputStream out = new ObjectOutputStream(bos)) {
            snapshot.serialize(out);
        }

        final byte[] bytes = bos.toByteArray();
        assertEquals(384, bytes.length);

        ShardDataTreeSnapshot deserialized;
        try (ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(bytes))) {
            deserialized = ShardDataTreeSnapshot.deserialize(in).getSnapshot();
        }

        Optional<NormalizedNode> actualNode = deserialized.getRootNode();
        assertTrue("rootNode present", actualNode.isPresent());
        assertEquals("rootNode", expectedNode, actualNode.get());
        assertEquals("Deserialized type", MetadataShardDataTreeSnapshot.class, deserialized.getClass());
        assertEquals("Metadata", expMetadata, ((MetadataShardDataTreeSnapshot)deserialized).getMetadata());
    }

    static class TestShardDataTreeSnapshotMetadata
            extends ShardDataTreeSnapshotMetadata<TestShardDataTreeSnapshotMetadata> {
        private static final long serialVersionUID = 1L;

        private final String data;

        TestShardDataTreeSnapshotMetadata(final String data) {
            this.data = data;
        }

        @Override
        public Class<TestShardDataTreeSnapshotMetadata> getType() {
            return TestShardDataTreeSnapshotMetadata.class;
        }

        @Override
        protected Externalizable externalizableProxy() {
            return new Proxy(data);
        }

        @Override
        public int hashCode() {
            return data.hashCode();
        }

        @Override
        public boolean equals(final Object obj) {
            return obj instanceof TestShardDataTreeSnapshotMetadata
                    && data.equals(((TestShardDataTreeSnapshotMetadata)obj).data);
        }

        private static class Proxy implements Externalizable {
            @java.io.Serial
            private static final long serialVersionUID = 7534948936595056176L;

            private String data;

            @SuppressWarnings("checkstyle:RedundantModifier")
            public Proxy() {
            }

            Proxy(final String data) {
                this.data = data;
            }

            @Override
            public void writeExternal(final ObjectOutput out) throws IOException {
                out.writeObject(data);
            }

            @Override
            public void readExternal(final ObjectInput in) throws IOException, ClassNotFoundException {
                data = (String) in.readObject();
            }

            Object readResolve() {
                return new TestShardDataTreeSnapshotMetadata(data);
            }
        }
    }
}
