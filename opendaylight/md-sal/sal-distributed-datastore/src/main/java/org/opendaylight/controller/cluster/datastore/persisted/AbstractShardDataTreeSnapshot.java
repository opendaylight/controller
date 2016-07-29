/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.persisted;

import com.google.common.annotations.Beta;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;
import javax.annotation.Nullable;
import org.opendaylight.controller.cluster.datastore.utils.SerializationUtils;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Abstract base class for snapshots of the ShardDataTree.
 *
 * @author Robert Varga
 */
@Beta
public abstract class AbstractShardDataTreeSnapshot {
    private static final Logger LOG = LoggerFactory.getLogger(AbstractShardDataTreeSnapshot.class);

    /**
     * Versions prior to Boron did not include any way to evolve the snapshot format and contained only
     * the raw data stored in the datastore.
     */
    @Deprecated
    private static @Nullable AbstractShardDataTreeSnapshot tryLegacySnapshot(final byte[] snapshotBytes) {
        final NormalizedNode<?, ?> node;
        try {
            node = SerializationUtils.deserializeNormalizedNode(snapshotBytes);
        } catch (Exception e) {
            LOG.debug("Legacy snapshot deserialization failed", e);
            return null;
        }

        return new LegacyShardDataTreeSnapshot(node);
    }

    public static AbstractShardDataTreeSnapshot deserialize(final byte[] snapshotBytes) throws IOException {
        final AbstractShardDataTreeSnapshot legacy = tryLegacySnapshot(snapshotBytes);
        if (legacy != null) {
            return legacy;
        }

        try (final InputStream is = new ByteArrayInputStream(snapshotBytes)) {
            try (final DataInputStream dis = new DataInputStream(is)) {
                return AbstractVersionedShardDataTreeSnapshot.deserialize(dis);
            }
        }
    }

    public abstract Optional<NormalizedNode<?, ?>> getRootNode();
    public abstract byte[] serialize() throws IOException;
}

