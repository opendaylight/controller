/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.messages;

import akka.serialization.JSerializer;
import org.apache.commons.lang3.SerializationUtils;
import org.opendaylight.controller.cluster.access.commands.CommitLocalTransactionRequest;
import org.opendaylight.controller.cluster.access.commands.ModifyTransactionRequest;
import org.opendaylight.controller.cluster.access.commands.ModifyTransactionRequestBuilder;
import org.opendaylight.controller.cluster.access.commands.TransactionDelete;
import org.opendaylight.controller.cluster.access.commands.TransactionMerge;
import org.opendaylight.controller.cluster.access.commands.TransactionWrite;
import org.opendaylight.controller.cluster.datastore.modification.Modification;
import org.opendaylight.controller.cluster.datastore.utils.AbstractBatchedModificationsCursor;
import org.opendaylight.controller.cluster.datastore.utils.AbstractPathModificationCursor;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.PathArgument;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;

/**
 * Specialized message transformer, which transforms a {@link ReadyLocalTransaction} into a {@link BatchedModifications}
 * message. It also handles {@link CommitLocalTransactionRequest}, which is translated into {@link ModifyTransactionRequest}.
 *
 * This serializer needs to be plugged into akka serialization to allow forwarding of ReadyLocalTransaction to remote
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
        if (obj instanceof ReadyLocalTransaction) {
            return SerializationUtils.serialize(readyLocalToBatched((ReadyLocalTransaction) obj));
        }
        if (obj instanceof CommitLocalTransactionRequest) {
            return SerializationUtils.serialize(commitLocalToModify((CommitLocalTransactionRequest) obj));
        }

        throw new IllegalArgumentException("Unsupported object type " + obj.getClass());
    }

    @Override
    public Object fromBinaryJava(final byte[] bytes, final Class<?> clazz) {
        return SerializationUtils.deserialize(bytes);
    }

    private static BatchedModifications readyLocalToBatched(final ReadyLocalTransaction local) {
        final BatchedModifications batched = new BatchedModifications(local.getTransactionID(),
            local.getRemoteVersion());
        batched.setDoCommitOnReady(local.isDoCommitOnReady());
        batched.setTotalMessagesSent(1);
        batched.setReady(true);

        local.getModification().applyToCursor(new AbstractBatchedModificationsCursor() {
            @Override
            protected void addModification(final Modification mod) {
                batched.addModification(mod);
            }
        });
        return batched;
    }

    private static ModifyTransactionRequest commitLocalToModify(final CommitLocalTransactionRequest local) {
        final ModifyTransactionRequestBuilder b = new ModifyTransactionRequestBuilder(local.getTarget(), local.getReplyTo());
        b.setCommit(local.isCoordinated());
        b.setSequence(local.getSequence());

        local.getModification().applyToCursor(new AbstractPathModificationCursor() {
            @Override
            public void delete(PathArgument child) {
                b.addModification(new TransactionDelete(current().node(child)));
            }

            @Override
            public void merge(PathArgument child, NormalizedNode<?, ?> data) {
                b.addModification(new TransactionMerge(current().node(child), data));
            }

            @Override
            public void write(PathArgument child, NormalizedNode<?, ?> data) {
                b.addModification(new TransactionWrite(current().node(child), data));
            }
        });

        return b.build();
    }
}
