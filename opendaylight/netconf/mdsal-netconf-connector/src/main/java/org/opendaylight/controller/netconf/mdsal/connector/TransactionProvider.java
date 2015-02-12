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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

//TODO make a global TransactionProvider for all Netconf sessions instead of each session having one.
public class TransactionProvider implements AutoCloseable{

    private static final Logger LOG = LoggerFactory.getLogger(TransactionProvider.class);

    private final DOMDataBroker dataBroker;

    private DOMDataReadWriteTransaction candidateTransaction = null;
    private DOMDataReadWriteTransaction runningTransaction = null;
    private final List<DOMDataReadWriteTransaction> allOpenReadWriteTransactions = new ArrayList<>();

    private final String netconfSessionIdForReporting;

    private static final String  NO_TRANSACTION_FOUND_FOR_SESSION = "No candidateTransaction found for session ";


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

    public synchronized Optional<DOMDataReadWriteTransaction> getCandidateTransaction() {
        if (candidateTransaction == null) {
            return Optional.absent();
        }

        return Optional.of(candidateTransaction);
    }

    public synchronized DOMDataReadWriteTransaction getOrCreateTransaction() {
        if (getCandidateTransaction().isPresent()) {
            return getCandidateTransaction().get();
        }

        candidateTransaction = dataBroker.newReadWriteTransaction();
        allOpenReadWriteTransactions.add(candidateTransaction);
        return candidateTransaction;
    }

    public synchronized boolean commitTransaction() throws NetconfDocumentedException {
        if (!getCandidateTransaction().isPresent()) {
            throw new NetconfDocumentedException(NO_TRANSACTION_FOUND_FOR_SESSION + netconfSessionIdForReporting,
                    ErrorType.application, ErrorTag.operation_failed, ErrorSeverity.error);
        }

        CheckedFuture<Void, TransactionCommitFailedException> future = candidateTransaction.submit();
        try {
            future.checkedGet();
        } catch (TransactionCommitFailedException e) {
            LOG.debug("Transaction {} failed on", candidateTransaction, e);
            throw new NetconfDocumentedException("Transaction commit failed on " + e.getMessage() + " " + netconfSessionIdForReporting,
                    ErrorType.application, ErrorTag.operation_failed, ErrorSeverity.error);
        }
        allOpenReadWriteTransactions.remove(candidateTransaction);
        candidateTransaction = null;

        return true;
    }

    public synchronized void abortTransaction() {
        LOG.debug("Aborting current candidateTransaction");
        Optional<DOMDataReadWriteTransaction> otx = getCandidateTransaction();
        Preconditions.checkState(otx.isPresent(), NO_TRANSACTION_FOUND_FOR_SESSION + netconfSessionIdForReporting);
        candidateTransaction.cancel();
        allOpenReadWriteTransactions.remove(candidateTransaction);
        candidateTransaction = null;
    }

    public synchronized DOMDataReadWriteTransaction createRunningTransaction() {
        runningTransaction = dataBroker.newReadWriteTransaction();
        allOpenReadWriteTransactions.add(runningTransaction);
        return runningTransaction;
    }

    public synchronized boolean commitRunningTransaction(DOMDataReadWriteTransaction tx) throws NetconfDocumentedException {
        allOpenReadWriteTransactions.remove(tx);

        CheckedFuture<Void, TransactionCommitFailedException> future = tx.submit();
        try {
            future.checkedGet();
        } catch (TransactionCommitFailedException e) {
            LOG.debug("Transaction {} failed on", tx, e);
            throw new NetconfDocumentedException("Transaction commit failed on " + e.getMessage() + " " + netconfSessionIdForReporting,
                    ErrorType.application, ErrorTag.operation_failed, ErrorSeverity.error);
        }

        return true;
    }

    public synchronized void abortRunningTransaction(DOMDataReadWriteTransaction tx) {
        LOG.debug("Aborting current running Transaction");
        Preconditions.checkState(runningTransaction != null, NO_TRANSACTION_FOUND_FOR_SESSION + netconfSessionIdForReporting);
        tx.cancel();
        allOpenReadWriteTransactions.remove(tx);
    }

}
