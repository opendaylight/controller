/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore;

import akka.actor.ActorSelection;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.CheckedFuture;
import org.opendaylight.controller.cluster.datastore.identifiers.TransactionIdentifier;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreThreePhaseCommitCohort;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreWriteTransaction;
import org.opendaylight.controller.sal.core.spi.data.SnapshotBackedTransactions;
import org.opendaylight.controller.sal.core.spi.data.SnapshotBackedWriteTransaction.TransactionReadyPrototype;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeSnapshot;
import scala.concurrent.Future;

/**
 * A local component which is statically bound to a write-only transaction.
 */
final class LocalWriteTransactionComponent extends AbstractTransactionComponent {
    private final DOMStoreWriteTransaction delegate;

    LocalWriteTransactionComponent(DOMStoreWriteTransaction delegate) {
        this.delegate = Preconditions.checkNotNull(delegate);
    }

    @Override
    CheckedFuture<Boolean, ReadFailedException> exists(YangInstanceIdentifier path) {
        throw new UnsupportedOperationException();
    }

    @Override
    CheckedFuture<Optional<NormalizedNode<?, ?>>, ReadFailedException> read(YangInstanceIdentifier path) {
        throw new UnsupportedOperationException();
    }

    @Override
    void delete(YangInstanceIdentifier path) {
        delegate.delete(path);
    }

    @Override
    void merge(YangInstanceIdentifier path, NormalizedNode<?, ?> data) {
        delegate.merge(path, data);
    }

    @Override
    void write(YangInstanceIdentifier path, NormalizedNode<?, ?> data) {
        delegate.write(path, data);
    }

}
