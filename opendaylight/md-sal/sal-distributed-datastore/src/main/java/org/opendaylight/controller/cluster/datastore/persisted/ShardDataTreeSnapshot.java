/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.persisted;

import com.google.common.annotations.Beta;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.Optional;
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

    public static ShardDataTreeSnapshot deserialize(final ObjectInput in) throws IOException {
        final ShardDataTreeSnapshot ret = AbstractVersionedShardDataTreeSnapshot.versionedDeserialize(in);

        // Make sure we consume all bytes, otherwise something went very wrong
        final int bytesLeft = in.available();
        if (bytesLeft != 0) {
            throw new IOException("Deserialization left " + bytesLeft + " in the buffer");
        }


        return ret;
    }

    /**
     * Get the root data node contained in this snapshot.
     *
     * @return An optional root node.
     */
    public abstract Optional<NormalizedNode<?, ?>> getRootNode();

    public abstract void serialize(ObjectOutput out) throws IOException;
}

