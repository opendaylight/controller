/*
 * Copyright (c) 2022 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.persisted;

import static com.google.common.base.Preconditions.checkArgument;

import com.google.common.collect.ImmutableMap;
import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.Map;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.codec.binfmt.NormalizedNodeDataInput;
import org.opendaylight.yangtools.yang.data.codec.binfmt.NormalizedNodeStreamVersion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Externalizable proxy for {@link MetadataShardDataTreeSnapshot}.
 */
final class MS implements Externalizable {
    private static final Logger LOG = LoggerFactory.getLogger(MS.class);
    @java.io.Serial
    private static final long serialVersionUID = 1L;

    private Map<Class<? extends ShardDataTreeSnapshotMetadata<?>>, ShardDataTreeSnapshotMetadata<?>> metadata;
    private NormalizedNodeStreamVersion version;
    private NormalizedNode rootNode;

    @SuppressWarnings("checkstyle:RedundantModifier")
    public MS() {
        // For Externalizable
    }

    MS(final MetadataShardDataTreeSnapshot snapshot) {
        rootNode = snapshot.getRootNode().orElseThrow();
        metadata = snapshot.getMetadata();
        version = snapshot.version().getStreamVersion();
    }

    @Override
    public void writeExternal(final ObjectOutput out) throws IOException {
        out.writeInt(metadata.size());
        for (var m : metadata.values()) {
            out.writeObject(m);
        }
        try (var stream = version.newDataOutput(out)) {
            stream.writeNormalizedNode(rootNode);
        }
    }

    @Override
    public void readExternal(final ObjectInput in) throws IOException, ClassNotFoundException {
        final int metaSize = in.readInt();
        checkArgument(metaSize >= 0, "Invalid negative metadata map length %s", metaSize);

        // Default pre-allocate is 4, which should be fine
        final var metaBuilder = ImmutableMap
            .<Class<? extends ShardDataTreeSnapshotMetadata<?>>, ShardDataTreeSnapshotMetadata<?>>builder();
        for (int i = 0; i < metaSize; ++i) {
            final var m = (ShardDataTreeSnapshotMetadata<?>) in.readObject();
            if (m != null) {
                metaBuilder.put(m.getType(), m);
            } else {
                LOG.warn("Skipping null metadata");
            }
        }
        metadata = metaBuilder.build();

        final var stream = NormalizedNodeDataInput.newDataInput(in);
        version = stream.getVersion();
        rootNode = stream.readNormalizedNode();
    }

    @java.io.Serial
    private Object readResolve() {
        return new MetadataShardDataTreeSnapshot(rootNode, metadata);
    }
}
