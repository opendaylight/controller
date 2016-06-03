/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.utils;

import com.google.common.annotations.Beta;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import java.util.ArrayDeque;
import java.util.Deque;
import javax.annotation.Nonnull;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.PathArgument;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeModificationCursor;

@Beta
public abstract class AbstractPathModificationCursor implements DataTreeModificationCursor {
    private final Deque<YangInstanceIdentifier> stack = new ArrayDeque<>();

    protected AbstractPathModificationCursor() {
        stack.push(YangInstanceIdentifier.EMPTY);
    }

    protected final YangInstanceIdentifier current() {
        return stack.peek();
    }

    @Override
    public final void enter(@Nonnull final PathArgument child) {
        stack.push(current().node(child));
    }

    @Override
    public final void enter(@Nonnull final PathArgument... path) {
        for (PathArgument arg : path) {
            enter(arg);
        }
    }

    @Override
    public final void enter(@Nonnull final Iterable<PathArgument> path) {
        for (PathArgument arg : path) {
            enter(arg);
        }
    }

    @Override
    public final void exit() {
        stack.pop();
    }

    @Override
    public final void exit(final int depth) {
        Preconditions.checkArgument(depth < stack.size(), "Stack holds only %s elements, cannot exit %s levels", stack.size(), depth);
        for (int i = 0; i < depth; ++i) {
            stack.pop();
        }
    }

    @Override
    public final Optional<NormalizedNode<?, ?>> readNode(@Nonnull final PathArgument child) {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public void close() {
        // No-op
    }
}
