package org.opendaylight.controller.sal.binding.impl.connect.dom;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import org.opendaylight.controller.md.sal.common.api.RegistrationListener;
import org.opendaylight.controller.md.sal.common.api.TransactionStatus;
import org.opendaylight.controller.md.sal.common.api.data.DataCommitHandler;
import org.opendaylight.controller.md.sal.common.api.data.DataCommitHandler.DataCommitTransaction;
import org.opendaylight.controller.md.sal.common.api.data.DataCommitHandlerRegistration;
import org.opendaylight.controller.md.sal.common.api.data.DataModification;
import org.opendaylight.controller.sal.binding.api.data.DataProviderService;
import org.opendaylight.controller.sal.binding.api.data.RuntimeDataProvider;
import org.opendaylight.controller.sal.common.util.Rpcs;
import org.opendaylight.controller.sal.core.api.Provider;
import org.opendaylight.controller.sal.core.api.Broker.ProviderSession;
import org.opendaylight.controller.sal.core.api.data.DataModificationTransaction;
import org.opendaylight.yangtools.concepts.Registration;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.RpcError;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.data.api.CompositeNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BindingIndependentDataServiceConnector implements //
        RuntimeDataProvider, //
        Provider {

    private final Logger LOG = LoggerFactory.getLogger(BindingIndependentDataServiceConnector.class);

    private static final InstanceIdentifier<? extends DataObject> ROOT = InstanceIdentifier.builder().toInstance();

    private static final org.opendaylight.yangtools.yang.data.api.InstanceIdentifier ROOT_BI = org.opendaylight.yangtools.yang.data.api.InstanceIdentifier
            .builder().toInstance();

    private BindingIndependentMappingService mappingService;

    private org.opendaylight.controller.sal.core.api.data.DataProviderService biDataService;

    private DataProviderService baDataService;

    private ConcurrentMap<Object, BindingToDomTransaction> domOpenedTransactions = new ConcurrentHashMap<>();
    private ConcurrentMap<Object, DomToBindingTransaction> bindingOpenedTransactions = new ConcurrentHashMap<>();

    private BindingToDomCommitHandler bindingToDomCommitHandler = new BindingToDomCommitHandler();
    private DomToBindingCommitHandler domToBindingCommitHandler = new DomToBindingCommitHandler();

    private Registration<DataCommitHandler<InstanceIdentifier<? extends DataObject>, DataObject>> baCommitHandlerRegistration;

    private Registration<DataCommitHandler<org.opendaylight.yangtools.yang.data.api.InstanceIdentifier, CompositeNode>> biCommitHandlerRegistration;

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

    private DataModificationTransaction createBindingToDomTransaction(
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
        for (InstanceIdentifier<? extends DataObject> entry : source.getRemovedConfigurationData()) {
            org.opendaylight.yangtools.yang.data.api.InstanceIdentifier biEntry = mappingService.toDataDom(entry);
            target.removeConfigurationData(biEntry);
        }
        for (InstanceIdentifier<? extends DataObject> entry : source.getRemovedOperationalData()) {
            org.opendaylight.yangtools.yang.data.api.InstanceIdentifier biEntry = mappingService.toDataDom(entry);
            target.removeOperationalData(biEntry);
        }
        return target;
    }

    private org.opendaylight.controller.sal.binding.api.data.DataModificationTransaction createDomToBindingTransaction(
            DataModification<org.opendaylight.yangtools.yang.data.api.InstanceIdentifier, CompositeNode> source) {
        org.opendaylight.controller.sal.binding.api.data.DataModificationTransaction target = baDataService
                .beginTransaction();
        for (Entry<org.opendaylight.yangtools.yang.data.api.InstanceIdentifier, CompositeNode> entry : source
                .getUpdatedConfigurationData().entrySet()) {
            InstanceIdentifier<?> baKey = mappingService.fromDataDom(entry.getKey());
            DataObject baData = mappingService.dataObjectFromDataDom(baKey, entry.getValue());
            target.putConfigurationData(baKey, baData);
        }
        for (Entry<org.opendaylight.yangtools.yang.data.api.InstanceIdentifier, CompositeNode> entry : source
                .getUpdatedOperationalData().entrySet()) {
            InstanceIdentifier<?> baKey = mappingService.fromDataDom(entry.getKey());
            DataObject baData = mappingService.dataObjectFromDataDom(baKey, entry.getValue());
            target.putOperationalData(baKey, baData);
        }
        for (org.opendaylight.yangtools.yang.data.api.InstanceIdentifier entry : source.getRemovedConfigurationData()) {
            InstanceIdentifier<?> baEntry = mappingService.fromDataDom(entry);
            target.removeConfigurationData(baEntry);
        }
        for (org.opendaylight.yangtools.yang.data.api.InstanceIdentifier entry : source.getRemovedOperationalData()) {
            InstanceIdentifier<?> baEntry = mappingService.fromDataDom(entry);
            target.removeOperationalData(baEntry);
        }
        return target;
    }

    public org.opendaylight.controller.sal.core.api.data.DataProviderService getBiDataService() {
        return biDataService;
    }

    public void setBiDataService(org.opendaylight.controller.sal.core.api.data.DataProviderService biDataService) {
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
        baCommitHandlerRegistration = baDataService.registerCommitHandler(ROOT, bindingToDomCommitHandler);
        biCommitHandlerRegistration = biDataService.registerCommitHandler(ROOT_BI, domToBindingCommitHandler);
        baDataService.registerCommitHandlerListener(domToBindingCommitHandler);
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

    private class DomToBindingTransaction implements
            DataCommitTransaction<org.opendaylight.yangtools.yang.data.api.InstanceIdentifier, CompositeNode> {

        private final org.opendaylight.controller.sal.binding.api.data.DataModificationTransaction backing;
        private final DataModification<org.opendaylight.yangtools.yang.data.api.InstanceIdentifier, CompositeNode> modification;

        public DomToBindingTransaction(
                org.opendaylight.controller.sal.binding.api.data.DataModificationTransaction backing,
                DataModification<org.opendaylight.yangtools.yang.data.api.InstanceIdentifier, CompositeNode> modification) {
            super();
            this.backing = backing;
            this.modification = modification;
            bindingOpenedTransactions.put(backing.getIdentifier(), this);
        }

        @Override
        public DataModification<org.opendaylight.yangtools.yang.data.api.InstanceIdentifier, CompositeNode> getModification() {
            return modification;
        }

        @Override
        public RpcResult<Void> rollback() throws IllegalStateException {
            // backing.cancel();
            return Rpcs.<Void> getRpcResult(true, null, Collections.<RpcError> emptySet());
        }

        @Override
        public RpcResult<Void> finish() throws IllegalStateException {
            Future<RpcResult<TransactionStatus>> result = backing.commit();
            try {
                RpcResult<TransactionStatus> baResult = result.get();
                return Rpcs.<Void> getRpcResult(baResult.isSuccessful(), null, baResult.getErrors());
            } catch (InterruptedException e) {
                throw new IllegalStateException("", e);
            } catch (ExecutionException e) {
                throw new IllegalStateException("", e);
            }
        }
    }

    private class BindingToDomTransaction implements
            DataCommitTransaction<InstanceIdentifier<? extends DataObject>, DataObject> {

        private DataModificationTransaction backing;
        private DataModification<InstanceIdentifier<? extends DataObject>, DataObject> modification;

        public BindingToDomTransaction(DataModificationTransaction backing,
                DataModification<InstanceIdentifier<? extends DataObject>, DataObject> modification) {
            this.backing = backing;
            this.modification = modification;
            domOpenedTransactions.put(backing.getIdentifier(), this);
        }

        @Override
        public DataModification<InstanceIdentifier<? extends DataObject>, DataObject> getModification() {
            return modification;
        }

        @Override
        public RpcResult<Void> finish() throws IllegalStateException {
            Future<RpcResult<TransactionStatus>> result = backing.commit();
            try {
                RpcResult<TransactionStatus> biResult = result.get();
                return Rpcs.<Void> getRpcResult(biResult.isSuccessful(), null, biResult.getErrors());
            } catch (InterruptedException e) {
                throw new IllegalStateException("", e);
            } catch (ExecutionException e) {
                throw new IllegalStateException("", e);
            } finally {
                domOpenedTransactions.remove(backing.getIdentifier());
            }
        }

        @Override
        public RpcResult<Void> rollback() throws IllegalStateException {
            domOpenedTransactions.remove(backing.getIdentifier());
            return Rpcs.<Void> getRpcResult(true, null, Collections.<RpcError> emptySet());
        }
    }

    private class BindingToDomCommitHandler implements
            DataCommitHandler<InstanceIdentifier<? extends DataObject>, DataObject> {

        @Override
        public org.opendaylight.controller.md.sal.common.api.data.DataCommitHandler.DataCommitTransaction<InstanceIdentifier<? extends DataObject>, DataObject> requestCommit(
                DataModification<InstanceIdentifier<? extends DataObject>, DataObject> bindingTransaction) {

            /**
             * Transaction was created as DOM transaction, in that case we do
             * not need to forward it back.
             */
            if (bindingOpenedTransactions.containsKey(bindingTransaction.getIdentifier())) {

                return CommitHandlersTransactions.allwaysSuccessfulTransaction(bindingTransaction);
            }
            DataModificationTransaction domTransaction = createBindingToDomTransaction(bindingTransaction);
            BindingToDomTransaction wrapped = new BindingToDomTransaction(domTransaction, bindingTransaction);
            LOG.info("Forwarding Binding Transaction: {} as DOM Transaction: {} .", bindingTransaction.getIdentifier(),
                    domTransaction.getIdentifier());
            return wrapped;
        }
    }

    private class DomToBindingCommitHandler implements //
            RegistrationListener<DataCommitHandlerRegistration<InstanceIdentifier<?>, DataObject>>, //
            DataCommitHandler<org.opendaylight.yangtools.yang.data.api.InstanceIdentifier, CompositeNode> {

        @Override
        public void onRegister(DataCommitHandlerRegistration<InstanceIdentifier<?>, DataObject> registration) {
            
            org.opendaylight.yangtools.yang.data.api.InstanceIdentifier domPath = mappingService.toDataDom(registration.getPath());
            // FIXME: do registration based on only active commit handlers.
            
        }

        @Override
        public void onUnregister(DataCommitHandlerRegistration<InstanceIdentifier<?>, DataObject> registration) {
            // NOOP for now
            // FIXME: do registration based on only active commit handlers.
        }

        public org.opendaylight.controller.md.sal.common.api.data.DataCommitHandler.DataCommitTransaction<org.opendaylight.yangtools.yang.data.api.InstanceIdentifier, CompositeNode> requestCommit(
                DataModification<org.opendaylight.yangtools.yang.data.api.InstanceIdentifier, CompositeNode> domTransaction) {
            Object identifier = domTransaction.getIdentifier();

            /**
             * We checks if the transcation was originated in this mapper. If it
             * was originated in this mapper we are returing allways success
             * commit hanlder to prevent creating loop in two-phase commit and
             * duplicating data.
             */
            if (domOpenedTransactions.containsKey(identifier)) {
                return CommitHandlersTransactions.allwaysSuccessfulTransaction(domTransaction);
            }

            org.opendaylight.controller.sal.binding.api.data.DataModificationTransaction baTransaction = createDomToBindingTransaction(domTransaction);
            DomToBindingTransaction forwardedTransaction = new DomToBindingTransaction(baTransaction, domTransaction);
            LOG.info("Forwarding DOM Transaction: {} as Binding Transaction: {}.", domTransaction.getIdentifier(),
                    baTransaction.getIdentifier());
            return forwardedTransaction;
        }
    }
}
