/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.access.commands;

import akka.serialization.JSerializer;
import com.google.common.base.Preconditions;
import org.apache.commons.lang.SerializationUtils;
import org.opendaylight.controller.cluster.datastore.util.AbstractPathModificationCursor;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.PathArgument;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;

/**
 * Specialized message transformer, which transforms a {@link CommitLocalTransactionRequest} into
 * a {@link ModifyTransactionRequest}.
 *
 * This serializer needs to be plugged into akka serialization to allow forwarding of CommitLocalTransactionRequest to
 * remote shards.
 */
public final class CommitLocalTransactionRequestSerializer extends JSerializer {
    @Override
    public int identifier() {
        return 97435437;
    }

    @Override
    public boolean includeManifest() {
        return false;
    }

    @Override
    public byte[] toBinary(final Object obj) {
        Preconditions.checkArgument(obj instanceof CommitLocalTransactionRequest, "Unsupported object {}", obj);
        return SerializationUtils.serialize(commitLocalToModify((CommitLocalTransactionRequest) obj));
    }

    private static ModifyTransactionRequest commitLocalToModify(final CommitLocalTransactionRequest local) {
        final ModifyTransactionRequestBuilder b = new ModifyTransactionRequestBuilder(local.getTarget(), local.getReplyTo());
        b.setCommit(local.isCoordinated());
        b.setSequence(local.getSequence());

        local.getModification().applyToCursor(new AbstractPathModificationCursor() {
            @Override
            public void delete(final PathArgument child) {
                b.addModification(new TransactionDelete(current().node(child)));
            }

            @Override
            public void merge(final PathArgument child, final NormalizedNode<?, ?> data) {
                b.addModification(new TransactionMerge(current().node(child), data));
            }

            @Override
            public void write(final PathArgument child, final NormalizedNode<?, ?> data) {
                b.addModification(new TransactionWrite(current().node(child), data));
            }
        });

        return b.build();
    }

    @Override
    public Object fromBinaryJava(final byte[] bytes, final Class<?> clazz) {
        return SerializationUtils.deserialize(bytes);
    }
}
