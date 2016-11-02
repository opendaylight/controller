/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.sharding;

import com.google.common.base.Preconditions;
import com.google.common.base.Verify;
import javax.annotation.Nonnull;
import org.opendaylight.controller.cluster.databroker.actors.dds.ClientTransaction;
import org.opendaylight.mdsal.dom.api.DOMDataTreeIdentifier;
import org.opendaylight.mdsal.dom.api.DOMDataTreeWriteCursor;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.PathArgument;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;

/**
 * Proxy cursor that delegates writes to {@link ClientTransaction}.
 */
class ShardProxyCursor implements DOMDataTreeWriteCursor {
    
    private YangInstanceIdentifier current = YangInstanceIdentifier.EMPTY;
    private final ClientTransaction tx;

    ShardProxyCursor(final DOMDataTreeIdentifier prefix, final ClientTransaction tx) {
        //TODO migrate whole package to mdsal LogicalDatastoreType
        this.tx = tx;
    }

    @Override
    public void delete(final PathArgument child) {
        tx.delete(current.node(child));
    }

    @Override
    public void merge(final PathArgument child, final NormalizedNode<?, ?> data) {
        tx.merge(current.node(child), data);
    }

    @Override
    public void write(final PathArgument child, final NormalizedNode<?, ?> data) {
        tx.write(current.node(child), data);
    }

    @Override
    public void enter(@Nonnull final PathArgument child) {
        current = current.node(child);
    }

    @Override
    public void enter(@Nonnull final PathArgument... path) {
        for (final PathArgument pathArgument : path) {
            enter(pathArgument);
        }
    }

    @Override
    public void enter(@Nonnull final Iterable<PathArgument> path) {
        path.forEach(this::enter);
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
    public void close() {

    }
}
