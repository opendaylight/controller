/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.databroker.actors.dds;

import static com.google.common.base.Preconditions.checkState;
import static java.util.Objects.requireNonNull;

import java.util.Arrays;
import org.opendaylight.mdsal.dom.api.DOMDataTreeWriteCursor;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.PathArgument;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;

/**
 * A {@link DOMDataTreeWriteCursor} tied to a {@link ClientTransaction}.
 *
 * @author Robert Varga
 */
@Deprecated(forRemoval = true)
final class ClientTransactionCursor implements DOMDataTreeWriteCursor {
    private YangInstanceIdentifier current = YangInstanceIdentifier.empty();
    private final ClientTransaction parent;

    ClientTransactionCursor(final ClientTransaction parent) {
        this.parent = requireNonNull(parent);
    }

    @Override
    public void enter(final PathArgument child) {
        current = current.node(child);
    }

    @Override
    public void enter(final PathArgument... path) {
        enter(Arrays.asList(path));
    }

    @Override
    public void enter(final Iterable<PathArgument> path) {
        path.forEach(this::enter);
    }

    @Override
    public void exit() {
        final YangInstanceIdentifier currentParent = current.getParent();
        checkState(currentParent != null);
        current = currentParent;
    }

    @Override
    public void exit(final int depth) {
        for (int i = 0; i < depth; ++i) {
            exit();
        }
    }

    @Override
    public void close() {
        parent.closeCursor(this);
    }

    @Override
    public void delete(final PathArgument child) {
        parent.delete(current.node(child));
    }

    @Override
    public void merge(final PathArgument child, final NormalizedNode<?, ?> data) {
        parent.merge(current.node(child), data);
    }

    @Override
    public void write(final PathArgument child, final NormalizedNode<?, ?> data) {
        parent.write(current.node(child), data);
    }
}
