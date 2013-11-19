package org.opendaylight.controller.sal.binding.impl.connect.dom;

import java.util.Collection;
import java.util.Collections;
import java.util.Map.Entry;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import org.opendaylight.controller.config.api.jmx.CommitStatus;
import org.opendaylight.controller.md.sal.common.api.TransactionStatus;
import org.opendaylight.controller.md.sal.common.api.data.DataCommitHandler;
import org.opendaylight.controller.md.sal.common.api.data.DataModification;
import org.opendaylight.controller.md.sal.common.api.data.DataReader;
import org.opendaylight.controller.md.sal.common.api.data.DataCommitHandler.DataCommitTransaction;
import org.opendaylight.controller.sal.binding.api.data.DataProviderService;
import org.opendaylight.controller.sal.binding.api.data.RuntimeDataProvider;
import org.opendaylight.controller.sal.common.util.Rpcs;
import org.opendaylight.controller.sal.core.api.Provider;
import org.opendaylight.controller.sal.core.api.Broker.ProviderSession;
import org.opendaylight.controller.sal.core.api.data.DataBrokerService;
import org.opendaylight.controller.sal.core.api.data.DataModificationTransaction;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.RpcError;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.data.api.CompositeNode;

import com.google.common.base.Preconditions;

public class BindingIndependentDataServiceConnector implements //
        RuntimeDataProvider, //
        DataCommitHandler<InstanceIdentifier<? extends DataObject>, DataObject>, Provider {

    private static final InstanceIdentifier<? extends DataObject> ROOT = InstanceIdentifier.builder().toInstance();

    private BindingIndependentMappingService mappingService;

    private DataBrokerService biDataService;

    private DataProviderService baDataService;

    @Override
    public DataObject readOperationalData(InstanceIdentifier<? extends DataObject> path) {
        org.opendaylight.yangtools.yang.data.api.InstanceIdentifier biPath = mappingService.toDataDom(path);
        CompositeNode result = biDataService.readOperationalData(biPath);
        return mappingService.dataObjectFromDataDom(path, result);
    }

    @Override
    public DataObject readConfigurationData(InstanceIdentifier<? extends DataObject> path) {
        org.opendaylight.yangtools.yang.data.api.InstanceIdentifier biPath = mappingService.toDataDom(path);
        CompositeNode result = biDataService.readConfigurationData(biPath);
        return mappingService.dataObjectFromDataDom(path, result);
    }

    @Override
    public org.opendaylight.controller.md.sal.common.api.data.DataCommitHandler.DataCommitTransaction<InstanceIdentifier<? extends DataObject>, DataObject> requestCommit(
            DataModification<InstanceIdentifier<? extends DataObject>, DataObject> modification) {

        DataModificationTransaction translated = translateTransaction(modification);
        return new WrappedTransaction(translated, modification);
    }

    private DataModificationTransaction translateTransaction(
            DataModification<InstanceIdentifier<? extends DataObject>, DataObject> source) {
        DataModificationTransaction target = biDataService.beginTransaction();
        for (Entry<InstanceIdentifier<? extends DataObject>, DataObject> entry : source.getUpdatedConfigurationData()
                .entrySet()) {
            Entry<org.opendaylight.yangtools.yang.data.api.InstanceIdentifier, CompositeNode> biEntry = mappingService
                    .toDataDom(entry);
            target.putConfigurationData(biEntry.getKey(), biEntry.getValue());
        }
        for (Entry<InstanceIdentifier<? extends DataObject>, DataObject> entry : source.getUpdatedOperationalData()
                .entrySet()) {
            Entry<org.opendaylight.yangtools.yang.data.api.InstanceIdentifier, CompositeNode> biEntry = mappingService
                    .toDataDom(entry);
            target.putOperationalData(biEntry.getKey(), biEntry.getValue());
        }
        for(InstanceIdentifier<? extends DataObject> entry : source.getRemovedConfigurationData()) {
            org.opendaylight.yangtools.yang.data.api.InstanceIdentifier biEntry = mappingService.toDataDom(entry);
            target.removeConfigurationData(biEntry);
        }
        for(InstanceIdentifier<? extends DataObject> entry : source.getRemovedOperationalData()) {
            org.opendaylight.yangtools.yang.data.api.InstanceIdentifier biEntry = mappingService.toDataDom(entry);
            target.removeOperationalData(biEntry);
        }
        return target;
    }

    private class WrappedTransaction implements
            DataCommitTransaction<InstanceIdentifier<? extends DataObject>, DataObject> {

        private DataModificationTransaction backing;
        private DataModification<InstanceIdentifier<? extends DataObject>, DataObject> modification;

        public WrappedTransaction(DataModificationTransaction backing,
                DataModification<InstanceIdentifier<? extends DataObject>, DataObject> modification) {
            this.backing = backing;
            this.modification = modification;
        }

        @Override
        public DataModification<InstanceIdentifier<? extends DataObject>, DataObject> getModification() {
            return modification;
        }

        @Override
        public RpcResult<Void> finish() throws IllegalStateException {
            Future<RpcResult<TransactionStatus>> result = backing.commit();
            try {
                RpcResult<TransactionStatus> biresult = result.get();
            } catch (InterruptedException e) {
                throw new IllegalStateException("", e);
            } catch (ExecutionException e) {
                throw new IllegalStateException("", e);
            }
            return Rpcs.<Void> getRpcResult(true, null, Collections.<RpcError> emptySet());
        }

        @Override
        public RpcResult<Void> rollback() throws IllegalStateException {
            // backing.cancel();
            return Rpcs.<Void> getRpcResult(true, null, Collections.<RpcError> emptySet());
        }

    }

    public DataBrokerService getBiDataService() {
        return biDataService;
    }

    public void setBiDataService(DataBrokerService biDataService) {
        this.biDataService = biDataService;
    }

    public DataProviderService getBaDataService() {
        return baDataService;
    }

    public void setBaDataService(DataProviderService baDataService) {
        this.baDataService = baDataService;
    }

    public void start() {
        baDataService.registerDataReader(ROOT, this);
        baDataService.registerCommitHandler(ROOT, this);
    }

    public void setMappingService(BindingIndependentMappingService mappingService) {
        this.mappingService = mappingService;
    }
    
    @Override
    public Collection<ProviderFunctionality> getProviderFunctionality() {
        return Collections.emptyList();
    }
    
    @Override
    public void onSessionInitiated(ProviderSession session) {
        setBiDataService(session.getService(org.opendaylight.controller.sal.core.api.data.DataProviderService.class));
        start();
    }

}
