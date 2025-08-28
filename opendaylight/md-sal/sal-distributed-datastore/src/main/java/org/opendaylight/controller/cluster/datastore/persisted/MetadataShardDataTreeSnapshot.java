/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.persisted;

import static java.util.Objects.requireNonNull;

import com.google.common.annotations.Beta;
import com.google.common.base.MoreObjects;
import com.google.common.collect.ImmutableMap;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.Serializable;
import java.util.Map;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;

/**
 * An {@link AbstractVersionedShardDataTreeSnapshot} which contains additional metadata.
 *
 * @author Robert Varga
 */
@Beta
public final class MetadataShardDataTreeSnapshot extends AbstractVersionedShardDataTreeSnapshot
        implements Serializable {
    @java.io.Serial
    private static final long serialVersionUID = 1L;

    private final Map<Class<? extends ShardDataTreeSnapshotMetadata<?>>, ShardDataTreeSnapshotMetadata<?>> metadata;
    @SuppressFBWarnings(value = "SE_BAD_FIELD", justification = "This field is not Serializable but this class "
            + "implements writeReplace to delegate serialization to a Proxy class and thus instances of this class "
            + "aren't serialized. FindBugs does not recognize this.")
    private final NormalizedNode rootNode;

    public MetadataShardDataTreeSnapshot(final NormalizedNode rootNode) {
        this(rootNode, ImmutableMap.of());
    }

    public MetadataShardDataTreeSnapshot(final NormalizedNode rootNode,
            final Map<Class<? extends ShardDataTreeSnapshotMetadata<?>>, ShardDataTreeSnapshotMetadata<?>> metadata) {
        this.rootNode = requireNonNull(rootNode);
        this.metadata = ImmutableMap.copyOf(metadata);
    }

    public Map<Class<? extends ShardDataTreeSnapshotMetadata<?>>, ShardDataTreeSnapshotMetadata<?>> getMetadata() {
        return metadata;
    }

    @Override
    NormalizedNode rootNode() {
        return rootNode;
    }

    @Override
    PayloadVersion version() {
        return PayloadVersion.POTASSIUM;
    }

    @java.io.Serial
    private Object writeReplace() {
        return new MS(this);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this).add("metadata", metadata).toString();
    }
}
