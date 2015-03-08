/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.binding.compat;

import com.google.common.base.Function;
import com.google.common.util.concurrent.AsyncFunction;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.ReadWriteTransaction;
import org.opendaylight.controller.md.sal.common.api.RegistrationListener;
import org.opendaylight.controller.md.sal.common.api.TransactionStatus;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataBroker.DataChangeScope;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeEvent;
import org.opendaylight.controller.md.sal.common.api.data.DataChangeEvent;
import org.opendaylight.controller.md.sal.common.api.data.DataCommitHandler;
import org.opendaylight.controller.md.sal.common.api.data.DataCommitHandler.DataCommitTransaction;
import org.opendaylight.controller.md.sal.common.api.data.DataCommitHandlerRegistration;
import org.opendaylight.controller.md.sal.common.api.data.DataReader;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.impl.service.AbstractDataTransaction;
import org.opendaylight.controller.sal.binding.api.data.DataChangeListener;
import org.opendaylight.controller.sal.binding.api.data.DataModificationTransaction;
import org.opendaylight.controller.sal.binding.api.data.DataProviderService;
import org.opendaylight.controller.sal.binding.codegen.impl.SingletonHolder;
import org.opendaylight.yangtools.concepts.AbstractObjectRegistration;
import org.opendaylight.yangtools.concepts.Delegator;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.concepts.Registration;
import org.opendaylight.yangtools.util.ListenerRegistry;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Deprecated
public class HydrogenDataBrokerAdapter implements DataProviderService, AutoCloseable {

    private static final Logger LOG = LoggerFactory.getLogger(HydrogenDataBrokerAdapter.class);

    private final ConcurrentHashMap<InstanceIdentifier<?>, CommitHandlerRegistrationImpl> commitHandlers = new ConcurrentHashMap<>();
    private final ListeningExecutorService executorService = SingletonHolder.getDefaultCommitExecutor();

    private final DataBroker delegate;

    public HydrogenDataBrokerAdapter(final DataBroker dataBroker) {
        delegate = dataBroker;
        LOG.info("ForwardedBackwardsCompatibleBroker started.");
    }

    @Override
    public DataModificationTransaction beginTransaction() {
        return new ForwardedBackwardsCompatibleTransacion(delegate.newReadWriteTransaction());
    }

    @Override
    public DataObject readConfigurationData(final InstanceIdentifier<? extends DataObject> path) {
        DataModificationTransaction tx = beginTransaction();
        return tx.readConfigurationData(path);
    }

    @Override
    public DataObject readOperationalData(final InstanceIdentifier<? extends DataObject> path) {
        DataModificationTransaction tx = beginTransaction();
        return tx.readOperationalData(path);
    }

    @Override
    public Registration registerCommitHandler(
            final InstanceIdentifier<? extends DataObject> path,
            final DataCommitHandler<InstanceIdentifier<? extends DataObject>, DataObject> commitHandler) {
        CommitHandlerRegistrationImpl reg = new CommitHandlerRegistrationImpl(path, commitHandler);
        commitHandlers.put(path, reg);
        return reg;
    }

    @Override
    @Deprecated
    public ListenerRegistration<RegistrationListener<DataCommitHandlerRegistration<InstanceIdentifier<? extends DataObject>, DataObject>>> registerCommitHandlerListener(
            final RegistrationListener<DataCommitHandlerRegistration<InstanceIdentifier<? extends DataObject>, DataObject>> commitHandlerListener) {
        throw new UnsupportedOperationException("Not supported contract.");
    }

    @Override
    public ListenerRegistration<DataChangeListener> registerDataChangeListener(
            final InstanceIdentifier<? extends DataObject> path, final DataChangeListener listener) {


        org.opendaylight.controller.md.sal.binding.api.DataChangeListener asyncOperListener = new BackwardsCompatibleOperationalDataChangeInvoker(listener);
        org.opendaylight.controller.md.sal.binding.api.DataChangeListener asyncCfgListener = new BackwardsCompatibleConfigurationDataChangeInvoker(listener);

        ListenerRegistration<org.opendaylight.controller.md.sal.binding.api.DataChangeListener> cfgReg = delegate.registerDataChangeListener(LogicalDatastoreType.CONFIGURATION, path, asyncCfgListener, DataChangeScope.SUBTREE);
        ListenerRegistration<org.opendaylight.controller.md.sal.binding.api.DataChangeListener> operReg = delegate.registerDataChangeListener(LogicalDatastoreType.OPERATIONAL, path, asyncOperListener, DataChangeScope.SUBTREE);

        return new LegacyListenerRegistration(listener,cfgReg,operReg);
    }

    @Override
    public Registration registerDataReader(
            final InstanceIdentifier<? extends DataObject> path,
            final DataReader<InstanceIdentifier<? extends DataObject>, DataObject> reader) {
        throw new UnsupportedOperationException("Data reader contract is not supported.");
    }

    public ListenableFuture<RpcResult<TransactionStatus>> commit(final ForwardedBackwardsCompatibleTransacion tx) {

        final List<DataCommitTransaction<InstanceIdentifier<? extends DataObject>, DataObject>> subTrans = new ArrayList<>();
        LOG.debug("Tx: {} Submitted.",tx.getIdentifier());
        ListenableFuture<Boolean> requestCommit = executorService.submit(new Callable<Boolean>() {

            @Override
            public Boolean call() throws Exception {
                try {
                    for (CommitHandlerRegistrationImpl handler : commitHandlers.values()) {

                        DataCommitTransaction<InstanceIdentifier<? extends DataObject>, DataObject> subTx = handler
                                .getInstance().requestCommit(tx);
                        subTrans.add(subTx);
                    }
                } catch (Exception e) {
                    LOG.error("Tx: {} Rollback.",tx.getIdentifier(),e);
                    for (DataCommitTransaction<InstanceIdentifier<? extends DataObject>, DataObject> subTx : subTrans) {
                        subTx.rollback();
                    }
                    return false;
                }
                LOG.debug("Tx: {} Can Commit True.",tx.getIdentifier());
                return true;
            }

        });

        ListenableFuture<RpcResult<TransactionStatus>> dataStoreCommit = Futures.transform(requestCommit, new AsyncFunction<Boolean, RpcResult<TransactionStatus>>() {

            @Override
            public ListenableFuture<RpcResult<TransactionStatus>> apply(final Boolean requestCommitSuccess) throws Exception {
                if(requestCommitSuccess) {
                    return AbstractDataTransaction.convertToLegacyCommitFuture(tx.delegate.submit());
                }
                return Futures.immediateFuture(RpcResultBuilder.<TransactionStatus>failed().withResult(TransactionStatus.FAILED).build());
            }
        });

        return Futures.transform(dataStoreCommit, new Function<RpcResult<TransactionStatus>,RpcResult<TransactionStatus>>() {
            @Override
            public RpcResult<TransactionStatus> apply(final RpcResult<TransactionStatus> input) {
                if(input.isSuccessful()) {
                    for(DataCommitTransaction<InstanceIdentifier<? extends DataObject>, DataObject> subTx : subTrans ) {
                        subTx.finish();
                    }
                } else {
                    LOG.error("Tx: {} Rollback - Datastore commit failed.",tx.getIdentifier());
                    for(DataCommitTransaction<InstanceIdentifier<? extends DataObject>, DataObject> subTx : subTrans ) {
                        subTx.rollback();
                    }
                }
                return input;
            }
        });
    }

    @Deprecated
    private class ForwardedBackwardsCompatibleTransacion implements DataModificationTransaction {

        private final ListenerRegistry<DataTransactionListener> listeners = ListenerRegistry.create();
        private final Map<InstanceIdentifier<? extends DataObject>, DataObject> updated = new HashMap<>();
        private final Map<InstanceIdentifier<? extends DataObject>, DataObject> created = new HashMap<>();
        private final Set<InstanceIdentifier<? extends DataObject>> removed = new HashSet<>();
        private final Map<InstanceIdentifier<? extends DataObject>, DataObject> original = new HashMap<>();
        private TransactionStatus status = TransactionStatus.NEW;

        private final Set<InstanceIdentifier<? extends DataObject>> posponedRemovedOperational = new HashSet<>();
        private final Set<InstanceIdentifier<? extends DataObject>> posponedRemovedConfiguration = new HashSet<>();

        private final ReadWriteTransaction delegate;


        @Override
        public final TransactionStatus getStatus() {
            return status;
        }

        protected ForwardedBackwardsCompatibleTransacion(final ReadWriteTransaction delegate) {
            this.delegate = delegate;
            LOG.debug("Tx {} allocated.",getIdentifier());
        }

        @Override
        public void putOperationalData(final InstanceIdentifier<? extends DataObject> path, final DataObject data) {
            boolean previouslyRemoved = posponedRemovedOperational.remove(path);

            @SuppressWarnings({ "rawtypes", "unchecked" })
            final InstanceIdentifier<DataObject> castedPath = (InstanceIdentifier) path;
            if(previouslyRemoved) {
                delegate.put(LogicalDatastoreType.OPERATIONAL, castedPath, data,true);
            } else {
                delegate.merge(LogicalDatastoreType.OPERATIONAL, castedPath, data,true);
            }
        }

        @Override
        public void putConfigurationData(final InstanceIdentifier<? extends DataObject> path, final DataObject data) {
            boolean previouslyRemoved = posponedRemovedConfiguration.remove(path);
            DataObject originalObj = readConfigurationData(path);
            if (originalObj != null) {
                original.put(path, originalObj);

            } else {
                created.put(path, data);
            }
            updated.put(path, data);
            @SuppressWarnings({"rawtypes","unchecked"})
            final InstanceIdentifier<DataObject> castedPath = (InstanceIdentifier) path;
            if(previouslyRemoved) {
                delegate.put(LogicalDatastoreType.CONFIGURATION, castedPath, data,true);
            } else {
                delegate.merge(LogicalDatastoreType.CONFIGURATION, castedPath, data,true);
            }
        }

        @Override
        public void removeOperationalData(final InstanceIdentifier<? extends DataObject> path) {
            posponedRemovedOperational.add(path);
        }

        @Override
        public void removeConfigurationData(final InstanceIdentifier<? extends DataObject> path) {
            posponedRemovedConfiguration.add(path);
        }

        @Override
        public Map<InstanceIdentifier<? extends DataObject>, DataObject> getCreatedOperationalData() {
            return Collections.emptyMap();
        }

        @Override
        public Map<InstanceIdentifier<? extends DataObject>, DataObject> getCreatedConfigurationData() {
            return created;
        }

        @Override
        public Map<InstanceIdentifier<? extends DataObject>, DataObject> getUpdatedOperationalData() {
            return Collections.emptyMap();
        }

        @Override
        public Map<InstanceIdentifier<? extends DataObject>, DataObject> getUpdatedConfigurationData() {
            return updated;
        }

        @Override
        public Set<InstanceIdentifier<? extends DataObject>> getRemovedConfigurationData() {
            return removed;
        }

        @Override
        public Set<InstanceIdentifier<? extends DataObject>> getRemovedOperationalData() {
            return Collections.emptySet();
        }

        @Override
        public Map<InstanceIdentifier<? extends DataObject>, DataObject> getOriginalConfigurationData() {
            return original;
        }

        @Override
        public Map<InstanceIdentifier<? extends DataObject>, DataObject> getOriginalOperationalData() {
            return Collections.emptyMap();
        }

        @Override
        public DataObject readOperationalData(final InstanceIdentifier<? extends DataObject> path) {
            try {
                return delegate.read(LogicalDatastoreType.OPERATIONAL, path).get().orNull();
            } catch (InterruptedException | ExecutionException e) {
                LOG.error("Read of {} failed.", path,e);
                return null;
            }
        }

        @Override
        public DataObject readConfigurationData(final InstanceIdentifier<? extends DataObject> path) {
            try {
                return delegate.read(LogicalDatastoreType.CONFIGURATION, path).get().orNull();
            } catch (InterruptedException | ExecutionException e) {
                LOG.error("Read of {} failed.", path,e);
                return null;
            }
        }

        private void changeStatus(final TransactionStatus status) {
            LOG.trace("Transaction {} changed status to {}", getIdentifier(), status);
            this.status = status;

            for(ListenerRegistration<DataTransactionListener> listener : listeners) {
                try {
                    listener.getInstance().onStatusUpdated(this, status);
                } catch (Exception e) {
                    LOG.error("Error during invoking transaction listener {}",listener.getInstance(),e);
                }
            }
        }

        @Override
        public ListenableFuture<RpcResult<TransactionStatus>> commit() {

            for(InstanceIdentifier<? extends DataObject> path : posponedRemovedConfiguration) {
                delegate.delete(LogicalDatastoreType.CONFIGURATION, path);
            }

            for(InstanceIdentifier<? extends DataObject> path : posponedRemovedOperational) {
                delegate.delete(LogicalDatastoreType.OPERATIONAL, path);
            }

            changeStatus(TransactionStatus.SUBMITED);

            final ListenableFuture<RpcResult<TransactionStatus>> f = HydrogenDataBrokerAdapter.this.commit(this);

            Futures.addCallback(f, new FutureCallback<RpcResult<TransactionStatus>>() {
                @Override
                public void onSuccess(final RpcResult<TransactionStatus> result) {
                    changeStatus(result.getResult());
                }

                @Override
                public void onFailure(final Throwable t) {
                    LOG.error("Transaction {} failed to complete", getIdentifier(), t);
                    changeStatus(TransactionStatus.FAILED);
                }
            });

            return f;
        }

        @Override
        public ListenerRegistration<DataTransactionListener> registerListener(final DataTransactionListener listener) {
            return listeners.register(listener);
        }

        @Override
        public Object getIdentifier() {
            // TODO Auto-generated method stub
            return null;
        }

    }

    private class CommitHandlerRegistrationImpl extends
            AbstractObjectRegistration<DataCommitHandler<InstanceIdentifier<? extends DataObject>, DataObject>> {

        private final InstanceIdentifier<? extends DataObject> path;

        public CommitHandlerRegistrationImpl(final InstanceIdentifier<? extends DataObject> path,
                final DataCommitHandler<InstanceIdentifier<? extends DataObject>, DataObject> commitHandler) {
            super(commitHandler);
            this.path = path;
        }

        @Override
        protected void removeRegistration() {
            commitHandlers.remove(path, this);
        }

    }


    private static final class LegacyListenerRegistration implements ListenerRegistration<DataChangeListener> {

        private final DataChangeListener instance;
        private final ListenerRegistration<org.opendaylight.controller.md.sal.binding.api.DataChangeListener> cfgReg;
        private final ListenerRegistration<org.opendaylight.controller.md.sal.binding.api.DataChangeListener> operReg;

        public LegacyListenerRegistration(final DataChangeListener listener,
                final ListenerRegistration<org.opendaylight.controller.md.sal.binding.api.DataChangeListener> cfgReg,
                final ListenerRegistration<org.opendaylight.controller.md.sal.binding.api.DataChangeListener> operReg) {
            this.instance = listener;
            this.cfgReg = cfgReg;
            this.operReg = operReg;
        }

        @Override
        public DataChangeListener getInstance() {
            return instance;
        }

        @Override
        public void close() {
            cfgReg.close();
            operReg.close();
        }

    }

    private static class BackwardsCompatibleOperationalDataChangeInvoker implements org.opendaylight.controller.md.sal.binding.api.DataChangeListener, Delegator<DataChangeListener> {

        private final org.opendaylight.controller.md.sal.common.api.data.DataChangeListener<?,?> delegate;


        public BackwardsCompatibleOperationalDataChangeInvoker(final DataChangeListener listener) {
            this.delegate = listener;
        }

        @SuppressWarnings({ "unchecked", "rawtypes" })
        @Override
        public void onDataChanged(final AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> change) {

            DataChangeEvent legacyChange = HydrogenDataChangeEvent.createOperational(change);
            delegate.onDataChanged(legacyChange);

        }

        @Override
        public DataChangeListener getDelegate() {
            return (DataChangeListener) delegate;
        }

    }

    private static class BackwardsCompatibleConfigurationDataChangeInvoker implements org.opendaylight.controller.md.sal.binding.api.DataChangeListener, Delegator<DataChangeListener> {
        private final org.opendaylight.controller.md.sal.common.api.data.DataChangeListener<?,?> delegate;

        public BackwardsCompatibleConfigurationDataChangeInvoker(final DataChangeListener listener) {
            this.delegate = listener;
        }

        @SuppressWarnings({ "unchecked", "rawtypes" })
        @Override
        public void onDataChanged(final AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> change) {

            DataChangeEvent legacyChange = HydrogenDataChangeEvent.createConfiguration(change);

            delegate.onDataChanged(legacyChange);

        }

        @Override
        public DataChangeListener getDelegate() {
            return (DataChangeListener) delegate;
        }

    }

    @Override
    public void close() throws Exception {
        // TODO Auto-generated method stub
    }
}
