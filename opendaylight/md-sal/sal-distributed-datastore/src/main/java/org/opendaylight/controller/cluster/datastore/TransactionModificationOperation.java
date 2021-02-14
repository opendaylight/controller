/*
 * Copyright (c) 2020 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore;

import static java.util.Objects.requireNonNull;

import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;

/**
 * A TransactionOperation to apply a specific modification. Subclasses provide type capture of required data, so that
 * we instantiate AbstractModification subclasses for the bare minimum time required.
 */
abstract class TransactionModificationOperation extends TransactionOperation {
    private abstract static class AbstractDataOperation extends TransactionModificationOperation {
        private final NormalizedNode data;

        AbstractDataOperation(final YangInstanceIdentifier path, final NormalizedNode data) {
            super(path);
            this.data = requireNonNull(data);
        }

        final NormalizedNode data() {
            return data;
        }
    }

    static final class DeleteOperation extends TransactionModificationOperation {
        DeleteOperation(final YangInstanceIdentifier path) {
            super(path);
        }

        @Override
        protected void invoke(final TransactionContext transactionContext, final Boolean havePermit) {
            transactionContext.executeDelete(path(), havePermit);
        }
    }

    static final class MergeOperation extends AbstractDataOperation {
        MergeOperation(final YangInstanceIdentifier path, final NormalizedNode data) {
            super(path, data);
        }

        @Override
        protected void invoke(final TransactionContext transactionContext, final Boolean havePermit) {
            transactionContext.executeMerge(path(), data(), havePermit);
        }
    }

    static final class WriteOperation extends AbstractDataOperation {
        WriteOperation(final YangInstanceIdentifier path, final NormalizedNode data) {
            super(path, data);
        }

        @Override
        protected void invoke(final TransactionContext transactionContext, final Boolean havePermit) {
            transactionContext.executeWrite(path(), data(), havePermit);
        }
    }

    private final YangInstanceIdentifier path;

    TransactionModificationOperation(final YangInstanceIdentifier path) {
        this.path = requireNonNull(path);
    }

    final YangInstanceIdentifier path() {
        return path;
    }
}
