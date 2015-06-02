/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.messages;

import akka.serialization.JSerializer;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import java.util.ArrayDeque;
import java.util.Deque;
import javax.annotation.Nonnull;
import org.apache.commons.lang3.SerializationUtils;
import org.opendaylight.controller.cluster.datastore.modification.DeleteModification;
import org.opendaylight.controller.cluster.datastore.modification.MergeModification;
import org.opendaylight.controller.cluster.datastore.modification.WriteModification;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.PathArgument;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeModificationCursor;

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
        batched.setReady(true);

        readyLocal.getModification().applyToCursor(new BatchedCursor(batched));

        return SerializationUtils.serialize(batched);
    }

    @Override
    public Object fromBinaryJava(final byte[] bytes, final Class<?> clazz) {
        return SerializationUtils.deserialize(bytes);
    }

    private static final class BatchedCursor implements DataTreeModificationCursor {
        private final Deque<YangInstanceIdentifier> stack = new ArrayDeque<>();
        private final BatchedModifications message;

        BatchedCursor(final BatchedModifications message) {
            this.message = Preconditions.checkNotNull(message);
            stack.push(YangInstanceIdentifier.EMPTY);
        }

        @Override
        public void delete(final PathArgument child) {
            message.addModification(new DeleteModification(stack.peek().node(child)));
        }

        @Override
        public void merge(final PathArgument child, final NormalizedNode<?, ?> data) {
            message.addModification(new MergeModification(stack.peek().node(child), data));
        }

        @Override
        public void write(final PathArgument child, final NormalizedNode<?, ?> data) {
            message.addModification(new WriteModification(stack.peek().node(child), data));
        }

        @Override
        public void enter(@Nonnull final PathArgument child) {
            stack.push(stack.peek().node(child));
        }

        @Override
        public void enter(@Nonnull final PathArgument... path) {
            for (PathArgument arg : path) {
                enter(arg);
            }
        }

        @Override
        public void enter(@Nonnull final Iterable<PathArgument> path) {
            for (PathArgument arg : path) {
                enter(arg);
            }
        }

        @Override
        public void exit() {
            stack.pop();
        }

        @Override
        public void exit(final int depth) {
            Preconditions.checkArgument(depth < stack.size(), "Stack holds only %s elements, cannot exit %s levels", stack.size(), depth);
            for (int i = 0; i < depth; ++i) {
                stack.pop();
            }
        }

        @Override
        public Optional<NormalizedNode<?, ?>> readNode(@Nonnull final PathArgument child) {
            throw new UnsupportedOperationException("Not implemented");
        }

        @Override
        public void close() {
            // No-op
        }
    }
}
