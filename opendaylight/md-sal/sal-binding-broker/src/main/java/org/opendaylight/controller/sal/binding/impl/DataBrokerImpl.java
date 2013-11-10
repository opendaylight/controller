package org.opendaylight.controller.sal.binding.impl;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import org.opendaylight.controller.md.sal.common.api.data.DataReader;
import org.opendaylight.controller.md.sal.common.impl.service.AbstractDataBroker;
import org.opendaylight.controller.sal.binding.api.data.DataChangeListener;
import org.opendaylight.controller.sal.binding.api.data.DataProviderService;
import org.opendaylight.controller.sal.binding.impl.util.BindingAwareDataReaderRouter;
import org.opendaylight.controller.sal.common.DataStoreIdentifier;
import org.opendaylight.yangtools.concepts.Registration;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.DataRoot;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.RpcResult;


public class DataBrokerImpl extends AbstractDataBroker<InstanceIdentifier<? extends DataObject>, DataObject, DataChangeListener> implements
        DataProviderService {

    public DataBrokerImpl() {
        setDataReadRouter(new BindingAwareDataReaderRouter());
    }

    @Override
    public DataTransactionImpl beginTransaction() {
        return new DataTransactionImpl(this);
    }

    @Override
    public <T extends DataRoot> T getData(DataStoreIdentifier store, Class<T> rootType) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public <T extends DataRoot> T getData(DataStoreIdentifier store, T filter) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public <T extends DataRoot> T getCandidateData(DataStoreIdentifier store, Class<T> rootType) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public <T extends DataRoot> T getCandidateData(DataStoreIdentifier store, T filter) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public RpcResult<DataRoot> editCandidateData(DataStoreIdentifier store, DataRoot changeSet) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Future<RpcResult<Void>> commit(DataStoreIdentifier store) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public DataObject getData(InstanceIdentifier<? extends DataObject> data) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public DataObject getConfigurationData(InstanceIdentifier<?> data) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void registerChangeListener(InstanceIdentifier<? extends DataObject> path, DataChangeListener changeListener) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void unregisterChangeListener(InstanceIdentifier<? extends DataObject> path,
            DataChangeListener changeListener) {
        // TODO Auto-generated method stub
        
    }
    
    
}