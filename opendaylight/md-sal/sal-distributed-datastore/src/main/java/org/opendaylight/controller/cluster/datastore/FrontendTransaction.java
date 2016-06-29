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
import org.opendaylight.controller.cluster.access.commands.CommitLocalTransactionRequest;
import org.opendaylight.controller.cluster.access.commands.ExistsTransactionRequest;
import org.opendaylight.controller.cluster.access.commands.ExistsTransactionSuccess;
import org.opendaylight.controller.cluster.access.commands.ModifyTransactionRequest;
import org.opendaylight.controller.cluster.access.commands.OutOfOrderRequestException;
import org.opendaylight.controller.cluster.access.commands.ReadTransactionRequest;
import org.opendaylight.controller.cluster.access.commands.ReadTransactionSuccess;
import org.opendaylight.controller.cluster.access.commands.TransactionAbortRequest;
import org.opendaylight.controller.cluster.access.commands.TransactionDoCommitRequest;
import org.opendaylight.controller.cluster.access.commands.TransactionPreCommitRequest;
import org.opendaylight.controller.cluster.access.commands.TransactionRequest;
import org.opendaylight.controller.cluster.access.commands.TransactionSuccess;
import org.opendaylight.controller.cluster.access.concepts.RequestException;
import org.opendaylight.controller.cluster.access.concepts.TransactionIdentifier;
import org.opendaylight.controller.cluster.access.concepts.UnsupportedRequestException;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
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

    FrontendTransaction(final TransactionIdentifier id) {
        this.id = Preconditions.checkNotNull(id);

        // FIXME: initialize properly
        cohort = null;
    }

    TransactionSuccess<?> handleRequest(final TransactionRequest<?> request, final long sequence) throws RequestException {


        if (request instanceof CommitLocalTransactionRequest) {

        } else if (request instanceof ModifyTransactionRequest) {

        } else if (request instanceof ExistsTransactionRequest) {

        } else if (request instanceof ReadTransactionRequest) {

        } else if (request instanceof TransactionPreCommitRequest) {

        } else if (request instanceof TransactionDoCommitRequest) {

        } else if (request instanceof TransactionAbortRequest) {

        } else {
            throw new UnsupportedRequestException(request);
        }

        expectedSequence++;


        // TODO Auto-generated method stub
        return null;
    }

    void checkRequestSequence(final long sequence) throws OutOfOrderRequestException {
        if (expectedSequence != sequence) {
            throw new OutOfOrderRequestException(expectedSequence);
        }
    }

    ExistsTransactionSuccess exists(final YangInstanceIdentifier path) {
        final Optional<NormalizedNode<?, ?>> data = cohort.getTransaction().getSnapshot().readNode(path);
        return new ExistsTransactionSuccess(id, data.isPresent());
    }

    ReadTransactionSuccess read(final YangInstanceIdentifier path) {
        final Optional<NormalizedNode<?, ?>> data = cohort.getTransaction().getSnapshot().readNode(path);
        return new ReadTransactionSuccess(id, data);
    }
}
