package org.opendaylight.controller.sal.binding.impl.forward;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import org.eclipse.xtext.xbase.lib.Exceptions;
import org.opendaylight.controller.md.sal.binding.api.BindingDataBroker;
import org.opendaylight.controller.md.sal.binding.api.BindingDataChangeListener;
import org.opendaylight.controller.md.sal.binding.api.BindingDataReadTransaction;
import org.opendaylight.controller.md.sal.binding.api.BindingDataReadWriteTransaction;
import org.opendaylight.controller.md.sal.binding.api.BindingDataWriteTransaction;
import org.opendaylight.controller.md.sal.common.api.TransactionStatus;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeEvent;
import org.opendaylight.controller.md.sal.common.api.data.AsyncTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.dom.api.DOMDataBroker;
import org.opendaylight.controller.md.sal.dom.api.DOMDataChangeListener;
import org.opendaylight.controller.md.sal.dom.api.DOMDataReadTransaction;
import org.opendaylight.controller.md.sal.dom.api.DOMDataReadWriteTransaction;
import org.opendaylight.controller.md.sal.dom.api.DOMDataWriteTransaction;
import org.opendaylight.yangtools.concepts.AbstractListenerRegistration;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.data.api.CompositeNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.impl.codec.BindingIndependentMappingService;
import org.opendaylight.yangtools.yang.data.impl.codec.DeserializationException;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;


/**
 * The DataBrokerImpl simply defers to the DOMDataBroker for all its operations. All transactions and
 * listener registrations are wrapped by the DataBrokerImpl to allow binding aware components to use the
 * DataBroker transparently.
 *
 * Besides this the DataBrokerImpl and it's collaborators also cache data that is already transformed from
 * the binding independent to binding aware format
 *
 * TODO : All references in this class to CompositeNode should be switched to NormalizedNode once the MappingService is updated
 *
 */
public class DataBrokerImpl implements BindingDataBroker{

    // Mapper to convert from Binding Independent objects to Binding Aware objects
    private BindingIndependentMappingService mappingService;

    // The Broker to whom we do all forwarding
    private DOMDataBroker domDataBroker;

    // Transaction tracking
    private final AtomicLong lastTransactionNumber = new AtomicLong();


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

    private abstract class AbstractBindingTransaction implements AsyncTransaction<InstanceIdentifier<?>, DataObject> {
        private final Map<LogicalDatastoreType, LoadingCache<InstanceIdentifier<?>, DataObject>> cacheMap = new ConcurrentHashMap<>();

        @Override
        public Object getIdentifier() {
            StringBuilder builder = new StringBuilder();
            builder.append("BA-").append(lastTransactionNumber.incrementAndGet())
                    .append("[").append(getTransaction().getIdentifier()).append("]");
            return builder.toString();
        }

        @Override
        public void close() {
            getTransaction().close();
        }

        protected ListenableFuture<Optional<DataObject>> transformFuture(final LogicalDatastoreType store, final InstanceIdentifier<?> path, ListenableFuture<Optional<NormalizedNode<?, ?>>> future) {
            return Futures.transform(future, new Function<Optional<NormalizedNode<?, ?>>, Optional<DataObject>>() {
                @Nullable
                @Override
                public Optional<DataObject> apply(@Nullable Optional<NormalizedNode<?, ?>> normalizedNodeOptional) {
                    try {
                        final DataObject dataObject = mappingService.dataObjectFromDataDom(path, (CompositeNode) normalizedNodeOptional.get());
                        updateCache(store, path, dataObject);
                        return Optional.of(dataObject);
                    } catch (DeserializationException e) {
                        Exceptions.sneakyThrow(e);
                    }
                    return null;
                }
            });
        }

        protected void doPut(DOMDataWriteTransaction writeTransaction, LogicalDatastoreType store, InstanceIdentifier<?> path, DataObject data) {
            invalidateCache(store, path);
            final org.opendaylight.yangtools.yang.data.api.InstanceIdentifier instanceIdentifier = mappingService.toDataDom(path);
            writeTransaction.put(store, instanceIdentifier, (NormalizedNode) mappingService.toDataDom(data));
        }

        protected void doMerge(DOMDataWriteTransaction writeTransaction, LogicalDatastoreType store, InstanceIdentifier<?> path, DataObject data) {
            invalidateCache(store, path);
            final org.opendaylight.yangtools.yang.data.api.InstanceIdentifier instanceIdentifier = mappingService.toDataDom(path);
            writeTransaction.merge(store, instanceIdentifier, (NormalizedNode) mappingService.toDataDom(data));
        }

        protected void doDelete(DOMDataWriteTransaction writeTransaction, LogicalDatastoreType store, InstanceIdentifier<?> path) {
            invalidateCache(store, path);
            final org.opendaylight.yangtools.yang.data.api.InstanceIdentifier instanceIdentifier = mappingService.toDataDom(path);
            writeTransaction.delete(store, instanceIdentifier);
        }

        protected Future<RpcResult<TransactionStatus>> doCommit(DOMDataWriteTransaction writeTransaction) {
            return writeTransaction.commit();
        }

        protected void doCancel(DOMDataWriteTransaction writeTransaction) {
            writeTransaction.cancel();
        }

        protected ListenableFuture<Optional<DataObject>> doRead(DOMDataReadTransaction readTransaction, LogicalDatastoreType store, InstanceIdentifier<?> path) {
            final DataObject dataObject = getFromCache(store, path);
            if(dataObject == null){
                final ListenableFuture<Optional<NormalizedNode<?, ?>>> future = readTransaction.read(store, mappingService.toDataDom(path));
                return transformFuture(store, path, future);
            } else {
                return Futures.immediateFuture(Optional.of(dataObject));
            }
        }

        protected abstract AsyncTransaction<org.opendaylight.yangtools.yang.data.api.InstanceIdentifier, NormalizedNode<?, ?>> getTransaction();

        private DataObject getFromCache(LogicalDatastoreType store, final InstanceIdentifier<?> path){
            LoadingCache<InstanceIdentifier<?>, DataObject> cache = cacheMap.get(store);
            if(cache != null){
                return cache.getIfPresent(path);
            }
            return null;
        }

        private void updateCache(LogicalDatastoreType store, final InstanceIdentifier<?> path, DataObject dataObject){
            // Check if cache exists. If not create one.
            LoadingCache<InstanceIdentifier<?>, DataObject> cache = cacheMap.get(store);
            if(cache == null){
                cache = CacheBuilder.newBuilder()
                           .maximumSize(1000)
                           .expireAfterWrite(1, TimeUnit.MINUTES)
                           .build(
                                   new CacheLoader<InstanceIdentifier<?>, DataObject>() {
                                       public DataObject load(InstanceIdentifier<?> key) {
                                           // Always return null here because we will not be utilizing the
                                           //  loading cache behavior for now
                                           return null;
                                       }
                                   });

            }

            cache.put(path, dataObject);
        }

        private void invalidateCache(LogicalDatastoreType store, final InstanceIdentifier<?> path){
            // TODO: More stuff will need to be invalidated at this point as a consequence
            LoadingCache<InstanceIdentifier<?>, DataObject> cache = cacheMap.get(store);
            if(cache != null){
                cache.invalidate(path);
            }

        }
    }

    private class BindingDataReadTransactionImpl extends AbstractBindingTransaction implements BindingDataReadTransaction {
        private final DOMDataReadTransaction domDataReadTransaction;

        public BindingDataReadTransactionImpl(DOMDataReadTransaction domDataReadTransaction){
            this.domDataReadTransaction = domDataReadTransaction;
        }

        @Override
        public ListenableFuture<Optional<DataObject>> read(LogicalDatastoreType store, final InstanceIdentifier<?> path) {
            return doRead(this.domDataReadTransaction, store, path);
        }

        @Override
        protected AsyncTransaction<org.opendaylight.yangtools.yang.data.api.InstanceIdentifier, NormalizedNode<?, ?>> getTransaction() {
            return domDataReadTransaction;
        }
    }


    private class BindingDataReadWriteTransactionImpl extends AbstractBindingTransaction implements BindingDataReadWriteTransaction {
        private final DOMDataReadWriteTransaction domDataReadWriteTransaction;

        public BindingDataReadWriteTransactionImpl(DOMDataReadWriteTransaction domDataReadWriteTransaction){
            this.domDataReadWriteTransaction = domDataReadWriteTransaction;
        }

        @Override
        public void cancel() {
            doCancel(domDataReadWriteTransaction);
        }

        @Override
        public void put(LogicalDatastoreType store, InstanceIdentifier<?> path, DataObject data) {
            doPut(domDataReadWriteTransaction, store, path, data);
        }

        @Override
        public void merge(LogicalDatastoreType store, InstanceIdentifier<?> path, DataObject data) {
            doMerge(domDataReadWriteTransaction, store, path, data);
        }

        @Override
        public void delete(LogicalDatastoreType store, InstanceIdentifier<?> path) {
            doDelete(domDataReadWriteTransaction, store, path);
        }

        @Override
        public Future<RpcResult<TransactionStatus>> commit() {
            return doCommit(domDataReadWriteTransaction);
        }

        @Override
        public ListenableFuture<Optional<DataObject>> read(LogicalDatastoreType store, InstanceIdentifier<?> path) {
            return doRead(domDataReadWriteTransaction, store, path);
        }

        @Override
        protected AsyncTransaction<org.opendaylight.yangtools.yang.data.api.InstanceIdentifier, NormalizedNode<?, ?>> getTransaction() {
            return domDataReadWriteTransaction;
        }
    }


    private class BindingDataWriteTransactionImpl extends AbstractBindingTransaction implements BindingDataWriteTransaction {

        private final DOMDataWriteTransaction domDataWriteTransaction;

        public BindingDataWriteTransactionImpl(DOMDataWriteTransaction domDataWriteTransaction){
            this.domDataWriteTransaction = domDataWriteTransaction;
        }

        @Override
        public void cancel() {
            doCancel(domDataWriteTransaction);
        }

        @Override
        public void put(LogicalDatastoreType store, InstanceIdentifier<?> path, DataObject data) {
            doPut(domDataWriteTransaction, store, path, data);
        }

        @Override
        public void merge(LogicalDatastoreType store, InstanceIdentifier<?> path, DataObject data) {
            doMerge(domDataWriteTransaction, store, path, data);
        }

        @Override
        public void delete(LogicalDatastoreType store, InstanceIdentifier<?> path) {
            doDelete(domDataWriteTransaction, store, path);
        }

        @Override
        public Future<RpcResult<TransactionStatus>> commit() {
            return doCommit(domDataWriteTransaction);
        }

        @Override
        protected AsyncTransaction<org.opendaylight.yangtools.yang.data.api.InstanceIdentifier, NormalizedNode<?, ?>> getTransaction() {
            return domDataWriteTransaction;
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
            try {
                return mappingService.dataObjectFromDataDom(InstanceIdentifier.builder().build(), (CompositeNode) dataChangeEvent.getOriginalSubtree());
            } catch (DeserializationException e) {
                Exceptions.sneakyThrow(e);
            }
            return null;
        }

        @Override
        public DataObject getUpdatedSubtree() {
            try {
                return mappingService.dataObjectFromDataDom(InstanceIdentifier.builder().build(), (CompositeNode) dataChangeEvent.getUpdatedSubtree());
            } catch (DeserializationException e) {
                Exceptions.sneakyThrow(e);
            }
            return null;
        }
    }


    private static class ListenerRegistrationImpl extends AbstractListenerRegistration<BindingDataChangeListener> {
        private final ListenerRegistration<DOMDataChangeListener> registration;
        public ListenerRegistrationImpl(BindingDataChangeListener listener, ListenerRegistration<DOMDataChangeListener> registration) {
            super(listener);
            this.registration = registration;
        }

        @Override
        protected void removeRegistration() {
            registration.close();
        }
    }
}
