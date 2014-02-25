/**
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.common.impl.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;

import org.opendaylight.controller.md.sal.common.api.TransactionStatus;
import org.opendaylight.controller.md.sal.common.api.data.DataChangeEvent;
import org.opendaylight.controller.md.sal.common.api.data.DataChangeListener;
import org.opendaylight.controller.md.sal.common.api.data.DataCommitHandler;
import org.opendaylight.controller.md.sal.common.api.data.DataCommitHandler.DataCommitTransaction;
import org.opendaylight.controller.sal.common.util.Rpcs;
import org.opendaylight.yangtools.concepts.Path;
import org.opendaylight.yangtools.yang.common.RpcError;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;

public class TwoPhaseCommit<P extends Path<P>, D extends Object, DCL extends DataChangeListener<P, D>> implements
        Callable<RpcResult<TransactionStatus>> {
    private final static Logger log = LoggerFactory.getLogger(TwoPhaseCommit.class);

    private final AbstractDataTransaction<P, D> transaction;

    private final AbstractDataBroker<P, D, DCL> dataBroker;

    public TwoPhaseCommit(final AbstractDataTransaction<P, D> transaction, final AbstractDataBroker<P, D, DCL> broker) {
        this.transaction = transaction;
        this.dataBroker = broker;
    }

    @Override
    public RpcResult<TransactionStatus> call() throws Exception {
        final Object transactionId = this.transaction.getIdentifier();

        Set<P> changedPaths = ImmutableSet.<P> builder().addAll(transaction.getUpdatedConfigurationData().keySet())
                .addAll(transaction.getCreatedConfigurationData().keySet())
                .addAll(transaction.getRemovedConfigurationData())
                .addAll(transaction.getUpdatedOperationalData().keySet())
                .addAll(transaction.getCreatedOperationalData().keySet())
                .addAll(transaction.getRemovedOperationalData()).build();

        log.trace("Transaction: {} Affected Subtrees: {}", transactionId, changedPaths);

        // The transaction has no effects, let's just shortcut it
        if (changedPaths.isEmpty()) {
            dataBroker.getFinishedTransactionsCount().getAndIncrement();
            transaction.changeStatus(TransactionStatus.COMMITED);

            log.trace("Transaction: {} Finished successfully (no effects).", transactionId);

            return Rpcs.<TransactionStatus> getRpcResult(true, TransactionStatus.COMMITED,
                    Collections.<RpcError> emptySet());
        }

        final ImmutableList.Builder<ListenerStateCapture<P, D, DCL>> listenersBuilder = ImmutableList.builder();
        listenersBuilder.addAll(dataBroker.affectedListeners(changedPaths));
        filterProbablyAffectedListeners(dataBroker.probablyAffectedListeners(changedPaths),listenersBuilder);



        final ImmutableList<ListenerStateCapture<P, D, DCL>> listeners = listenersBuilder.build();
        final Iterable<DataCommitHandler<P, D>> commitHandlers = dataBroker.affectedCommitHandlers(changedPaths);
        captureInitialState(listeners);


        log.trace("Transaction: {} Starting Request Commit.",transactionId);
        final List<DataCommitTransaction<P, D>> handlerTransactions = new ArrayList<>();
        try {
            for (final DataCommitHandler<P, D> handler : commitHandlers) {
                DataCommitTransaction<P, D> requestCommit = handler.requestCommit(this.transaction);
                if (requestCommit != null) {
                    handlerTransactions.add(requestCommit);
                } else {
                    log.debug("Transaction: {}, Handler {}  is not participating in transaction.", transactionId,
                            handler);
                }
            }
        } catch (Exception e) {
            log.error("Transaction: {} Request Commit failed", transactionId, e);
            dataBroker.getFailedTransactionsCount().getAndIncrement();
            this.transaction.changeStatus(TransactionStatus.FAILED);
            return this.rollback(handlerTransactions, e);

        }

        log.trace("Transaction: {} Starting Finish.",transactionId);
        final List<RpcResult<Void>> results = new ArrayList<RpcResult<Void>>();
        try {
            for (final DataCommitTransaction<P, D> subtransaction : handlerTransactions) {
                results.add(subtransaction.finish());
            }
        } catch (Exception e) {
            log.error("Transaction: {} Finish Commit failed", transactionId, e);
            dataBroker.getFailedTransactionsCount().getAndIncrement();
            transaction.changeStatus(TransactionStatus.FAILED);
            return this.rollback(handlerTransactions, e);
        }


        dataBroker.getFinishedTransactionsCount().getAndIncrement();
        transaction.changeStatus(TransactionStatus.COMMITED);

        log.trace("Transaction: {} Finished successfully.", transactionId);

        captureFinalState(listeners);

        log.trace("Transaction: {} Notifying listeners.", transactionId);

        publishDataChangeEvent(listeners);
        return Rpcs.<TransactionStatus> getRpcResult(true, TransactionStatus.COMMITED,
                Collections.<RpcError> emptySet());
    }

    private void captureInitialState(ImmutableList<ListenerStateCapture<P, D, DCL>> listeners) {
        for (ListenerStateCapture<P, D, DCL> state : listeners) {
            state.setInitialConfigurationState(dataBroker.readConfigurationData(state.getPath()));
            state.setInitialOperationalState(dataBroker.readOperationalData(state.getPath()));
        }
    }


    private void captureFinalState(ImmutableList<ListenerStateCapture<P, D, DCL>> listeners) {
        for (ListenerStateCapture<P, D, DCL> state : listeners) {
            state.setFinalConfigurationState(dataBroker.readConfigurationData(state.getPath()));
            state.setFinalOperationalState(dataBroker.readOperationalData(state.getPath()));
        }
    }

    private void filterProbablyAffectedListeners(
            ImmutableList<ListenerStateCapture<P, D, DCL>> probablyAffectedListeners, Builder<ListenerStateCapture<P, D, DCL>> reallyAffected) {

        for(ListenerStateCapture<P, D, DCL> listenerSet : probablyAffectedListeners) {
            P affectedPath = listenerSet.getPath();
            Optional<RootedChangeSet<P,D>> configChange = resolveConfigChange(affectedPath);
            Optional<RootedChangeSet<P, D>> operChange = resolveOperChange(affectedPath);

            if(configChange.isPresent() || operChange.isPresent()) {
                reallyAffected.add(listenerSet);
                if(configChange.isPresent()) {
                    listenerSet.setNormalizedConfigurationChanges(configChange.get());
                }

                if(operChange.isPresent()) {
                    listenerSet.setNormalizedOperationalChanges(operChange.get());
                }
            }
        }
    }

    private Optional<RootedChangeSet<P, D>> resolveOperChange(P affectedPath) {
        Map<P, D> originalOper = dataBroker.deepGetBySubpath(transaction.getOriginalOperationalData(),affectedPath);
        Map<P, D> createdOper = dataBroker.deepGetBySubpath(transaction.getCreatedOperationalData(),affectedPath);
        Map<P, D> updatedOper = dataBroker.deepGetBySubpath(transaction.getUpdatedOperationalData(),affectedPath);
        Set<P> removedOper = Sets.filter(transaction.getRemovedOperationalData(), dataBroker.createIsContainedPredicate(affectedPath));
        return resolveChanges(affectedPath,originalOper,createdOper,updatedOper,removedOper);
    }

    private Optional<RootedChangeSet<P, D>> resolveConfigChange(P affectedPath) {
        Map<P, D> originalConfig = dataBroker.deepGetBySubpath(transaction.getOriginalConfigurationData(),affectedPath);
        Map<P, D> createdConfig = dataBroker.deepGetBySubpath(transaction.getCreatedConfigurationData(),affectedPath);
        Map<P, D> updatedConfig = dataBroker.deepGetBySubpath(transaction.getUpdatedConfigurationData(),affectedPath);
        Set<P> removedConfig = Sets.filter(transaction.getRemovedConfigurationData(), dataBroker.createIsContainedPredicate(affectedPath));
        return resolveChanges(affectedPath,originalConfig,createdConfig,updatedConfig,removedConfig);
    }

    private Optional<RootedChangeSet<P,D>> resolveChanges(P affectedPath, Map<P, D> originalConfig, Map<P, D> createdConfig, Map<P, D> updatedConfig,Set<P> potentialDeletions) {
        Predicate<P> isContained = dataBroker.createIsContainedPredicate(affectedPath);

        if(createdConfig.isEmpty() && updatedConfig.isEmpty() && potentialDeletions.isEmpty()) {
            return Optional.absent();
        }
        RootedChangeSet<P, D> changeSet = new RootedChangeSet<P,D>(affectedPath,originalConfig);
        changeSet.addCreated(createdConfig);

        for(Entry<P, D> entry : updatedConfig.entrySet()) {
            if(originalConfig.containsKey(entry.getKey())) {
                changeSet.addUpdated(entry);
            } else {
                changeSet.addCreated(entry);
            }
        }

        for(Entry<P,D> entry : originalConfig.entrySet()) {
            for(P deletion : potentialDeletions) {
                if(isContained.apply(deletion)) {
                    changeSet.addRemoval(entry.getKey());
                }
            }
        }

        if(changeSet.isChange()) {
            return Optional.of(changeSet);
        } else {
            return Optional.absent();
        }

    }

    public void publishDataChangeEvent(final ImmutableList<ListenerStateCapture<P, D, DCL>> listeners) {
        ExecutorService executor = this.dataBroker.getExecutor();
        final Runnable notifyTask = new Runnable() {
            @Override
            public void run() {
                for (final ListenerStateCapture<P, D, DCL> listenerSet : listeners) {
                    DataChangeEvent<P, D> changeEvent = listenerSet.createEvent(transaction);
                    for (final DataChangeListenerRegistration<P, D, DCL> listener : listenerSet.getListeners()) {
                        try {
                            listener.getInstance().onDataChanged(changeEvent);
                        } catch (Exception e) {
                            log.error("Unhandled exception when invoking listener {}", listener, e);
                        }
                    }
                }
            }
        };
        executor.submit(notifyTask);
    }

    public RpcResult<TransactionStatus> rollback(final List<DataCommitTransaction<P, D>> transactions, final Exception e) {
        for (final DataCommitTransaction<P, D> transaction : transactions) {
            transaction.rollback();
        }
        Set<RpcError> _emptySet = Collections.<RpcError> emptySet();
        return Rpcs.<TransactionStatus> getRpcResult(false, TransactionStatus.FAILED, _emptySet);
    }
}
