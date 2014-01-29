package org.opendaylight.controller.md.sal.binding.util;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.Future;

import org.opendaylight.controller.md.sal.common.api.TransactionStatus;
import org.opendaylight.controller.sal.binding.api.data.DataBrokerService;
import org.opendaylight.controller.sal.binding.api.data.DataModificationTransaction;
import org.opendaylight.yangtools.concepts.Delegator;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.RpcResult;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;


public class ChainedTransactionSupport {

    DataBrokerService dataBroker;
    
    
    public DataModificationTransaction beginChainedTransaction(DataModificationTransaction parent) {
        checkArgument(parent != null,"Parent Transaction should not be null.");
        
        if(parent.getStatus() == TransactionStatus.COMMITED) {
            return dataBroker.beginTransaction();
        }
        
        checkState(parent.getStatus() != TransactionStatus.CANCELED);
        checkState(parent.getStatus() != TransactionStatus.FAILED);
        
        
        DataModificationTransaction backingTransaction = dataBroker.beginTransaction();
        return new ChainedTransaction(parent, backingTransaction);
    }
    
    private class ChainedTransaction implements Delegator<DataModificationTransaction>, DataModificationTransaction {
        
        final DataModificationTransaction parent;
        final DataModificationTransaction delegate;
        
        
        
        public Map<InstanceIdentifier<? extends DataObject>, DataObject> getCreatedOperationalData() {
            return delegate.getCreatedOperationalData();
        }

        public Map<InstanceIdentifier<? extends DataObject>, DataObject> getCreatedConfigurationData() {
            return delegate.getCreatedConfigurationData();
        }

        public DataObject readOperationalData(InstanceIdentifier<? extends DataObject> path) {
            return delegate.readOperationalData(path);
        }

        public TransactionStatus getStatus() {
            return delegate.getStatus();
        }

        public Map<InstanceIdentifier<? extends DataObject>, DataObject> getUpdatedOperationalData() {
            return delegate.getUpdatedOperationalData();
        }

        public void putRuntimeData(InstanceIdentifier<? extends DataObject> path, DataObject data) {
            delegate.putRuntimeData(path, data);
        }

        public Object getIdentifier() {
            return delegate.getIdentifier();
        }

        public DataObject readConfigurationData(InstanceIdentifier<? extends DataObject> path) {
            return delegate.readConfigurationData(path);
        }

        public Future<RpcResult<TransactionStatus>> commit() {
            return delegate.commit();
        }

        public void putOperationalData(InstanceIdentifier<? extends DataObject> path, DataObject data) {
            delegate.putOperationalData(path, data);
        }

        public void putConfigurationData(InstanceIdentifier<? extends DataObject> path, DataObject data) {
            delegate.putConfigurationData(path, data);
        }

        public Map<InstanceIdentifier<? extends DataObject>, DataObject> getUpdatedConfigurationData() {
            return delegate.getUpdatedConfigurationData();
        }

        public void removeRuntimeData(InstanceIdentifier<? extends DataObject> path) {
            delegate.removeRuntimeData(path);
        }

        public void removeOperationalData(InstanceIdentifier<? extends DataObject> path) {
            delegate.removeOperationalData(path);
        }

        public void removeConfigurationData(InstanceIdentifier<? extends DataObject> path) {
            delegate.removeConfigurationData(path);
        }

        public Set<InstanceIdentifier<? extends DataObject>> getRemovedConfigurationData() {
            return delegate.getRemovedConfigurationData();
        }

        public Set<InstanceIdentifier<? extends DataObject>> getRemovedOperationalData() {
            return delegate.getRemovedOperationalData();
        }

        public Map<InstanceIdentifier<? extends DataObject>, DataObject> getOriginalConfigurationData() {
            return delegate.getOriginalConfigurationData();
        }

        public ListenerRegistration<DataTransactionListener> registerListener(DataTransactionListener listener) {
            return delegate.registerListener(listener);
        }

        public Map<InstanceIdentifier<? extends DataObject>, DataObject> getOriginalOperationalData() {
            return delegate.getOriginalOperationalData();
        }

        public ChainedTransaction(DataModificationTransaction parent, DataModificationTransaction delegate) {
            super();
            this.parent = parent;
            this.delegate = delegate;
        }

        @Override
        public DataModificationTransaction getDelegate() {
            return delegate;
        }
    }
}
