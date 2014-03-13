package org.opendaylight.controller.sal.binding.impl;

import com.google.common.base.Optional;
import com.google.common.util.concurrent.ListenableFuture;
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
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.impl.codec.BindingIndependentMappingService;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.Future;


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

    @Override
    public BindingDataReadTransaction newReadOnlyTransaction() {
        return new BindingDataReadTransactionImpl(domDataBroker.newReadOnlyTransaction());
    }

    @Override
    public BindingDataReadWriteTransaction newReadWriteTransaction() {
        return new BindingDataReadWriteTransactionImpl(domDataBroker.newReadWriteTransaction());
    }

    @Override
    public BindingDataWriteTransaction newWriteOnlyTransaction() {
        return new BindingDataWriteTransactionImpl(domDataBroker.newWriteOnlyTransaction());
    }

    @Override
    public ListenerRegistration<BindingDataChangeListener> registerDataChangeListener(LogicalDatastoreType store, InstanceIdentifier<?> path, BindingDataChangeListener listener, DataChangeScope triggeringScope) {
        DOMDataChangeListener domDataChangeListener = new DOMDataChangeListenerImpl(store, path, listener, triggeringScope);
        return new ListenerRegistrationImpl(domDataBroker.registerDataChangeListener(store, mappingService.toDataDom(path), domDataChangeListener, triggeringScope));
    }

    private static class BindingDataReadTransactionImpl implements BindingDataReadTransaction {
        private final DOMDataReadTransaction domDataReadTransaction;

        public BindingDataReadTransactionImpl(DOMDataReadTransaction domDataReadTransaction){
            this.domDataReadTransaction = domDataReadTransaction;
        }

        @Override
        public ListenableFuture<Optional<DataObject>> read(LogicalDatastoreType store, InstanceIdentifier<?> path) {
            return null;
        }

        @Override
        public Object getIdentifier() {
            return null;
        }

        @Override
        public void close() {
        }
    }

    private static class BindingDataReadWriteTransactionImpl implements BindingDataReadWriteTransaction {
        private final DOMDataReadWriteTransaction domDataReadWriteTransaction;

        public BindingDataReadWriteTransactionImpl(DOMDataReadWriteTransaction domDataReadWriteTransaction){
            this.domDataReadWriteTransaction = domDataReadWriteTransaction;
        }

        @Override
        public void cancel() {
        }

        @Override
        public void put(LogicalDatastoreType store, InstanceIdentifier<?> path, DataObject data) {
        }

        @Override
        public void merge(LogicalDatastoreType store, InstanceIdentifier<?> path, DataObject data) {
        }

        @Override
        public void delete(LogicalDatastoreType store, InstanceIdentifier<?> path) {
        }

        @Override
        public Future<RpcResult<TransactionStatus>> commit() {
            return null;
        }

        @Override
        public ListenableFuture<Optional<DataObject>> read(LogicalDatastoreType store, InstanceIdentifier<?> path) {
            return null;
        }

        @Override
        public Object getIdentifier() {
            return null;
        }

        @Override
        public void close() {
        }
    }


    private static class BindingDataWriteTransactionImpl implements BindingDataReadWriteTransaction {

        private final DOMDataWriteTransaction domDataWriteTransaction;

        public BindingDataWriteTransactionImpl(DOMDataWriteTransaction domDataWriteTransaction){
            this.domDataWriteTransaction = domDataWriteTransaction;
        }

        @Override
        public void cancel() {
        }

        @Override
        public void put(LogicalDatastoreType store, InstanceIdentifier<?> path, DataObject data) {
        }

        @Override
        public void merge(LogicalDatastoreType store, InstanceIdentifier<?> path, DataObject data) {
        }

        @Override
        public void delete(LogicalDatastoreType store, InstanceIdentifier<?> path) {
        }

        @Override
        public Future<RpcResult<TransactionStatus>> commit() {
            return null;
        }

        @Override
        public ListenableFuture<Optional<DataObject>> read(LogicalDatastoreType store, InstanceIdentifier<?> path) {
            return null;
        }

        @Override
        public Object getIdentifier() {
            return null;
        }

        @Override
        public void close() {
        }
    }

    private static class DOMDataChangeListenerImpl implements DOMDataChangeListener {
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
        }
    }

    private static class AsyncDataChangeEventImpl implements AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> {
        private final AsyncDataChangeEvent<InstanceIdentifier<?>, NormalizedNode> dataChangeEvent;

        public AsyncDataChangeEventImpl(AsyncDataChangeEvent<InstanceIdentifier<?>, NormalizedNode> dataChangeEvent){
            this.dataChangeEvent = dataChangeEvent;
        }

        @Override
        public Map<InstanceIdentifier<?>, DataObject> getCreatedData() {
            return null;
        }

        @Override
        public Map<InstanceIdentifier<?>, DataObject> getUpdatedData() {
            return null;
        }

        @Override
        public Set<InstanceIdentifier<?>> getRemovedPaths() {
            return null;
        }

        @Override
        public Map<InstanceIdentifier<?>, ? extends DataObject> getOriginalData() {
            return null;
        }

        @Override
        public DataObject getOriginalSubtree() {
            return null;
        }

        @Override
        public DataObject getUpdatedSubtree() {
            return null;
        }
    }

    private static class ListenerRegistrationImpl implements ListenerRegistration<BindingDataChangeListener>{
        private final ListenerRegistration<DOMDataChangeListener> registration;

        public ListenerRegistrationImpl(ListenerRegistration<DOMDataChangeListener> registration) {
            this.registration = registration;
        }

        @Override
        public BindingDataChangeListener getInstance() {
            return null;
        }

        @Override
        public void close() {
        }
    }
}
