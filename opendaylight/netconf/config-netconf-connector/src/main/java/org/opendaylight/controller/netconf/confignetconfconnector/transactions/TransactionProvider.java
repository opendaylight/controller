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
import org.opendaylight.controller.config.api.ValidationException;
import org.opendaylight.controller.config.api.jmx.CommitStatus;
import org.opendaylight.controller.config.util.ConfigRegistryClient;
import org.opendaylight.controller.config.util.ConfigTransactionClient;
import org.opendaylight.controller.netconf.api.NetconfDocumentedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.management.InstanceNotFoundException;
import javax.management.ObjectName;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class TransactionProvider implements AutoCloseable {
    private static final Logger logger = LoggerFactory.getLogger(TransactionProvider.class);

    private final ConfigRegistryClient configRegistryClient;

    private final String netconfSessionIdForReporting;
    private ObjectName transaction;
    private final List<ObjectName> allOpenedTransactions = new ArrayList<>();

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
                logger.debug("Ignoring exception while closing transaction {}", tx, e);
            }
        }
        allOpenedTransactions.clear();
    }

    public Optional<ObjectName> getTransaction() {

        if (transaction == null)
            return Optional.absent();

        // Transaction was already closed somehow
        if (isStillOpenTransaction(transaction) == false) {
            logger.warn("Fixing illegal state: transaction {} was closed in {}", transaction,
                    netconfSessionIdForReporting);
            transaction = null;
            return Optional.absent();
        }
        return Optional.of(transaction);
    }

    private boolean isStillOpenTransaction(ObjectName transaction) {
        boolean isStillOpenTransaction = configRegistryClient.getOpenConfigs().contains(transaction);
        return isStillOpenTransaction;
    }

    public synchronized ObjectName getOrCreateTransaction() {
        Optional<ObjectName> ta = getTransaction();

        if (ta.isPresent())
            return ta.get();
        transaction = configRegistryClient.beginConfig();
        allOpenedTransactions.add(transaction);
        return transaction;
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
    public synchronized CommitStatus commitTransaction() throws NetconfDocumentedException {
        final Optional<ObjectName> taON = getTransaction();
        Preconditions.checkState(taON.isPresent(), "No transaction found for session " + netconfSessionIdForReporting);
        CommitStatus status = configRegistryClient.commitConfig(taON.get());
        allOpenedTransactions.remove(transaction);
        transaction = null;
        return status;
    }

    public synchronized void abortTransaction() {
        Optional<ObjectName> taON = getTransaction();
        Preconditions.checkState(taON.isPresent(), "No transaction found for session " + netconfSessionIdForReporting);

        ConfigTransactionClient transactionClient = configRegistryClient.getConfigTransactionClient(taON.get());
        transactionClient.abortConfig();
        allOpenedTransactions.remove(transaction);
        transaction = null;
    }

    public synchronized void abortTestTransaction(ObjectName testTx) {
        ConfigTransactionClient transactionClient = configRegistryClient.getConfigTransactionClient(testTx);
        allOpenedTransactions.remove(testTx);
        transactionClient.abortConfig();
    }

    public void validateTransaction() throws ValidationException {
        Optional<ObjectName> taON = getTransaction();
        Preconditions.checkState(taON.isPresent(), "No transaction found for session " + netconfSessionIdForReporting);

        ConfigTransactionClient transactionClient = configRegistryClient.getConfigTransactionClient(taON.get());
        transactionClient.validateConfig();
    }

    public void validateTestTransaction(ObjectName taON) {
        ConfigTransactionClient transactionClient = configRegistryClient.getConfigTransactionClient(taON);
        transactionClient.validateConfig();
    }

    public void wipeTestTransaction(ObjectName taON) {
        wipeInternal(taON, true, null);
    }

    /**
     * Wiping means removing all module instances keeping the transaction open.
     */
    synchronized void wipeInternal(ObjectName taON, boolean isTest, String moduleName) {
        ConfigTransactionClient transactionClient = configRegistryClient.getConfigTransactionClient(taON);

        Set<ObjectName> lookupConfigBeans = moduleName == null ? transactionClient.lookupConfigBeans()
                : transactionClient.lookupConfigBeans(moduleName);
        for (ObjectName instance : lookupConfigBeans) {
            try {
                transactionClient.destroyModule(instance);
            } catch (InstanceNotFoundException e) {
                if (isTest)
                    logger.debug("Unable to clean configuration in transactiom {}", taON, e);
                else
                    logger.warn("Unable to clean configuration in transactiom {}", taON, e);

                throw new IllegalStateException("Unable to clean configuration in transactiom " + taON, e);
            }
        }
        logger.debug("Transaction {} wiped clean", taON);
    }

    public void wipeTransaction() {
        Optional<ObjectName> taON = getTransaction();
        Preconditions.checkState(taON.isPresent(), "No transaction found for session " + netconfSessionIdForReporting);
        wipeInternal(taON.get(), false, null);
    }

}
