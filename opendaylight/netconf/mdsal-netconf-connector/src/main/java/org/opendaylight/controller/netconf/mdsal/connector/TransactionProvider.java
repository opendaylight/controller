/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.netconf.mdsal.connector;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.CheckedFuture;
import java.util.ArrayList;
import java.util.List;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.controller.md.sal.dom.api.DOMDataBroker;
import org.opendaylight.controller.md.sal.dom.api.DOMDataReadWriteTransaction;
import org.opendaylight.controller.netconf.api.NetconfDocumentedException;
import org.opendaylight.controller.netconf.api.NetconfDocumentedException.ErrorSeverity;
import org.opendaylight.controller.netconf.api.NetconfDocumentedException.ErrorTag;
import org.opendaylight.controller.netconf.api.NetconfDocumentedException.ErrorType;
import org.opendaylight.controller.netconf.confignetconfconnector.exception.NoTransactionFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TransactionProvider implements AutoCloseable{

    private static final Logger LOG = LoggerFactory.getLogger(TransactionProvider.class);

    private final DOMDataBroker dataBroker;

    private DOMDataReadWriteTransaction transaction = null;
    private final List<DOMDataReadWriteTransaction> allOpenReadWriteTransactions = new ArrayList<>();

    private final String netconfSessionIdForReporting;

    private static final String  NO_TRANSACTION_FOUND_FOR_SESSION = "No transaction found for session ";


    public TransactionProvider(DOMDataBroker dataBroker, String netconfSessionIdForReporting) {
        this.dataBroker = dataBroker;
        this.netconfSessionIdForReporting = netconfSessionIdForReporting;
    }

    @Override
    public synchronized void close() throws Exception {
        for (DOMDataReadWriteTransaction rwt : allOpenReadWriteTransactions) {
            rwt.cancel();
        }

        allOpenReadWriteTransactions.clear();
    }

    public synchronized Optional<DOMDataReadWriteTransaction> getTransaction() {
        if (transaction == null) {
            return Optional.absent();
        }

        return Optional.of(transaction);
    }

    public synchronized DOMDataReadWriteTransaction getOrCreateTransaction() {
        if (getTransaction().isPresent()) {
            return getTransaction().get();
        }

        transaction = dataBroker.newReadWriteTransaction();
        allOpenReadWriteTransactions.add(transaction);
        return transaction;
    }

    public synchronized boolean commitTransaction() throws NetconfDocumentedException {
        if (!getTransaction().isPresent()) {
            throw new NoTransactionFoundException(NO_TRANSACTION_FOUND_FOR_SESSION + netconfSessionIdForReporting,
                    ErrorType.application, ErrorTag.operation_failed, ErrorSeverity.error);
        }

        CheckedFuture<Void, TransactionCommitFailedException> future = transaction.submit();
        try {
            future.checkedGet();
        } catch (TransactionCommitFailedException e) {
            LOG.debug("Transaction {} failed on {} ", transaction, e);
            throw new NetconfDocumentedException("Transaction commit failed on " + e.getMessage() + " " + netconfSessionIdForReporting,
                    ErrorType.application, ErrorTag.operation_failed, ErrorSeverity.error);
        }

        return true;
    }

    public synchronized void abortTransaction() {
        LOG.debug("Aborting current transaction");
        Optional<DOMDataReadWriteTransaction> otx = getTransaction();
        Preconditions.checkState(otx.isPresent(), NO_TRANSACTION_FOUND_FOR_SESSION + netconfSessionIdForReporting);
        transaction.cancel();
        allOpenReadWriteTransactions.remove(transaction);
        transaction = null;
    }

}
