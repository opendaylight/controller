package org.opendaylight.controller.sal.binding.api.data;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.Future;

import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.Identifiable;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.RpcResult;

public interface DataModification {
    
    /**
     * Returns transaction identifier
     * 
     * @return Transaction identifier
     */
    Object getIdentifier();
    
    TransactionStatus getStatus();
    
    /**
     * Reads data from overal data storage which includes
     * runtime and configuration data.
     * 
     * @param path
     * @return
     */
    DataObject read(InstanceIdentifier path);
    
    /**
     * Reads data from configuration data storage.
     * 
     * @param path Instance identifier which 
     * @return
     */
    DataObject readConfiguration(InstanceIdentifier path);
    
    void putRuntimeData(InstanceIdentifier path,DataObject data);
    void putConfigurationData(InstanceIdentifier path,DataObject data);
    void removeRuntimeData(InstanceIdentifier path);
    void removeConfigurationData(InstanceIdentifier path);


    Map<InstanceIdentifier,DataObject> getRuntimeDataUpdates();
    Map<InstanceIdentifier,DataObject> getConfigurationDataUpdates();
    Set<InstanceIdentifier> getRemovals();
    Set<InstanceIdentifier> getConfigurationRemovals();
    
    /**
     * Commits transaction to be stored in global data repository.
     * 
     * 
     * @return  Future object which returns RpcResult with TransactionStatus 
     *          when transaction is processed by store.
     */
    Future<RpcResult<TransactionStatus>> commit();
    
    void registerListener(DataTransactionListener listener);
    void unregisterListener(DataTransactionListener listener);
    
    public enum TransactionStatus {
        
        UNSUBMITTED,
        COMMITING,
        COMMITED,
        FAILED,
        CANCELED
    }
    
    public interface DataTransactionListener {
        
        void onStatusUpdated(DataModification transaction,TransactionStatus status);
        
    }
}
