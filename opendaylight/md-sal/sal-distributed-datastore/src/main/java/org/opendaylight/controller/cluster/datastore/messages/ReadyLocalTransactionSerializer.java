/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.messages;

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.Objects.requireNonNull;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import org.apache.commons.lang3.SerializationUtils;
import org.apache.pekko.actor.ExtendedActorSystem;
import org.apache.pekko.serialization.JSerializer;
import org.apache.pekko.util.ClassLoaderObjectInputStream;
import org.opendaylight.controller.cluster.datastore.utils.AbstractBatchedModificationsCursor;

/**
 * Specialized message transformer, which transforms a {@link ReadyLocalTransaction}
 * into a {@link BatchedModifications} message. This serializer needs to be plugged
 * into akka serialization to allow forwarding of ReadyLocalTransaction to remote
 * shards.
 */
@Deprecated(since = "9.0.0", forRemoval = true)
public final class ReadyLocalTransactionSerializer extends JSerializer {
    private final ExtendedActorSystem system;

    public ReadyLocalTransactionSerializer(final ExtendedActorSystem system) {
        this.system = requireNonNull(system);
    }

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
        checkArgument(obj instanceof ReadyLocalTransaction, "Unsupported object type %s", obj.getClass());
        final ReadyLocalTransaction readyLocal = (ReadyLocalTransaction) obj;
        final BatchedModifications batched = new BatchedModifications(readyLocal.getTransactionId(),
                readyLocal.getRemoteVersion());
        batched.setDoCommitOnReady(readyLocal.isDoCommitOnReady());
        batched.setTotalMessagesSent(1);
        batched.setReady(readyLocal.getParticipatingShardNames());

        readyLocal.getModification().applyToCursor(new BatchedCursor(batched));

        return SerializationUtils.serialize(batched);
    }

    @Override
    public Object fromBinaryJava(final byte[] bytes, final Class<?> clazz) {
        try (ClassLoaderObjectInputStream is = new ClassLoaderObjectInputStream(system.dynamicAccess().classLoader(),
            new ByteArrayInputStream(bytes))) {
            return is.readObject();
        } catch (IOException | ClassNotFoundException e) {
            throw new IllegalStateException("Failed to deserialize object", e);
        }
    }

    private static final class BatchedCursor extends AbstractBatchedModificationsCursor {
        private final BatchedModifications message;

        BatchedCursor(final BatchedModifications message) {
            this.message = requireNonNull(message);
        }

        @Override
        protected BatchedModifications getModifications() {
            return message;
        }
    }
}
