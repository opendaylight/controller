package org.opendaylight.controller.sal.binding.impl;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.ListenableFuture;
import org.eclipse.xtext.xbase.lib.Exceptions;
import org.opendaylight.controller.md.sal.binding.api.BindingDataBroker;
import org.opendaylight.controller.md.sal.binding.api.BindingDataChangeListener;
import org.opendaylight.controller.md.sal.binding.api.BindingDataReadTransaction;
import org.opendaylight.controller.md.sal.binding.api.BindingDataReadWriteTransaction;
import org.opendaylight.controller.md.sal.binding.api.BindingDataWriteTransaction;
import org.opendaylight.controller.md.sal.common.api.TransactionStatus;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeEvent;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.dom.api.DOMDataBroker;
import org.opendaylight.controller.md.sal.dom.api.DOMDataChangeListener;
import org.opendaylight.controller.md.sal.dom.api.DOMDataReadTransaction;
import org.opendaylight.controller.md.sal.dom.api.DOMDataReadWriteTransaction;
import org.opendaylight.controller.md.sal.dom.api.DOMDataWriteTransaction;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.data.api.CompositeNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.impl.codec.BindingIndependentMappingService;
import org.opendaylight.yangtools.yang.data.impl.codec.DeserializationException;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;


/**
 * The ForwardingDataBrokerImpl simply defers to the DOMDataBroker for all its operations. All transactions and
 * listener registrations are wrapped by the ForwardingDataBrokerImpl to allow binding aware components to use the
 * DataBroker transparently.
 *
 * Besides this the ForwardingDataBrokerImpl and it's collaborators also cache data that is already transformed from
 * the binding independent to binding aware format
 */
public class ForwardingDataBrokerImpl implements BindingDataBroker{

    // Mapper to convert from Binding Independent objects to Binding Aware objects
    private BindingIndependentMappingService mappingService;

    // The Broker to whom we do all forwarding
    private DOMDataBroker domDataBroker;


    public void setMappingService(BindingIndependentMappingService mappingService){
        Preconditions.checkNotNull(mappingService, "mappingService should not be null");
        this.mappingService = mappingService;
    }

    public void setDomDataBroker(DOMDataBroker domDataBroker){
        Preconditions.checkNotNull(domDataBroker, "domDataBroker should not be null");
        this.domDataBroker = domDataBroker;
    }

    @Override
    public BindingDataReadTransaction newReadOnlyTransaction() {
        checkInvariants();
        return new BindingDataReadTransactionImpl(domDataBroker.newReadOnlyTransaction());
    }

    @Override
    public BindingDataReadWriteTransaction newReadWriteTransaction() {
        checkInvariants();
        return new BindingDataReadWriteTransactionImpl(domDataBroker.newReadWriteTransaction());
    }

    @Override
    public BindingDataWriteTransaction newWriteOnlyTransaction() {
        checkInvariants();
        return new BindingDataWriteTransactionImpl(domDataBroker.newWriteOnlyTransaction());
    }

    @Override
    public ListenerRegistration<BindingDataChangeListener> registerDataChangeListener(LogicalDatastoreType store, InstanceIdentifier<?> path, BindingDataChangeListener listener, DataChangeScope triggeringScope) {
        checkInvariants();
        DOMDataChangeListener domDataChangeListener = new DOMDataChangeListenerImpl(store, path, listener, triggeringScope);
        return new ListenerRegistrationImpl(listener, domDataBroker.registerDataChangeListener(store, mappingService.toDataDom(path), domDataChangeListener, triggeringScope));
    }

    private void checkInvariants(){
        Preconditions.checkNotNull(mappingService, "mappingService should not be null");
        Preconditions.checkNotNull(domDataBroker, "domDataBroker should not be null");
    }

    private Map<InstanceIdentifier<?>, DataObject> fromDOMToData(Map<org.opendaylight.yangtools.yang.data.api.InstanceIdentifier, ? extends NormalizedNode<?, ?>> createdData) {
        Map<InstanceIdentifier<?>, DataObject> newMap = new HashMap<>();
        for(Map.Entry<org.opendaylight.yangtools.yang.data.api.InstanceIdentifier, ? extends NormalizedNode<?, ?>> entry : createdData.entrySet()) {
            try {
                InstanceIdentifier key = mappingService.fromDataDom(entry.getKey());
                DataObject value = mappingService.dataObjectFromDataDom(key, (CompositeNode) entry.getValue());
                newMap.put(key, value);
            } catch (DeserializationException e) {
                Exceptions.sneakyThrow(e);
            }

        }

        return newMap;
    }

    private class BindingDataReadTransactionImpl implements BindingDataReadTransaction {
        private final DOMDataReadTransaction domDataReadTransaction;

        public BindingDataReadTransactionImpl(DOMDataReadTransaction domDataReadTransaction){
            this.domDataReadTransaction = domDataReadTransaction;
        }

        @Override
        public ListenableFuture<Optional<DataObject>> read(LogicalDatastoreType store, InstanceIdentifier<?> path) {
            // QUESTION : Is it safe to cache the data that we read here?
            return new ListenableFutureImpl(path, domDataReadTransaction.read(store, mappingService.toDataDom(path)));
        }

        @Override
        public Object getIdentifier() {
            // QUESTION : What is the type of the identifier? Does it need to be transformed at all or can we pass it transparently?
            return domDataReadTransaction.getIdentifier();
        }

        @Override
        public void close() {
            domDataReadTransaction.close();
        }
    }

    private class BindingDataReadWriteTransactionImpl implements BindingDataReadWriteTransaction {
        private final DOMDataReadWriteTransaction domDataReadWriteTransaction;

        public BindingDataReadWriteTransactionImpl(DOMDataReadWriteTransaction domDataReadWriteTransaction){
            this.domDataReadWriteTransaction = domDataReadWriteTransaction;
        }

        @Override
        public void cancel() {
            domDataReadWriteTransaction.cancel();
        }

        @Override
        public void put(LogicalDatastoreType store, InstanceIdentifier<?> path, DataObject data) {
            final org.opendaylight.yangtools.yang.data.api.InstanceIdentifier instanceIdentifier = mappingService.toDataDom(path);
            domDataReadWriteTransaction.put(store, instanceIdentifier, (NormalizedNode) mappingService.toDataDom(data));
        }

        @Override
        public void merge(LogicalDatastoreType store, InstanceIdentifier<?> path, DataObject data) {
            final org.opendaylight.yangtools.yang.data.api.InstanceIdentifier instanceIdentifier = mappingService.toDataDom(path);
            domDataReadWriteTransaction.merge(store, instanceIdentifier, (NormalizedNode) mappingService.toDataDom(data));
        }

        @Override
        public void delete(LogicalDatastoreType store, InstanceIdentifier<?> path) {
            final org.opendaylight.yangtools.yang.data.api.InstanceIdentifier instanceIdentifier = mappingService.toDataDom(path);
            domDataReadWriteTransaction.delete(store, instanceIdentifier);
        }

        @Override
        public Future<RpcResult<TransactionStatus>> commit() {
            return domDataReadWriteTransaction.commit();
        }

        @Override
        public ListenableFuture<Optional<DataObject>> read(LogicalDatastoreType store, InstanceIdentifier<?> path) {
            return new ListenableFutureImpl(path, domDataReadWriteTransaction.read(store, mappingService.toDataDom(path)));
        }

        @Override
        public Object getIdentifier() {
            return domDataReadWriteTransaction.getIdentifier();
        }

        @Override
        public void close() {
            domDataReadWriteTransaction.close();
        }
    }


    private class BindingDataWriteTransactionImpl implements BindingDataWriteTransaction {

        private final DOMDataWriteTransaction domDataWriteTransaction;

        public BindingDataWriteTransactionImpl(DOMDataWriteTransaction domDataWriteTransaction){
            this.domDataWriteTransaction = domDataWriteTransaction;
        }

        @Override
        public void cancel() {
            domDataWriteTransaction.cancel();
        }

        @Override
        public void put(LogicalDatastoreType store, InstanceIdentifier<?> path, DataObject data) {
            final org.opendaylight.yangtools.yang.data.api.InstanceIdentifier instanceIdentifier = mappingService.toDataDom(path);
            domDataWriteTransaction.put(store, instanceIdentifier, (NormalizedNode) mappingService.toDataDom(data));
        }

        @Override
        public void merge(LogicalDatastoreType store, InstanceIdentifier<?> path, DataObject data) {
            final org.opendaylight.yangtools.yang.data.api.InstanceIdentifier instanceIdentifier = mappingService.toDataDom(path);
            domDataWriteTransaction.merge(store, instanceIdentifier, (NormalizedNode) mappingService.toDataDom(data));
        }

        @Override
        public void delete(LogicalDatastoreType store, InstanceIdentifier<?> path) {
            final org.opendaylight.yangtools.yang.data.api.InstanceIdentifier instanceIdentifier = mappingService.toDataDom(path);
            domDataWriteTransaction.delete(store, instanceIdentifier);
        }

        @Override
        public Future<RpcResult<TransactionStatus>> commit() {
            return domDataWriteTransaction.commit();
        }

        @Override
        public Object getIdentifier() {
            return domDataWriteTransaction.getIdentifier();
        }

        @Override
        public void close() {
            domDataWriteTransaction.close();
        }
    }

    private class DOMDataChangeListenerImpl implements DOMDataChangeListener {
        private final BindingDataChangeListener bindingDataChangeListener;
        private final LogicalDatastoreType store;
        private final InstanceIdentifier<?> path;
        private final DataChangeScope triggeringScope;

        public DOMDataChangeListenerImpl(LogicalDatastoreType store, InstanceIdentifier<?> path, BindingDataChangeListener bindingDataChangeListener, DataChangeScope triggeringScope){
            this.store = store;
            this.path = path;
            this.bindingDataChangeListener = bindingDataChangeListener;
            this.triggeringScope = triggeringScope;
        }

        @Override
        public void onDataChanged(AsyncDataChangeEvent<org.opendaylight.yangtools.yang.data.api.InstanceIdentifier, NormalizedNode<?, ?>> change) {
            bindingDataChangeListener.onDataChanged(new AsyncDataChangeEventImpl(change));
        }
    }

    private class AsyncDataChangeEventImpl implements AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> {
        private final AsyncDataChangeEvent<org.opendaylight.yangtools.yang.data.api.InstanceIdentifier, NormalizedNode<?, ?>> dataChangeEvent;

        public AsyncDataChangeEventImpl(AsyncDataChangeEvent<org.opendaylight.yangtools.yang.data.api.InstanceIdentifier, NormalizedNode<?, ?>> change){
            this.dataChangeEvent = change;
        }

        @Override
        public Map<InstanceIdentifier<?>, DataObject> getCreatedData() {
            return fromDOMToData(dataChangeEvent.getCreatedData());
        }

        @Override
        public Map<InstanceIdentifier<?>, DataObject> getUpdatedData() {
            return fromDOMToData(dataChangeEvent.getUpdatedData());

        }

        @Override
        public Set<InstanceIdentifier<?>> getRemovedPaths() {
            final Set<org.opendaylight.yangtools.yang.data.api.InstanceIdentifier> removedPaths = dataChangeEvent.getRemovedPaths();
            final Set<InstanceIdentifier<?>> output = new HashSet<>();
            for(org.opendaylight.yangtools.yang.data.api.InstanceIdentifier instanceIdentifier : removedPaths){
                try {
                    output.add(mappingService.fromDataDom(instanceIdentifier));
                } catch (DeserializationException e) {
                    Exceptions.sneakyThrow(e);
                }
            }

            return output;
        }

        @Override
        public Map<InstanceIdentifier<?>, ? extends DataObject> getOriginalData() {
            return fromDOMToData(dataChangeEvent.getOriginalData());

        }

        @Override
        public DataObject getOriginalSubtree() {
            // FIXME : What is the id that should be passed for the data object (not clear)
            try {
                return mappingService.dataObjectFromDataDom(mappingService.fromDataDom(null), (CompositeNode) dataChangeEvent.getOriginalSubtree());
            } catch (DeserializationException e) {
                Exceptions.sneakyThrow(e);
            }
            return null;
        }

        @Override
        public DataObject getUpdatedSubtree() {
            // FIXME : What is the id that should be passed for the data object (not clear)
            try {
                return mappingService.dataObjectFromDataDom(mappingService.fromDataDom(null), (CompositeNode) dataChangeEvent.getUpdatedSubtree());
            } catch (DeserializationException e) {
                Exceptions.sneakyThrow(e);
            }
            return null;
        }
    }


    private static class ListenerRegistrationImpl implements ListenerRegistration<BindingDataChangeListener>{
        private final ListenerRegistration<DOMDataChangeListener> registration;
        private final BindingDataChangeListener listener;

        public ListenerRegistrationImpl(BindingDataChangeListener listener, ListenerRegistration<DOMDataChangeListener> registration) {
            this.listener = listener;
            this.registration = registration;
        }

        @Override
        public BindingDataChangeListener getInstance() {
            return listener;
        }

        @Override
        public void close() {
            registration.close();
        }
    }

    private class ListenableFutureImpl implements ListenableFuture<Optional<DataObject>> {
        private final ListenableFuture<Optional<NormalizedNode<?, ?>>> future;
        private final InstanceIdentifier<?> path;

        public ListenableFutureImpl(InstanceIdentifier<?> path, ListenableFuture<Optional<NormalizedNode<?, ?>>> future){
            this.future = future;
            this.path = path;
        }

        @Override
        public void addListener(Runnable runnable, Executor executor) {
            future.addListener(runnable, executor);
        }

        @Override
        public boolean cancel(boolean mayInterruptIfRunning) {
            return future.cancel(mayInterruptIfRunning);
        }

        @Override
        public boolean isCancelled() {
            return future.isCancelled();
        }

        @Override
        public boolean isDone() {
            return future.isDone();
        }

        @Override
        public Optional<DataObject> get() throws InterruptedException, ExecutionException {
            final Optional<NormalizedNode<?, ?>> normalizedNodeOptional = future.get();
            try {
                return Optional.of(mappingService.dataObjectFromDataDom(path, (CompositeNode) normalizedNodeOptional.get()));
            } catch (DeserializationException e) {
                Exceptions.sneakyThrow(e);
            }
            return null;
        }

        @Override
        public Optional<DataObject> get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
            final Optional<NormalizedNode<?, ?>> normalizedNodeOptional = future.get(timeout, unit);
            try {
                return Optional.of(mappingService.dataObjectFromDataDom(path, (CompositeNode) normalizedNodeOptional.get()));
            } catch (DeserializationException e) {
                Exceptions.sneakyThrow(e);
            }
            return null;
        }
    }
}
