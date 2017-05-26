/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.binding.compat;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.ReadWriteTransaction;
import org.opendaylight.controller.md.sal.common.api.TransactionStatus;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataBroker.DataChangeScope;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeEvent;
import org.opendaylight.controller.md.sal.common.api.data.DataChangeEvent;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.sal.binding.api.data.DataChangeListener;
import org.opendaylight.controller.sal.binding.api.data.DataModificationTransaction;
import org.opendaylight.controller.sal.binding.api.data.DataProviderService;
import org.opendaylight.yangtools.concepts.Delegator;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.util.ListenerRegistry;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Deprecated
public class HydrogenDataBrokerAdapter implements DataProviderService, AutoCloseable {

    private static final Logger LOG = LoggerFactory.getLogger(HydrogenDataBrokerAdapter.class);

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
        final DataModificationTransaction tx = beginTransaction();
        return tx.readConfigurationData(path);
    }

    @Override
    public DataObject readOperationalData(final InstanceIdentifier<? extends DataObject> path) {
        final DataModificationTransaction tx = beginTransaction();
        return tx.readOperationalData(path);
    }

    @Override
    public ListenerRegistration<DataChangeListener> registerDataChangeListener(
            final InstanceIdentifier<? extends DataObject> path, final DataChangeListener listener) {


        final org.opendaylight.controller.md.sal.binding.api.DataChangeListener asyncOperListener = new BackwardsCompatibleOperationalDataChangeInvoker(listener);
        final org.opendaylight.controller.md.sal.binding.api.DataChangeListener asyncCfgListener = new BackwardsCompatibleConfigurationDataChangeInvoker(listener);

        final ListenerRegistration<org.opendaylight.controller.md.sal.binding.api.DataChangeListener> cfgReg = delegate.registerDataChangeListener(LogicalDatastoreType.CONFIGURATION, path, asyncCfgListener, DataChangeScope.SUBTREE);
        final ListenerRegistration<org.opendaylight.controller.md.sal.binding.api.DataChangeListener> operReg = delegate.registerDataChangeListener(LogicalDatastoreType.OPERATIONAL, path, asyncOperListener, DataChangeScope.SUBTREE);

        return new LegacyListenerRegistration(listener,cfgReg,operReg);
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
            final boolean previouslyRemoved = posponedRemovedOperational.remove(path);

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
            final boolean previouslyRemoved = posponedRemovedConfiguration.remove(path);
            final DataObject originalObj = readConfigurationData(path);
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

            for(final ListenerRegistration<DataTransactionListener> listener : listeners) {
                try {
                    listener.getInstance().onStatusUpdated(this, status);
                } catch (final Exception e) {
                    LOG.error("Error during invoking transaction listener {}",listener.getInstance(),e);
                }
            }
        }

        @Override
        public ListenableFuture<RpcResult<TransactionStatus>> commit() {

            for(final InstanceIdentifier<? extends DataObject> path : posponedRemovedConfiguration) {
                delegate.delete(LogicalDatastoreType.CONFIGURATION, path);
            }

            for(final InstanceIdentifier<? extends DataObject> path : posponedRemovedOperational) {
                delegate.delete(LogicalDatastoreType.OPERATIONAL, path);
            }

            changeStatus(TransactionStatus.SUBMITED);

            final ListenableFuture<RpcResult<TransactionStatus>> f = delegate.commit();

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

            final DataChangeEvent legacyChange = HydrogenDataChangeEvent.createOperational(change);
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

            final DataChangeEvent legacyChange = HydrogenDataChangeEvent.createConfiguration(change);

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
