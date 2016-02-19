/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.messages;

import akka.serialization.JSerializer;
import com.google.common.base.Preconditions;
import org.apache.commons.lang3.SerializationUtils;
import org.opendaylight.controller.cluster.datastore.utils.AbstractBatchedModificationsCursor;

/**
 * Specialized message transformer, which transforms a {@link ReadyLocalTransaction}
 * into a {@link BatchedModifications} message. This serializer needs to be plugged
 * into akka serialization to allow forwarding of ReadyLocalTransaction to remote
 * shards.
 */
public final class ReadyLocalTransactionSerializer extends JSerializer {
    @Override
    public int identifier() {
        return 97439437;
    }

    @Override
    public boolean includeManifest() {
        return false;
    }

    @Override
    public byte[] toBinary(final Object obj) {
        Preconditions.checkArgument(obj instanceof ReadyLocalTransaction, "Unsupported object type %s", obj.getClass());
        final ReadyLocalTransaction readyLocal = (ReadyLocalTransaction) obj;
        final BatchedModifications batched = new BatchedModifications(readyLocal.getTransactionID(),
                readyLocal.getRemoteVersion(), "");
        batched.setDoCommitOnReady(readyLocal.isDoCommitOnReady());
        batched.setTotalMessagesSent(1);
        batched.setReady(true);

        readyLocal.getModification().applyToCursor(new BatchedCursor(batched));

        return SerializationUtils.serialize(batched);
    }

    @Override
    public Object fromBinaryJava(final byte[] bytes, final Class<?> clazz) {
        return SerializationUtils.deserialize(bytes);
    }

    private static final class BatchedCursor extends AbstractBatchedModificationsCursor {
        private final BatchedModifications message;

        BatchedCursor(final BatchedModifications message) {
            this.message = Preconditions.checkNotNull(message);
        }

        @Override
        protected BatchedModifications getModifications() {
            return message;
        }
    }
}
