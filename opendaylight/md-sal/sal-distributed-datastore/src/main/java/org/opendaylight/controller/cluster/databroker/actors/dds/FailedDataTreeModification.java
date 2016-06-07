/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.databroker.actors.dds;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import java.util.function.Supplier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeModification;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeModificationCursor;

/**
 * An implementation of DataTreeModification which throws a specified {@link RuntimeException} when any of its methods
 * are invoked.
 *
 * @author Robert Varga
 */
final class FailedDataTreeModification implements DataTreeModification {
    private final Supplier<? extends RuntimeException> supplier;

    FailedDataTreeModification(final Supplier<? extends RuntimeException> supplier) {
        this.supplier = Preconditions.checkNotNull(supplier);
    }

    @Override
    public Optional<NormalizedNode<?, ?>> readNode(final YangInstanceIdentifier path) {
        throw supplier.get();
    }

    @Override
    public DataTreeModification newModification() {
        throw supplier.get();
    }

    @Override
    public void delete(final YangInstanceIdentifier path) {
        throw supplier.get();
    }

    @Override
    public void merge(final YangInstanceIdentifier path, final NormalizedNode<?, ?> data) {
        throw supplier.get();
    }

    @Override
    public void write(final YangInstanceIdentifier path, final NormalizedNode<?, ?> data) {
        throw supplier.get();
    }

    @Override
    public void ready() {
        throw supplier.get();
    }

    @Override
    public void applyToCursor(final DataTreeModificationCursor cursor) {
        throw supplier.get();
    }
}
