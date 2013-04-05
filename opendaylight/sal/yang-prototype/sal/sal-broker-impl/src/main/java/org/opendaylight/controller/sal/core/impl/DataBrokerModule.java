/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.core.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import org.opendaylight.controller.sal.common.DataStoreIdentifier;
import org.opendaylight.controller.sal.common.util.Rpcs;
import org.opendaylight.controller.sal.core.api.BrokerService;
import org.opendaylight.controller.sal.core.api.Broker.ConsumerSession;
import org.opendaylight.controller.sal.core.api.Broker.ProviderSession;
import org.opendaylight.controller.sal.core.api.Consumer.ConsumerFunctionality;
import org.opendaylight.controller.sal.core.api.Provider.ProviderFunctionality;
import org.opendaylight.controller.sal.core.api.data.DataBrokerService;
import org.opendaylight.controller.sal.core.api.data.DataCommitHandler;
import org.opendaylight.controller.sal.core.api.data.DataProviderService;
import org.opendaylight.controller.sal.core.api.data.DataValidator;
import org.opendaylight.controller.sal.core.api.data.DataCommitHandler.CommitTransaction;
import org.opendaylight.controller.sal.core.api.data.DataProviderService.DataRefresher;
import org.opendaylight.controller.sal.core.spi.BrokerModule;
import org.opendaylight.controller.yang.common.RpcError;
import org.opendaylight.controller.yang.common.RpcResult;
import org.opendaylight.controller.yang.data.api.CompositeNode;
import org.opendaylight.controller.yang.data.api.CompositeNodeModification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableSet;

public class DataBrokerModule implements BrokerModule {

    private static final Logger log = LoggerFactory
            .getLogger(DataBrokerModule.class);

    private static final Set<Class<? extends ProviderFunctionality>> SUPPORTED_PROVIDER_FUNCTIONALITY = ImmutableSet
            .of((Class<? extends ProviderFunctionality>) DataValidator.class,
                    DataRefresher.class, DataCommitHandler.class);

    private static final Set<Class<? extends BrokerService>> PROVIDED_SESSION_SERVICES = ImmutableSet
            .of((Class<? extends BrokerService>) DataBrokerService.class,
                    DataProviderService.class);

    private Map<DataStoreIdentifier, StoreContext> storeContext;

    private ExecutorService executor;
    
    private SequentialCommitHandlerCoordinator coordinator = new SequentialCommitHandlerCoordinator();

    @Override
    public Set<Class<? extends BrokerService>> getProvidedServices() {
        return PROVIDED_SESSION_SERVICES;
    }

    @Override
    public Set<Class<? extends ProviderFunctionality>> getSupportedProviderFunctionality() {
        return SUPPORTED_PROVIDER_FUNCTIONALITY;
    }

    @Override
    public Set<Class<? extends ConsumerFunctionality>> getSupportedConsumerFunctionality() {
        return Collections.emptySet();
    }

    @Override
    public <T extends BrokerService> T getServiceForSession(Class<T> service,
            ConsumerSession session) {
        if (DataProviderService.class.equals(service)
                && session instanceof ProviderSession) {
            @SuppressWarnings("unchecked")
            T ret = (T) newDataProviderService(session);
            return ret;
        } else if (DataBrokerService.class.equals(service)) {

            @SuppressWarnings("unchecked")
            T ret = (T) newDataConsumerService(session);
            return ret;
        }

        throw new IllegalArgumentException(
                "The requested session-specific service is not provided by this module.");
    }

    private DataProviderService newDataProviderService(ConsumerSession session) {
        return new DataProviderSession();
    }

    private DataBrokerService newDataConsumerService(ConsumerSession session) {
        return new DataConsumerSession();
    }

    private StoreContext context(DataStoreIdentifier store) {
        return storeContext.get(store);
    }

    private static class StoreContext {
        private Set<DataCommitHandler> commitHandlers = Collections
                .synchronizedSet(new HashSet<DataCommitHandler>());
        private Set<DataValidator> validators = Collections
                .synchronizedSet(new HashSet<DataValidator>());
        private Set<DataRefresher> refreshers = Collections
                .synchronizedSet(new HashSet<DataRefresher>());
    }

    private class DataConsumerSession implements DataBrokerService {

        @Override
        public CompositeNode getData(DataStoreIdentifier store) {
            // TODO Implement this method
            throw new UnsupportedOperationException("Not implemented");
        }

        @Override
        public CompositeNode getData(DataStoreIdentifier store,
                CompositeNode filter) {
            // TODO Implement this method
            throw new UnsupportedOperationException("Not implemented");
        }

        @Override
        public CompositeNode getCandidateData(DataStoreIdentifier store) {
            // TODO Implement this method
            throw new UnsupportedOperationException("Not implemented");
        }

        @Override
        public CompositeNode getCandidateData(DataStoreIdentifier store,
                CompositeNode filter) {
            // TODO Implement this method
            throw new UnsupportedOperationException("Not implemented");
        }

        @Override
        public RpcResult<CompositeNode> editCandidateData(
                DataStoreIdentifier store, CompositeNodeModification changeSet) {
            // TODO Implement this method
            throw new UnsupportedOperationException("Not implemented");
        }

        @Override
        public Future<RpcResult<Void>> commit(DataStoreIdentifier store) {
            // TODO Implement this method
            throw new UnsupportedOperationException("Not implemented");
        }

        @Override
        public void closeSession() {
            // TODO Implement this method
            throw new UnsupportedOperationException("Not implemented");
        }

        @Override
        public Set<DataStoreIdentifier> getDataStores() {
            // TODO Auto-generated method stub
            return null;
        }

    }

    private class DataProviderSession extends DataConsumerSession implements
            DataProviderService {

        private Set<DataCommitHandler> providerCommitHandlers = new HashSet<DataCommitHandler>();
        private Set<DataValidator> providerValidators = new HashSet<DataValidator>();
        private Set<DataRefresher> providerRefreshers = new HashSet<DataRefresher>();

        @Override
        public void addValidator(DataStoreIdentifier store,
                DataValidator validator) {
            if (validator == null)
                throw new IllegalArgumentException(
                        "Validator should not be null");

            providerValidators.add(validator);
            context(store).validators.add(validator);
        }

        @Override
        public void removeValidator(DataStoreIdentifier store,
                DataValidator validator) {
            if (validator == null)
                throw new IllegalArgumentException(
                        "Validator should not be null");

            providerValidators.remove(validator);
            context(store).validators.remove(validator);
        }

        @Override
        public void addCommitHandler(DataStoreIdentifier store,
                DataCommitHandler provider) {
            if (provider == null)
                throw new IllegalArgumentException(
                        "CommitHandler should not be null");

            providerCommitHandlers.add(provider);
            context(store).commitHandlers.add(provider);
        }

        @Override
        public void removeCommitHandler(DataStoreIdentifier store,
                DataCommitHandler provider) {
            if (provider == null)
                throw new IllegalArgumentException(
                        "CommitHandler should not be null");

            providerCommitHandlers.remove(provider);
            context(store).commitHandlers.remove(provider);
        }

        @Override
        public void addRefresher(DataStoreIdentifier store,
                DataRefresher provider) {
            if (provider == null)
                throw new IllegalArgumentException(
                        "Refresher should not be null");

            providerRefreshers.add(provider);
            context(store).refreshers.add(provider);
        }

        @Override
        public void removeRefresher(DataStoreIdentifier store,
                DataRefresher provider) {
            if (provider == null)
                throw new IllegalArgumentException(
                        "Refresher should not be null");

            providerRefreshers.remove(provider);
            context(store).refreshers.remove(provider);
        }

    }

    private class SequentialCommitHandlerCoordinator implements
            DataCommitHandler {

        @Override
        public RpcResult<CommitTransaction> requestCommit(
                DataStoreIdentifier store) {
            List<RpcError> errors = new ArrayList<RpcError>();
            Set<CommitTransaction> transactions = new HashSet<DataCommitHandler.CommitTransaction>();
            boolean successful = true;

            for (DataCommitHandler commitHandler : context(store).commitHandlers) {
                try {
                    RpcResult<CommitTransaction> partialResult = commitHandler
                            .requestCommit(store);
                    successful = partialResult.isSuccessful() & successful;
                    if (partialResult.isSuccessful()) {
                        transactions.add(partialResult.getResult());
                    }

                    errors.addAll(partialResult.getErrors());
                } catch (Exception e) {
                    log.error("Uncaught exception prevented commit request."
                            + e.getMessage(), e);
                    successful = false;
                    // FIXME: Add RPC Error with exception.
                }
                if (successful == false)
                    break;
            }
            CommitTransaction transaction = new SequentialCommitTransaction(
                    store, transactions);
            return Rpcs.getRpcResult(successful, transaction, errors);
        }

        @Override
        public Set<DataStoreIdentifier> getSupportedDataStores() {
            return Collections.emptySet();
        }
    }

    private class SequentialCommitTransaction implements CommitTransaction {

        final Set<CommitTransaction> transactions;
        final DataStoreIdentifier store;

        public SequentialCommitTransaction(DataStoreIdentifier s,
                Set<CommitTransaction> t) {
            transactions = t;
            store = s;
        }

        @Override
        public RpcResult<Void> finish() {
            List<RpcError> errors = new ArrayList<RpcError>();
            boolean successful = true;

            for (CommitTransaction commitHandler : transactions) {
                try {
                    RpcResult<Void> partialResult = commitHandler.finish();
                    successful = partialResult.isSuccessful() & successful;
                    errors.addAll(partialResult.getErrors());
                } catch (Exception e) {
                    log.error(
                            "Uncaught exception prevented finishing of commit."
                                    + e.getMessage(), e);
                    successful = false;
                    // FIXME: Add RPC Error with exception.
                }
                if (successful == false)
                    break;
            }

            return Rpcs.getRpcResult(successful, null, errors);
        }

        @Override
        public RpcResult<Void> rollback() {
            List<RpcError> errors = new ArrayList<RpcError>();
            boolean successful = true;

            for (CommitTransaction commitHandler : transactions) {
                try {
                    RpcResult<Void> partialResult = commitHandler.rollback();
                    successful = partialResult.isSuccessful() & successful;
                    errors.addAll(partialResult.getErrors());
                } catch (Exception e) {
                    log.error(
                            "Uncaught exception prevented rollback of commit."
                                    + e.getMessage(), e);
                    successful = false;
                    // FIXME: Add RPC Error with exception.
                }
                if (successful == false)
                    break;
            }

            return Rpcs.getRpcResult(successful, null, errors);
        }

        @Override
        public DataStoreIdentifier getDataStore() {
            return this.store;
        }

        @Override
        public DataCommitHandler getHandler() {
            return coordinator;
        }
    }

    private class ValidationCoordinator implements DataValidator {

        private final DataStoreIdentifier store;

        ValidationCoordinator(DataStoreIdentifier store) {
            this.store = store;
        }

        @Override
        public RpcResult<Void> validate(CompositeNode toValidate) {
            List<RpcError> errors = new ArrayList<RpcError>();
            boolean successful = true;

            for (DataValidator validator : context(store).validators) {
                try {
                    RpcResult<Void> partialResult = validator
                            .validate(toValidate);
                    successful = partialResult.isSuccessful() & successful;
                    errors.addAll(partialResult.getErrors());
                } catch (Exception e) {
                    log.error(
                            "Uncaught exception prevented validation."
                                    + e.getMessage(), e);
                    successful = false;
                    // FIXME: Add RPC Error with exception.
                }
                if (successful == false)
                    break;
            }

            return Rpcs.getRpcResult(successful, null, errors);
        }

        @Override
        public Set<DataStoreIdentifier> getSupportedDataStores() {
            return Collections.emptySet();
        }

    }

    private class DataRefreshCoordinator implements DataRefresher {

        private final DataStoreIdentifier store;

        DataRefreshCoordinator(DataStoreIdentifier store) {
            this.store = store;
        }

        @Override
        public void refreshData() {

            for (DataRefresher refresher : context(store).refreshers) {
                try {
                    refresher.refreshData();
                } catch (Exception e) {
                    log.error(
                            "Uncaught exception during refresh of data: "
                                    + e.getMessage(), e);
                }

            }
        }
    }
}
