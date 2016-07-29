/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.persisted;

import com.google.common.base.Verify;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Optional;
import javax.annotation.Nonnull;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An internally-versioned {@link ShardDataTreeSnapshot}. This class is an intermediate implementation-private
 * class.
 *
 * @author Robert Varga
 */
abstract class AbstractVersionedShardDataTreeSnapshot extends ShardDataTreeSnapshot {
    private static final Logger LOG = LoggerFactory.getLogger(AbstractVersionedShardDataTreeSnapshot.class);

    static ShardDataTreeSnapshot deserialize(final DataInputStream is) throws IOException {
        final PayloadVersion version = PayloadVersion.readFrom(is);
        switch (version) {
            case BORON:
                // Boron snapshots use Java Serialization
                try (final ObjectInputStream ois = new ObjectInputStream(is)) {
                    return (ShardDataTreeSnapshot) ois.readObject();
                } catch (ClassNotFoundException e) {
                    LOG.error("Failed to serialize data tree snapshot", e);
                    throw new IOException("Snapshot failed to deserialize", e);
                }
            case TEST_FUTURE_VERSION:
            case TEST_PAST_VERSION:
                // These versions are never returned and this code is effectively dead
                break;
        }

        // Not included as default in above switch to ensure we get warnings when new versions are added
        throw new IOException("Encountered unhandled version" + version);
    }

    @Override
    public final Optional<NormalizedNode<?, ?>> getRootNode() {
        return Optional.of(Verify.verifyNotNull(rootNode(), "Snapshot %s returned non-present root node", getClass()));
    }

    /**
     * Return the root node.
     *
     * @return The root node.
     */
    abstract @Nonnull NormalizedNode<?, ?> rootNode();

    /**
     * Return the snapshot payload version. Implementations of this method should return a constant.
     *
     * @return Snapshot payload version
     */
    abstract @Nonnull PayloadVersion version();

    private void versionedSerialize(final DataOutputStream dos, final PayloadVersion version) throws IOException {
        switch (version) {
            case BORON:
                // Boron snapshots use Java Serialization
                try (ObjectOutputStream oos = new ObjectOutputStream(dos)) {
                    oos.writeObject(this);
                }
                return;
            case TEST_FUTURE_VERSION:
            case TEST_PAST_VERSION:
                break;

        }

        throw new IOException("Encountered unhandled version" + version);
    }

    @Override
    public final byte[] serialize() throws IOException {
        try (final ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            try (final DataOutputStream dos = new DataOutputStream(bos)) {
                final PayloadVersion version = version();
                version.writeTo(dos);
                versionedSerialize(dos, version);
            }

            return bos.toByteArray();
        }
    }
}
