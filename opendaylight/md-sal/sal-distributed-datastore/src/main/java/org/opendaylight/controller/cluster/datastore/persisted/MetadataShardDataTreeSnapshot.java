/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.persisted;

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.Objects.requireNonNull;

import com.google.common.annotations.Beta;
import com.google.common.base.MoreObjects;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.io.Serializable;
import java.io.StreamCorruptedException;
import java.util.Map;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.codec.binfmt.NormalizedNodeDataInput;
import org.opendaylight.yangtools.yang.data.codec.binfmt.NormalizedNodeDataOutput;
import org.opendaylight.yangtools.yang.data.codec.binfmt.NormalizedNodeStreamVersion;
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
        private NormalizedNodeStreamVersion version;
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
            this.version = snapshot.version().getStreamVersion();
        }

        @Override
        public void writeExternal(final ObjectOutput out) throws IOException {
            out.writeInt(metadata.size());
            for (ShardDataTreeSnapshotMetadata<?> m : metadata.values()) {
                out.writeObject(m);
            }
            out.writeBoolean(true);
            try (NormalizedNodeDataOutput stream = version.newDataOutput(out)) {
                stream.writeNormalizedNode(rootNode);
            }
        }

        @Override
        public void readExternal(final ObjectInput in) throws IOException, ClassNotFoundException {
            final int metaSize = in.readInt();
            checkArgument(metaSize >= 0, "Invalid negative metadata map length %s", metaSize);

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
            final boolean present = in.readBoolean();
            if (!present) {
                throw new StreamCorruptedException("Unexpected missing root node");
            }

            final NormalizedNodeDataInput stream = NormalizedNodeDataInput.newDataInput(in);
            version = stream.getVersion();
            rootNode = stream.readNormalizedNode();
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
        this.rootNode = requireNonNull(rootNode);
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
        return PayloadVersion.MAGNESIUM;
    }

    private Object writeReplace() {
        return new Proxy(this);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this).add("metadata", metadata).toString();
    }
}
