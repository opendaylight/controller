package org.opendaylight.controller.sal.binding.impl;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Future;

import org.opendaylight.controller.sal.binding.api.BindingAwareBroker;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker.ConsumerSession;
import org.opendaylight.controller.sal.binding.api.BindingAwareProvider.ProviderFunctionality;
import org.opendaylight.controller.sal.binding.api.BindingAwareService;
import org.opendaylight.controller.sal.binding.api.DataBrokerService;
import org.opendaylight.controller.sal.binding.api.DataCommitHandler;
import org.opendaylight.controller.sal.binding.api.DataProviderService;
import org.opendaylight.controller.sal.binding.api.DataValidator;
import org.opendaylight.controller.sal.binding.spi.MappingProvider;
import org.opendaylight.controller.sal.binding.spi.SALBindingModule;
import org.opendaylight.controller.sal.common.DataStoreIdentifier;
import org.opendaylight.controller.sal.binding.api.DataRefresher;
import org.opendaylight.controller.yang.binding.DataRoot;
import org.opendaylight.controller.yang.common.RpcResult;
import org.opendaylight.controller.yang.data.api.CompositeNode;

public class DataModule implements SALBindingModule {

    private BindingAwareBroker broker;
    private org.opendaylight.controller.sal.core.api.Broker.ProviderSession biSession;
    private MappingProvider mappingProvider;
    private final BIFacade biFacade = new BIFacade();
    private org.opendaylight.controller.sal.core.api.data.DataProviderService biDataService;

    @Override
    public void setBroker(BindingAwareBroker broker) {
        this.broker = broker;
    }

    @Override
    public void onBISessionAvailable(
            org.opendaylight.controller.sal.core.api.Broker.ProviderSession session) {
        this.biSession = session;
        this.biDataService = session
                .getService(org.opendaylight.controller.sal.core.api.data.DataProviderService.class);
        // biDataService.addRefresher(store, refresher)

    }

    @Override
    public void setMappingProvider(MappingProvider provider) {
        this.mappingProvider = provider;

    }

    @Override
    public Set<Class<? extends BindingAwareService>> getProvidedServices() {
        Set<Class<? extends BindingAwareService>> ret = new HashSet<Class<? extends BindingAwareService>>();
        ret.add(DataBrokerService.class);
        ret.add(DataProviderService.class);
        return ret;
    }

    @Override
    public <T extends BindingAwareService> T getServiceForSession(
            Class<T> service, ConsumerSession session) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Set<Class<? extends ProviderFunctionality>> getSupportedProviderFunctionality() {
        // TODO Auto-generated method stub
        return null;
    }

    private class DataBrokerSession implements DataBrokerService {

        @Override
        public <T extends DataRoot> T getData(DataStoreIdentifier store,
                Class<T> rootType) {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public <T extends DataRoot> T getData(DataStoreIdentifier store,
                T filter) {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public <T extends DataRoot> T getCandidateData(
                DataStoreIdentifier store, Class<T> rootType) {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public <T extends DataRoot> T getCandidateData(
                DataStoreIdentifier store, T filter) {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public RpcResult<DataRoot> editCandidateData(DataStoreIdentifier store,
                DataRoot changeSet) {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public Future<RpcResult<Void>> commit(DataStoreIdentifier store) {
            // TODO Auto-generated method stub
            return null;
        }

    }

    private class DataProviderSession extends DataBrokerSession implements
            DataProviderService {

        @Override
        public void addValidator(DataStoreIdentifier store,
                DataValidator validator) {
            // TODO Auto-generated method stub

        }

        @Override
        public void removeValidator(DataStoreIdentifier store,
                DataValidator validator) {
            // TODO Auto-generated method stub

        }

        @Override
        public void addCommitHandler(DataStoreIdentifier store,
                DataCommitHandler provider) {
            // TODO Auto-generated method stub

        }

        @Override
        public void removeCommitHandler(DataStoreIdentifier store,
                DataCommitHandler provider) {
            // TODO Auto-generated method stub

        }

        @Override
        public void addRefresher(DataStoreIdentifier store,
                DataRefresher refresher) {
            // TODO Auto-generated method stub

        }

        @Override
        public void removeRefresher(DataStoreIdentifier store,
                DataRefresher refresher) {
            // TODO Auto-generated method stub

        }

    }

    private class BIFacade
            implements
            org.opendaylight.controller.sal.core.api.data.DataCommitHandler,
            org.opendaylight.controller.sal.core.api.data.DataValidator,
            org.opendaylight.controller.sal.core.api.data.DataProviderService.DataRefresher {

        @Override
        public RpcResult<Void> validate(CompositeNode toValidate) {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public Set<DataStoreIdentifier> getSupportedDataStores() {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public RpcResult<CommitTransaction> requestCommit(
                DataStoreIdentifier store) {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public void refreshData() {
            // TODO Auto-generated method stub
            
        }

    }

}
