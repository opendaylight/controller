/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import javax.annotation.concurrent.NotThreadSafe;
import org.opendaylight.controller.cluster.access.commands.ExistsTransactionSuccess;
import org.opendaylight.controller.cluster.access.commands.OutOfOrderRequestException;
import org.opendaylight.controller.cluster.access.commands.ReadTransactionSuccess;
import org.opendaylight.controller.cluster.access.commands.TransactionSuccess;
import org.opendaylight.controller.cluster.access.concepts.TransactionIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeModification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Frontend transaction state as observed by the shard leader.
 *
 * @author Robert Varga
 */
@NotThreadSafe
final class FrontendTransaction {
    private static final Logger LOG = LoggerFactory.getLogger(FrontendTransaction.class);
    private final TransactionIdentifier id;
    private final CohortEntry cohort;

    private long expectedSequence = 0;

    private FrontendTransaction(final TransactionIdentifier id) {
        this.id = Preconditions.checkNotNull(id);

        // FIXME: initialize properly
        cohort = null;
    }

    private FrontendTransaction(final TransactionIdentifier id, final DataTreeModification mod) {
        this.id = Preconditions.checkNotNull(id);

        // FIXME: initialize properly
        cohort = null;
    }

    static FrontendTransaction createOpen(final TransactionIdentifier id) {
        return new FrontendTransaction(id);
    }

    static FrontendTransaction createReady(final TransactionIdentifier id, final DataTreeModification mod) {
        return new FrontendTransaction(id, mod);
    }

    void checkRequestSequence(final long sequence) throws OutOfOrderRequestException {
        if (expectedSequence != sequence) {
            throw new OutOfOrderRequestException(expectedSequence);
        }
    }

    void incrementSequence() {
        expectedSequence++;
    }

    ExistsTransactionSuccess exists(final YangInstanceIdentifier path) {
        final Optional<NormalizedNode<?, ?>> data = cohort.getTransaction().getSnapshot().readNode(path);
        incrementSequence();
        return new ExistsTransactionSuccess(id, data.isPresent());
    }

    ReadTransactionSuccess read(final YangInstanceIdentifier path) {
        final Optional<NormalizedNode<?, ?>> data = cohort.getTransaction().getSnapshot().readNode(path);
        incrementSequence();
        return new ReadTransactionSuccess(id, data);
    }

    void delete(final YangInstanceIdentifier path) {
        cohort.getTransaction().getSnapshot().delete(path);
    }

    void merge(final YangInstanceIdentifier path, final NormalizedNode<?, ?> data) {
        cohort.getTransaction().getSnapshot().merge(path, data);
    }

    void write(final YangInstanceIdentifier path, final NormalizedNode<?, ?> data) {
        cohort.getTransaction().getSnapshot().write(path, data);
    }

    TransactionSuccess<?> directAbort() {
        // TODO Auto-generated method stub
        return null;
    }

    TransactionSuccess<?> directCommit() {
        // TODO Auto-generated method stub
        return null;
    }

    TransactionSuccess<?> coordinatedCommit() {
        // TODO Auto-generated method stub
        return null;
    }
}
