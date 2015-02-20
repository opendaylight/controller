/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.netconf.confignetconfconnector.transactions;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import javax.management.InstanceNotFoundException;
import javax.management.ObjectName;
import org.opendaylight.controller.config.api.ConflictingVersionException;
import org.opendaylight.controller.config.api.ValidationException;
import org.opendaylight.controller.config.api.jmx.CommitStatus;
import org.opendaylight.controller.config.util.ConfigRegistryClient;
import org.opendaylight.controller.config.util.ConfigTransactionClient;
import org.opendaylight.controller.netconf.api.NetconfDocumentedException;
import org.opendaylight.controller.netconf.confignetconfconnector.exception.NoTransactionFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TransactionProvider implements AutoCloseable {
    private static final Logger LOG = LoggerFactory.getLogger(TransactionProvider.class);

    private final ConfigRegistryClient configRegistryClient;

    private final String netconfSessionIdForReporting;
    private ObjectName candidateTx;
    private ObjectName readTx;
    private final List<ObjectName> allOpenedTransactions = new ArrayList<>();
    private static final String  NO_TRANSACTION_FOUND_FOR_SESSION = "No transaction found for session ";

    public TransactionProvider(ConfigRegistryClient configRegistryClient, String netconfSessionIdForReporting) {
        this.configRegistryClient = configRegistryClient;
        this.netconfSessionIdForReporting = netconfSessionIdForReporting;
    }

    @Override
    public synchronized void close() {
        for (ObjectName tx : allOpenedTransactions) {
            try {
                if (isStillOpenTransaction(tx)) {
                    configRegistryClient.getConfigTransactionClient(tx).abortConfig();
                }
            } catch (Exception e) {
                LOG.debug("Ignoring exception while closing transaction {}", tx, e);
            }
        }
        allOpenedTransactions.clear();
    }

    public synchronized Optional<ObjectName> getTransaction() {

        if (candidateTx == null){
            return Optional.absent();
        }

        // Transaction was already closed somehow
        if (!isStillOpenTransaction(candidateTx)) {
            LOG.warn("Fixing illegal state: transaction {} was closed in {}", candidateTx,
                    netconfSessionIdForReporting);
            candidateTx = null;
            return Optional.absent();
        }
        return Optional.of(candidateTx);
    }

    public synchronized Optional<ObjectName> getReadTransaction() {

        if (readTx == null){
            return Optional.absent();
        }

        // Transaction was already closed somehow
        if (!isStillOpenTransaction(readTx)) {
            LOG.warn("Fixing illegal state: transaction {} was closed in {}", readTx,
                    netconfSessionIdForReporting);
            readTx = null;
            return Optional.absent();
        }
        return Optional.of(readTx);
    }

    private boolean isStillOpenTransaction(ObjectName transaction) {
        return configRegistryClient.getOpenConfigs().contains(transaction);
    }

    public synchronized ObjectName getOrCreateTransaction() {
        Optional<ObjectName> ta = getTransaction();

        if (ta.isPresent()) {
            return ta.get();
        }
        candidateTx = configRegistryClient.beginConfig();
        allOpenedTransactions.add(candidateTx);
        return candidateTx;
    }

    public synchronized ObjectName getOrCreateReadTransaction() {
        Optional<ObjectName> ta = getReadTransaction();

        if (ta.isPresent()) {
            return ta.get();
        }
        readTx = configRegistryClient.beginConfig();
        allOpenedTransactions.add(readTx);
        return readTx;
    }

    /**
     * Used for editConfig test option
     */
    public synchronized ObjectName getTestTransaction() {
        ObjectName testTx = configRegistryClient.beginConfig();
        allOpenedTransactions.add(testTx);
        return testTx;
    }

    /**
     * Commit and notification send must be atomic
     */
    public synchronized CommitStatus commitTransaction() throws ValidationException, ConflictingVersionException, NoTransactionFoundException {
        if (!getTransaction().isPresent()){
            throw new NoTransactionFoundException("No transaction found for session " + netconfSessionIdForReporting,
                    NetconfDocumentedException.ErrorType.application,
                    NetconfDocumentedException.ErrorTag.operation_failed,
                    NetconfDocumentedException.ErrorSeverity.error);
        }
        final Optional<ObjectName> maybeTaON = getTransaction();
        ObjectName taON = maybeTaON.get();
        try {
            CommitStatus status = configRegistryClient.commitConfig(taON);
            // clean up
            allOpenedTransactions.remove(candidateTx);
            candidateTx = null;
            return status;
        } catch (ValidationException validationException) {
            // no clean up: user can reconfigure and recover this transaction
            LOG.warn("Transaction {} failed on {}", taON, validationException.toString());
            throw validationException;
        } catch (ConflictingVersionException e) {
            LOG.error("Exception while commit of {}, aborting transaction", taON, e);
            // clean up
            abortTransaction();
            throw e;
        }
    }

    public synchronized void abortTransaction() {
        LOG.debug("Aborting current transaction");
        Optional<ObjectName> taON = getTransaction();
        Preconditions.checkState(taON.isPresent(), NO_TRANSACTION_FOUND_FOR_SESSION + netconfSessionIdForReporting);

        ConfigTransactionClient transactionClient = configRegistryClient.getConfigTransactionClient(taON.get());
        transactionClient.abortConfig();
        allOpenedTransactions.remove(candidateTx);
        candidateTx = null;
    }

    public synchronized void closeReadTransaction() {
        LOG.debug("Closing read transaction");
        Optional<ObjectName> taON = getReadTransaction();
        Preconditions.checkState(taON.isPresent(), NO_TRANSACTION_FOUND_FOR_SESSION + netconfSessionIdForReporting);

        ConfigTransactionClient transactionClient = configRegistryClient.getConfigTransactionClient(taON.get());
        transactionClient.abortConfig();
        allOpenedTransactions.remove(readTx);
        readTx = null;
    }

    public synchronized void abortTestTransaction(ObjectName testTx) {
        LOG.debug("Aborting transaction {}", testTx);
        ConfigTransactionClient transactionClient = configRegistryClient.getConfigTransactionClient(testTx);
        allOpenedTransactions.remove(testTx);
        transactionClient.abortConfig();
    }

    public void validateTransaction() throws ValidationException {
        Optional<ObjectName> taON = getTransaction();
        Preconditions.checkState(taON.isPresent(), NO_TRANSACTION_FOUND_FOR_SESSION + netconfSessionIdForReporting);

        ConfigTransactionClient transactionClient = configRegistryClient.getConfigTransactionClient(taON.get());
        transactionClient.validateConfig();
    }

    public void validateTestTransaction(ObjectName taON) throws ValidationException {
        ConfigTransactionClient transactionClient = configRegistryClient.getConfigTransactionClient(taON);
        transactionClient.validateConfig();
    }

    public void wipeTestTransaction(ObjectName taON) {
        wipeInternal(taON, true);
    }

    /**
     * Wiping means removing all module instances keeping the transaction open + service references.
     */
    synchronized void wipeInternal(ObjectName taON, boolean isTest) {
        ConfigTransactionClient transactionClient = configRegistryClient.getConfigTransactionClient(taON);

        Set<ObjectName> lookupConfigBeans = transactionClient.lookupConfigBeans();
        int i = lookupConfigBeans.size();
        for (ObjectName instance : lookupConfigBeans) {
            try {
                transactionClient.destroyModule(instance);
            } catch (InstanceNotFoundException e) {
                if (isTest){
                    LOG.debug("Unable to clean configuration in transactiom {}", taON, e);
                } else {
                    LOG.warn("Unable to clean configuration in transactiom {}", taON, e);
                }

                throw new IllegalStateException("Unable to clean configuration in transactiom " + taON, e);
            }
        }
        LOG.debug("Transaction {} wiped clean of {} config beans", taON, i);

        transactionClient.removeAllServiceReferences();
        LOG.debug("Transaction {} wiped clean of all service references", taON);
    }

    public void wipeTransaction() {
        Optional<ObjectName> taON = getTransaction();
        Preconditions.checkState(taON.isPresent(), NO_TRANSACTION_FOUND_FOR_SESSION + netconfSessionIdForReporting);
        wipeInternal(taON.get(), false);
    }

}
