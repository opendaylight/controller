/*
 * Copyright (c) 2016 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.utils;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import java.util.ArrayDeque;
import java.util.Deque;
import javax.annotation.Nonnull;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.PathArgument;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeModificationCursor;

/**
 * Base class for a DataTreeModificationCursor.
 *
 * @author Thomas Pantelis
 */
public abstract class AbstractDataTreeModificationCursor implements DataTreeModificationCursor {
    private final Deque<YangInstanceIdentifier> stack = new ArrayDeque<>();

    protected AbstractDataTreeModificationCursor() {
        stack.push(YangInstanceIdentifier.EMPTY);
    }

    protected YangInstanceIdentifier next(@Nonnull final PathArgument child) {
        return stack.peek().node(child);
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
