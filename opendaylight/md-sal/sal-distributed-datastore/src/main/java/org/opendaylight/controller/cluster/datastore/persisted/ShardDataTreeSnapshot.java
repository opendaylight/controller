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
import javax.annotation.Nonnull;
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
public abstract class ShardDataTreeSnapshot {
    private static final Logger LOG = LoggerFactory.getLogger(ShardDataTreeSnapshot.class);

    ShardDataTreeSnapshot() {
        // Hidden to prevent subclassing from outside of this package
    }

    public static ShardDataTreeSnapshot deserialize(final byte[] bytes) throws IOException {
        /**
         * Unfortunately versions prior to Boron did not include any way to evolve the snapshot format and contained
         * only the raw data stored in the datastore. Furthermore utilities involved do not check if the array is
         * completely consumed, which has a nasty side-effect when coupled with the fact that PayloadVersion writes
         * a short value.
         *
         * Since our versions fit into a single byte, we end up writing the 0 as the first byte, which would be
         * interpreted as 'not present' by the old snapshot format, which uses writeBoolean/readBoolean. A further
         * complication is that readBoolean interprets any non-zero value as true, hence we cannot use a wild value
         * to cause it to fail.
         */
        if (isLegacyStream(bytes)) {
            return deserializeLegacy(bytes);
        }

        try {
            try (final InputStream is = new ByteArrayInputStream(bytes)) {
                try (final DataInputStream dis = new DataInputStream(is)) {
                    final ShardDataTreeSnapshot ret = AbstractVersionedShardDataTreeSnapshot.deserialize(dis);

                    // Make sure we consume all bytes, otherwise something went very wrong
                    final int bytesLeft = dis.available();
                    if (bytesLeft != 0) {
                        throw new IOException("Deserialization left " + bytesLeft + " in the buffer");
                    }


                    return ret;
                }
            }
        } catch (IOException e) {
            LOG.debug("Failed to deserialize versioned stream, attempting pre-Lithium ProtoBuf", e);
            return deserializeLegacy(bytes);
        }
    }

    /**
     * Get the root data node contained in this snapshot.
     *
     * @return An optional root node.
     */
    public abstract Optional<NormalizedNode<?, ?>> getRootNode();

    /**
     * Serialize this snapshot into a byte array for persistence.
     *
     * @return Serialized snapshot
     * @throws IOException when a serialization problem occurs
     */
    public abstract @Nonnull byte[] serialize() throws IOException;

    private static boolean isLegacyStream(final byte[] bytes) {
        if (bytes.length < 2) {
            // Versioned streams have at least two bytes
            return true;
        }

        /*
         * The stream could potentially be a versioned stream. Here we rely on the signature marker available
         * in org.opendaylight.controller.cluster.datastore.node.utils.stream.TokenTypes.
         *
         * For an old stream to be this long, the first byte has to be non-zero and the second byte has to be 0xAB.
         *
         * For a versioned stream, that translates to at least version 427 -- giving us at least 421 further versions
         * before this check breaks.
         */
        return bytes[0] != 0 && bytes[1] == (byte)0xAB;
    }

    @Deprecated
    private static ShardDataTreeSnapshot deserializeLegacy(final byte[] bytes) {
        return new LegacyShardDataTreeSnapshot(SerializationUtils.deserializeNormalizedNode(bytes));
    }
}

