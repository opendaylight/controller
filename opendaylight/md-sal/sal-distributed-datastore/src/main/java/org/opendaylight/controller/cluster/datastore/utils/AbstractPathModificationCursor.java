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
import com.google.common.base.Verify;
import javax.annotation.Nonnull;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.PathArgument;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeModificationCursor;

@Beta
public abstract class AbstractPathModificationCursor implements DataTreeModificationCursor {
    private YangInstanceIdentifier current = YangInstanceIdentifier.EMPTY;

    protected final YangInstanceIdentifier current() {
        return current;
    }

    @Override
    public final void enter(@Nonnull final PathArgument child) {
        current = current.node(child);
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
        Preconditions.checkState(!current.isEmpty());
        current = Verify.verifyNotNull(current.getParent());
    }

    @Override
    public final void exit(final int depth) {
        Preconditions.checkArgument(depth >= 0);

        YangInstanceIdentifier next = current;
        for (int i = 0; i < depth; ++i) {
            next = next.getParent();
            Preconditions.checkState(next != null);
        }

        current = next;
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
