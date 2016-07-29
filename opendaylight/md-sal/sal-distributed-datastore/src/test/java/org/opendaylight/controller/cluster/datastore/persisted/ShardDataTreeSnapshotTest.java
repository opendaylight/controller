/*
 * Copyright (c) 2016 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.persisted;

import static org.junit.Assert.assertEquals;
import com.google.common.collect.ImmutableMap;
import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.Map;
import java.util.Optional;
import org.junit.Test;
import org.opendaylight.controller.md.cluster.datastore.model.TestModel;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.impl.schema.ImmutableNodes;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.impl.ImmutableContainerNodeBuilder;

/**
 * Unit tests for ShardDataTreeSnapshot.
 *
 * @author Thomas Pantelis
 */
public class ShardDataTreeSnapshotTest {

    @Test
    public void testShardDataTreeSnapshotWithNoMetadata() throws Exception {
        NormalizedNode<?, ?> expectedNode = ImmutableContainerNodeBuilder.create().withNodeIdentifier(
                new YangInstanceIdentifier.NodeIdentifier(TestModel.TEST_QNAME)).
                withChild(ImmutableNodes.leafNode(TestModel.DESC_QNAME, "foo")).build();

        MetadataShardDataTreeSnapshot snapshot = new MetadataShardDataTreeSnapshot(expectedNode);
        byte[] serialized = snapshot.serialize();

        ShardDataTreeSnapshot deserialized = ShardDataTreeSnapshot.deserialize(serialized);

        Optional<NormalizedNode<?, ?>> actualNode = deserialized.getRootNode();
        assertEquals("rootNode present", true, actualNode.isPresent());
        assertEquals("rootNode", expectedNode, actualNode.get());
        assertEquals("Deserialized type", MetadataShardDataTreeSnapshot.class, deserialized.getClass());
        assertEquals("Metadata size", 0, ((MetadataShardDataTreeSnapshot)deserialized).getMetadata().size());
    }

    @Test
    public void testShardDataTreeSnapshotWithMetadata() throws Exception {
        NormalizedNode<?, ?> expectedNode = ImmutableContainerNodeBuilder.create().withNodeIdentifier(
                new YangInstanceIdentifier.NodeIdentifier(TestModel.TEST_QNAME)).
                withChild(ImmutableNodes.leafNode(TestModel.DESC_QNAME, "foo")).build();

        Map<Class<? extends ShardDataTreeSnapshotMetadata>, ShardDataTreeSnapshotMetadata> expMetadata =
                ImmutableMap.of(TestShardDataTreeSnapshotMetadata.class, new TestShardDataTreeSnapshotMetadata("test"));
        MetadataShardDataTreeSnapshot snapshot = new MetadataShardDataTreeSnapshot(expectedNode, expMetadata);
        byte[] serialized = snapshot.serialize();

        ShardDataTreeSnapshot deserialized = ShardDataTreeSnapshot.deserialize(serialized);

        Optional<NormalizedNode<?, ?>> actualNode = deserialized.getRootNode();
        assertEquals("rootNode present", true, actualNode.isPresent());
        assertEquals("rootNode", expectedNode, actualNode.get());
        assertEquals("Deserialized type", MetadataShardDataTreeSnapshot.class, deserialized.getClass());
        assertEquals("Metadata", expMetadata, ((MetadataShardDataTreeSnapshot)deserialized).getMetadata());
    }

    @Test
    public void testPreBoronShardDataTreeSnapshot() throws Exception {
        NormalizedNode<?, ?> expectedNode = ImmutableContainerNodeBuilder.create().withNodeIdentifier(
                new YangInstanceIdentifier.NodeIdentifier(TestModel.TEST_QNAME)).
                withChild(ImmutableNodes.leafNode(TestModel.DESC_QNAME, "foo")).build();

        PreBoronShardDataTreeSnapshot snapshot = new PreBoronShardDataTreeSnapshot(expectedNode);
        byte[] serialized = snapshot.serialize();

        ShardDataTreeSnapshot deserialized = ShardDataTreeSnapshot.deserialize(serialized);

        Optional<NormalizedNode<?, ?>> actualNode = deserialized.getRootNode();
        assertEquals("rootNode present", true, actualNode.isPresent());
        assertEquals("rootNode", expectedNode, actualNode.get());
        assertEquals("Deserialized type", PreBoronShardDataTreeSnapshot.class, deserialized.getClass());
    }

    static class TestShardDataTreeSnapshotMetadata extends ShardDataTreeSnapshotMetadata {
        private static final long serialVersionUID = 1L;

        private final String data;

        TestShardDataTreeSnapshotMetadata(String data) {
            this.data = data;
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
        public boolean equals(Object obj) {
            return data.equals(((TestShardDataTreeSnapshotMetadata)obj).data);
        }


        private static class Proxy implements Externalizable {
            private String data;

            public Proxy() {
            }

            Proxy(String data) {
                this.data = data;
            }

            @Override
            public void writeExternal(ObjectOutput out) throws IOException {
                out.writeObject(data);
            }

            @Override
            public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
                data = (String) in.readObject();
            }

            Object readResolve() {
                return new TestShardDataTreeSnapshotMetadata(data);
            }
        }
    }
}
