/*
 * Copyright (c) 2019 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.node.utils.transformer;

import static java.util.Objects.requireNonNull;

import com.google.common.annotations.Beta;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Deque;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.PathArgument;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeModificationCursor;
import org.opendaylight.yangtools.yang.data.util.DataSchemaContextNode;
import org.opendaylight.yangtools.yang.data.util.DataSchemaContextTree;

@Beta
public final class PruningDataTreeCandidateTransactionCursor implements DataTreeCandidateTransactionCursor {
    private final Deque<DataSchemaContextNode<?>> stack = new ArrayDeque<>();
    private final DataSchemaContextTree tree;

    private DataTreeModificationCursor cursor;
    private int unknown;

    public PruningDataTreeCandidateTransactionCursor(final DataSchemaContextTree tree) {
        this.tree = requireNonNull(tree);
    }

    public void init(final DataTreeModificationCursor targetCursor) {
        cursor = requireNonNull(targetCursor);
        stack.clear();
        unknown = 0;
    }

    @Override
    public void enter(final PathArgument child) {
        if (findChild(child) != null) {
            cursor.enter(child);
        } else {
            unknown++;
        }
    }

    @Override
    public void exit() {
        if (unknown == 0) {
            stack.pop();
            cursor.exit();
        } else {
            unknown--;
        }
    }

    @Override
    public void delete(final PathArgument child) {
        if (findChild(child) != null)  {
            cursor.delete(child);
        }
    }

    @Override
    public void merge(final PathArgument child, final NormalizedNodeStream dataStream) throws IOException {
        final DataSchemaContextNode<?> schema = findChild(child);
        if (schema != null) {
            final NormalizedNode<?, ?> data = streamData(schema, dataStream);
            cursor.merge(child, data);
        } else {
            skipData(dataStream);
        }
    }

    @Override
    public void write(final PathArgument child, final NormalizedNodeStream dataStream) throws IOException {
        final DataSchemaContextNode<?> schema = findChild(child);
        if (schema != null) {
            final NormalizedNode<?, ?> data = streamData(schema, dataStream);
            cursor.write(child, data);
        } else {
            skipData(dataStream);
        }
    }

    private @Nullable DataSchemaContextNode<?> findChild(final PathArgument id) {
        if (unknown != 0) {
            return null;
        }

        DataSchemaContextNode<?> current = stack.peek();
        if (current == null) {
            current = tree.getRoot();
        }
        return current.getChild(id);
    }

    private static void skipData(@NonNull final NormalizedNodeStream dataStream) throws IOException {
        dataStream.writeTo(DummyNormalizedNodeStreamWriter.INSTANCE);
    }

    private static NormalizedNode<?, ?> streamData(final DataSchemaContextNode<?> schema,
            final NormalizedNodeStream dataStream) {
        // TODO Auto-generated method stub
        return null;
    }
}
