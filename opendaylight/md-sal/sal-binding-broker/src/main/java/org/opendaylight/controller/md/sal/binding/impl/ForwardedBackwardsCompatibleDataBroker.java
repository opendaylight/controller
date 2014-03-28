package org.opendaylight.controller.md.sal.binding.impl;

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

import org.opendaylight.controller.md.sal.binding.api.BindingDataChangeListener;
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
import org.opendaylight.controller.md.sal.dom.api.DOMDataBroker;
import org.opendaylight.controller.md.sal.dom.api.DOMDataReadWriteTransaction;
import org.opendaylight.controller.sal.binding.api.data.DataChangeListener;
import org.opendaylight.controller.sal.binding.api.data.DataModificationTransaction;
import org.opendaylight.controller.sal.binding.api.data.DataProviderService;
import org.opendaylight.controller.sal.common.util.Rpcs;
import org.opendaylight.yangtools.concepts.AbstractObjectRegistration;
import org.opendaylight.yangtools.concepts.Delegator;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.concepts.Registration;
import org.opendaylight.yangtools.concepts.util.ListenerRegistry;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.RpcError;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.data.impl.codec.BindingIndependentMappingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Function;
import com.google.common.util.concurrent.AsyncFunction;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;

public class ForwardedBackwardsCompatibleDataBroker extends AbstractForwardedDataBroker implements DataProviderService, AutoCloseable {

    private static final Logger LOG = LoggerFactory.getLogger(ForwardedBackwardsCompatibleDataBroker.class);

    private final ConcurrentHashMap<InstanceIdentifier<?>, CommitHandlerRegistrationImpl> commitHandlers = new ConcurrentHashMap<>();
    private final ListenerRegistry<DataChangeListener> fakeRegistry = ListenerRegistry.create();
    private final ListeningExecutorService executorService;

    public ForwardedBackwardsCompatibleDataBroker(final DOMDataBroker domDataBroker,
            final BindingIndependentMappingService mappingService, final ListeningExecutorService executor) {
        super(domDataBroker, mappingService);
        executorService = executor;
        LOG.info("ForwardedBackwardsCompatibleBroker started.");
    }

    @Override
    public DataModificationTransaction beginTransaction() {
        return new ForwardedBackwardsCompatibleTransacion(getDelegate().newReadWriteTransaction(), getCodec());
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
    public Registration<DataCommitHandler<InstanceIdentifier<? extends DataObject>, DataObject>> registerCommitHandler(
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


        BindingDataChangeListener asyncOperListener = new BackwardsCompatibleOperationalDataChangeInvoker(listener);
        BindingDataChangeListener asyncCfgListener = new BackwardsCompatibleConfigurationDataChangeInvoker(listener);

        ListenerRegistration<BindingDataChangeListener> cfgReg = registerDataChangeListener(LogicalDatastoreType.CONFIGURATION, path, asyncOperListener, DataChangeScope.SUBTREE);
        ListenerRegistration<BindingDataChangeListener> operReg = registerDataChangeListener(LogicalDatastoreType.OPERATIONAL, path, asyncCfgListener, DataChangeScope.SUBTREE);

        return new LegacyListenerRegistration(listener,cfgReg,operReg);
    }

    @Override
    public Registration<DataReader<InstanceIdentifier<? extends DataObject>, DataObject>> registerDataReader(
            final InstanceIdentifier<? extends DataObject> path,
            final DataReader<InstanceIdentifier<? extends DataObject>, DataObject> reader) {
        throw new UnsupportedOperationException("Data reader contract is not supported.");
    }

    @Override
    public void close() throws Exception {
        // TODO Auto-generated method stub

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
                    return tx.getDelegate().commit();
                }
                return Futures.immediateFuture(Rpcs.getRpcResult(false, TransactionStatus.FAILED, Collections.<RpcError>emptySet()));
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

    private class ForwardedBackwardsCompatibleTransacion extends
            AbstractForwardedTransaction<DOMDataReadWriteTransaction> implements DataModificationTransaction {

        private final Map<InstanceIdentifier<? extends DataObject>, DataObject> updated = new HashMap<>();
        private final Map<InstanceIdentifier<? extends DataObject>, DataObject> created = new HashMap<>();
        private final Set<InstanceIdentifier<? extends DataObject>> removed = new HashSet<>();
        private final Map<InstanceIdentifier<? extends DataObject>, DataObject> original = new HashMap<>();

        @Override
        public TransactionStatus getStatus() {
            return null;
        }

        protected ForwardedBackwardsCompatibleTransacion(final DOMDataReadWriteTransaction delegate,
                final BindingToNormalizedNodeCodec codec) {
            super(delegate, codec);
            LOG.debug("Tx {} allocated.",getIdentifier());
        }

        @Override
        public void putOperationalData(final InstanceIdentifier<? extends DataObject> path, final DataObject data) {

            doPutWithEnsureParents(getDelegate(), LogicalDatastoreType.OPERATIONAL, path, data);
        }

        @Override
        public void putConfigurationData(final InstanceIdentifier<? extends DataObject> path, final DataObject data) {
            DataObject originalObj = readConfigurationData(path);
            if (originalObj != null) {
                original.put(path, originalObj);

            } else {
                created.put(path, data);
            }
            updated.put(path, data);
            doPutWithEnsureParents(getDelegate(), LogicalDatastoreType.CONFIGURATION, path, data);
        }

        @Override
        public void removeOperationalData(final InstanceIdentifier<? extends DataObject> path) {
            doDelete(getDelegate(), LogicalDatastoreType.OPERATIONAL, path);

        }

        @Override
        public void removeConfigurationData(final InstanceIdentifier<? extends DataObject> path) {
            doDelete(getDelegate(), LogicalDatastoreType.CONFIGURATION, path);
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
                return doRead(getDelegate(), LogicalDatastoreType.OPERATIONAL, path).get().orNull();
            } catch (InterruptedException | ExecutionException e) {
                LOG.error("Read of {} failed.", path,e);
                return null;
            }
        }

        @Override
        public DataObject readConfigurationData(final InstanceIdentifier<? extends DataObject> path) {
            try {
                return doRead(getDelegate(), LogicalDatastoreType.CONFIGURATION, path).get().orNull();
            } catch (InterruptedException | ExecutionException e) {
                LOG.error("Read of {} failed.", path,e);
                return null;
            }
        }

        @Override
        public Object getIdentifier() {
            return getDelegate().getIdentifier();
        }

        @Override
        public ListenableFuture<RpcResult<TransactionStatus>> commit() {
            return ForwardedBackwardsCompatibleDataBroker.this.commit(this);
        }

        @Override
        public ListenerRegistration<DataTransactionListener> registerListener(final DataTransactionListener listener) {
            throw new UnsupportedOperationException();
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
        private final ListenerRegistration<BindingDataChangeListener> cfgReg;
        private final ListenerRegistration<BindingDataChangeListener> operReg;

        public LegacyListenerRegistration(final DataChangeListener listener,
                final ListenerRegistration<BindingDataChangeListener> cfgReg,
                final ListenerRegistration<BindingDataChangeListener> operReg) {
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

    private static class BackwardsCompatibleOperationalDataChangeInvoker implements BindingDataChangeListener, Delegator<DataChangeListener> {

        private final org.opendaylight.controller.md.sal.common.api.data.DataChangeListener<?,?> delegate;


        public BackwardsCompatibleOperationalDataChangeInvoker(final DataChangeListener listener) {
            this.delegate = listener;
        }

        @SuppressWarnings({ "unchecked", "rawtypes" })
        @Override
        public void onDataChanged(final AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> change) {

            DataChangeEvent legacyChange = LegacyDataChangeEvent.createOperational(change);
            delegate.onDataChanged(legacyChange);

        }

        @Override
        public DataChangeListener getDelegate() {
            return (DataChangeListener) delegate;
        }

    }

    private static class BackwardsCompatibleConfigurationDataChangeInvoker implements BindingDataChangeListener, Delegator<DataChangeListener> {


        @SuppressWarnings("rawtypes")
        private final org.opendaylight.controller.md.sal.common.api.data.DataChangeListener<?,?> delegate;

        public BackwardsCompatibleConfigurationDataChangeInvoker(final DataChangeListener listener) {
            this.delegate = listener;
        }

        @SuppressWarnings({ "unchecked", "rawtypes" })
        @Override
        public void onDataChanged(final AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> change) {

            DataChangeEvent legacyChange = LegacyDataChangeEvent.createOperational(change);
            delegate.onDataChanged(legacyChange);

        }

        @Override
        public DataChangeListener getDelegate() {
            return (DataChangeListener) delegate;
        }

    }
}
