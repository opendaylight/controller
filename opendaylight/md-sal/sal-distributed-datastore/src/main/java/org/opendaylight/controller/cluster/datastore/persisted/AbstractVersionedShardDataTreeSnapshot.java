/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.persisted;

import com.google.common.base.Verify;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
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

    @SuppressWarnings("checkstyle:FallThrough")
    static ShardDataTreeSnapshot versionedDeserialize(final ObjectInput in) throws IOException {
        final PayloadVersion version = PayloadVersion.readFrom(in);
        switch (version) {
            case BORON:
                // Boron snapshots use Java Serialization
                try {
                    return (ShardDataTreeSnapshot) in.readObject();
                } catch (ClassNotFoundException e) {
                    LOG.error("Failed to serialize data tree snapshot", e);
                    throw new IOException("Snapshot failed to deserialize", e);
                }
            case TEST_FUTURE_VERSION:
            case TEST_PAST_VERSION:
                // These versions are never returned and this code is effectively dead
                break;
            default:
                throw new IOException("Invalid payload version in snapshot");
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
    @Nonnull
    abstract NormalizedNode<?, ?> rootNode();

    /**
     * Return the snapshot payload version. Implementations of this method should return a constant.
     *
     * @return Snapshot payload version
     */
    @Nonnull
    abstract PayloadVersion version();

    private void versionedSerialize(final ObjectOutput out, final PayloadVersion version) throws IOException {
        switch (version) {
            case BORON:
                // Boron snapshots use Java Serialization
                out.writeObject(this);
                return;
            case TEST_FUTURE_VERSION:
            case TEST_PAST_VERSION:
                break;
            default:
                throw new IOException("Invalid payload version in snapshot");
        }

        throw new IOException("Encountered unhandled version" + version);
    }

    @Override
    public void serialize(final ObjectOutput out) throws IOException {
        final PayloadVersion version = version();
        version.writeTo(out);
        versionedSerialize(out, version);
    }
}
