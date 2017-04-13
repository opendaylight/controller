/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.persisted;

import com.google.common.annotations.Beta;
import com.google.common.base.MoreObjects;
import com.google.common.base.Preconditions;
import com.google.common.base.Verify;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.io.Serializable;
import java.util.Map;
import org.opendaylight.controller.cluster.datastore.node.utils.stream.SerializationUtils;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An {@link AbstractVersionedShardDataTreeSnapshot} which contains additional metadata.
 *
 * @author Robert Varga
 */
@Beta
public final class MetadataShardDataTreeSnapshot extends AbstractVersionedShardDataTreeSnapshot
        implements Serializable {
    private static final class Proxy implements Externalizable {
        private static final long serialVersionUID = 1L;
        private static final Logger LOG = LoggerFactory.getLogger(MetadataShardDataTreeSnapshot.class);

        private Map<Class<? extends ShardDataTreeSnapshotMetadata<?>>, ShardDataTreeSnapshotMetadata<?>> metadata;
        private NormalizedNode<?, ?> rootNode;

        // checkstyle flags the public modifier as redundant which really doesn't make sense since it clearly isn't
        // redundant. It is explicitly needed for Java serialization to be able to create instances via reflection.
        @SuppressWarnings("checkstyle:RedundantModifier")
        public Proxy() {
            // For Externalizable
        }

        Proxy(final MetadataShardDataTreeSnapshot snapshot) {
            this.rootNode = snapshot.getRootNode().get();
            this.metadata = snapshot.getMetadata();
        }

        @Override
        public void writeExternal(final ObjectOutput out) throws IOException {
            out.writeInt(metadata.size());
            for (ShardDataTreeSnapshotMetadata<?> m : metadata.values()) {
                out.writeObject(m);
            }

            SerializationUtils.serializeNormalizedNode(rootNode, out);
        }

        @Override
        public void readExternal(final ObjectInput in) throws IOException, ClassNotFoundException {
            final int metaSize = in.readInt();
            Preconditions.checkArgument(metaSize >= 0, "Invalid negative metadata map length %s", metaSize);

            // Default pre-allocate is 4, which should be fine
            final Builder<Class<? extends ShardDataTreeSnapshotMetadata<?>>, ShardDataTreeSnapshotMetadata<?>>
                    metaBuilder = ImmutableMap.builder();
            for (int i = 0; i < metaSize; ++i) {
                final ShardDataTreeSnapshotMetadata<?> m = (ShardDataTreeSnapshotMetadata<?>) in.readObject();
                if (m != null) {
                    metaBuilder.put(m.getType(), m);
                } else {
                    LOG.warn("Skipping null metadata");
                }
            }

            metadata = metaBuilder.build();
            rootNode = Verify.verifyNotNull(SerializationUtils.deserializeNormalizedNode(in));
        }

        private Object readResolve() {
            return new MetadataShardDataTreeSnapshot(rootNode, metadata);
        }
    }

    private static final long serialVersionUID = 1L;

    @SuppressFBWarnings(value = "SE_BAD_FIELD", justification = "This field is not Serializable but this class "
            + "implements writeReplace to delegate serialization to a Proxy class and thus instances of this class "
            + "aren't serialized. FindBugs does not recognize this.")
    private final Map<Class<? extends ShardDataTreeSnapshotMetadata<?>>, ShardDataTreeSnapshotMetadata<?>> metadata;

    @SuppressFBWarnings(value = "SE_BAD_FIELD", justification = "See above justification.")
    private final NormalizedNode<?, ?> rootNode;

    public MetadataShardDataTreeSnapshot(final NormalizedNode<?, ?> rootNode) {
        this(rootNode, ImmutableMap.of());
    }

    public MetadataShardDataTreeSnapshot(final NormalizedNode<?, ?> rootNode,
            final Map<Class<? extends ShardDataTreeSnapshotMetadata<?>>, ShardDataTreeSnapshotMetadata<?>> metadata) {
        this.rootNode = Preconditions.checkNotNull(rootNode);
        this.metadata = ImmutableMap.copyOf(metadata);
    }

    public Map<Class<? extends ShardDataTreeSnapshotMetadata<?>>, ShardDataTreeSnapshotMetadata<?>> getMetadata() {
        return metadata;
    }

    @Override
    NormalizedNode<?, ?> rootNode() {
        return rootNode;
    }

    @Override
    PayloadVersion version() {
        return PayloadVersion.BORON;
    }

    private Object writeReplace() {
        return new Proxy(this);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this).add("metadata", metadata).toString();
    }
}
