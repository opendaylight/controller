package org.opendaylight.controller.sal.binding.impl.connect.dom;

import java.util.Map;
import java.util.concurrent.ConcurrentMap;

import org.opendaylight.controller.md.sal.common.api.RegistrationListener;
import org.opendaylight.controller.md.sal.common.api.data.DataCommitHandler;
import org.opendaylight.controller.md.sal.common.api.data.DataCommitHandlerRegistration;
import org.opendaylight.controller.md.sal.common.api.data.DataModification;
import org.opendaylight.controller.sal.binding.api.data.DataProviderService;
import org.opendaylight.controller.sal.common.util.CommitHandlerTransactions;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.CompositeNode;
import org.opendaylight.yangtools.yang.data.impl.codec.BindingIndependentMappingService;
import org.opendaylight.yangtools.yang.data.impl.codec.DeserializationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @deprecated This is part of the legacy DataBrokerService
 */
@Deprecated
class DomToBindingCommitHandler implements //
    RegistrationListener<DataCommitHandlerRegistration<InstanceIdentifier<? extends DataObject>, DataObject>>, //
    DataCommitHandler<org.opendaylight.yangtools.yang.data.api.InstanceIdentifier, CompositeNode> {

    private final Logger LOG = LoggerFactory.getLogger(DomToBindingCommitHandler.class);

    private final ConcurrentMap<Object, DomToBindingTransaction> bindingOpenedTransactions;
    private final ConcurrentMap<Object, BindingToDomTransaction> domOpenedTransactions;

    DomToBindingCommitHandler(final ConcurrentMap<Object, DomToBindingTransaction> bindingOpenedTransactions,
        final ConcurrentMap<Object, BindingToDomTransaction> domOpenedTransactions) {
        this.bindingOpenedTransactions = bindingOpenedTransactions;
        this.domOpenedTransactions = domOpenedTransactions;
    }

    private DataProviderService baDataService;
    private BindingIndependentMappingService mappingService;

    public void setBindingAwareDataService(final DataProviderService baDataService) {
        this.baDataService = baDataService;
    }

    public void setMappingService(final BindingIndependentMappingService mappingService) {
        this.mappingService = mappingService;
    }

    @Override
    public void onRegister(final DataCommitHandlerRegistration<InstanceIdentifier<? extends DataObject>, DataObject> registration) {
        mappingService.toDataDom(registration.getPath());
    }

    @Override
    public void onUnregister(
        final DataCommitHandlerRegistration<InstanceIdentifier<? extends DataObject>, DataObject> registration) {
        // NOOP for now
        // FIXME: do registration based on only active commit handlers.
    }

    @Override
    public org.opendaylight.controller.md.sal.common.api.data.DataCommitHandler.DataCommitTransaction<org.opendaylight.yangtools.yang.data.api.InstanceIdentifier, CompositeNode> requestCommit(
        final DataModification<org.opendaylight.yangtools.yang.data.api.InstanceIdentifier, CompositeNode> domTransaction) {
        Object identifier = domTransaction.getIdentifier();

        /**
         * We checks if the transcation was originated in this mapper. If it
         * was originated in this mapper we are returing allways success
         * commit hanlder to prevent creating loop in two-phase commit and
         * duplicating data.
         */
        if (domOpenedTransactions.containsKey(identifier)) {
            return CommitHandlerTransactions.allwaysSuccessfulTransaction(domTransaction);
        }

        org.opendaylight.controller.sal.binding.api.data.DataModificationTransaction baTransaction = createDomToBindingTransaction(domTransaction);
        DomToBindingTransaction forwardedTransaction = new DomToBindingTransaction(baTransaction, domTransaction, bindingOpenedTransactions);
        LOG.trace("Forwarding DOM Transaction: {} as Binding Transaction: {}.", domTransaction.getIdentifier(),
            baTransaction.getIdentifier());
        return forwardedTransaction;
    }

    private org.opendaylight.controller.sal.binding.api.data.DataModificationTransaction createDomToBindingTransaction(
        final DataModification<org.opendaylight.yangtools.yang.data.api.InstanceIdentifier, CompositeNode> source) {
        if (baDataService == null) {
            final String msg = "Binding Aware Service is not initialized correctly! DOM to Binding Transaction cannot be created for ";
            LOG.error(msg + "{}", source);
            throw new IllegalStateException(msg + source);
        }
        if (mappingService == null) {
            final String msg = "Mapping Service is not initialized correctly! DOM to Binding Transaction cannot be created for ";
            LOG.error(msg + "{}", source);
            throw new IllegalStateException(msg + source);
        }

        org.opendaylight.controller.sal.binding.api.data.DataModificationTransaction target = baDataService
            .beginTransaction();
        for (org.opendaylight.yangtools.yang.data.api.InstanceIdentifier entry : source.getRemovedConfigurationData()) {
            try {

                InstanceIdentifier<?> baEntry = mappingService.fromDataDom(entry);
                target.removeConfigurationData(baEntry);
            } catch (DeserializationException e) {
                LOG.error("Ommiting from BA transaction: {}.", entry, e);
            }
        }
        for (org.opendaylight.yangtools.yang.data.api.InstanceIdentifier entry : source.getRemovedOperationalData()) {
            try {

                InstanceIdentifier<?> baEntry = mappingService.fromDataDom(entry);
                target.removeOperationalData(baEntry);
            } catch (DeserializationException e) {
                LOG.error("Ommiting from BA transaction: {}.", entry, e);
            }
        }
        for (Map.Entry<org.opendaylight.yangtools.yang.data.api.InstanceIdentifier, CompositeNode> entry : source
            .getUpdatedConfigurationData().entrySet()) {
            try {
                InstanceIdentifier<?> baKey = mappingService.fromDataDom(entry.getKey());
                DataObject baData = mappingService.dataObjectFromDataDom(baKey, entry.getValue());
                target.putConfigurationData(baKey, baData);
            } catch (DeserializationException e) {
                LOG.error("Ommiting from BA transaction: {}.", entry.getKey(), e);
            }
        }
        for (Map.Entry<org.opendaylight.yangtools.yang.data.api.InstanceIdentifier, CompositeNode> entry : source
            .getUpdatedOperationalData().entrySet()) {
            try {

                InstanceIdentifier<?> baKey = mappingService.fromDataDom(entry.getKey());
                DataObject baData = mappingService.dataObjectFromDataDom(baKey, entry.getValue());
                target.putOperationalData(baKey, baData);
            } catch (DeserializationException e) {
                LOG.error("Ommiting from BA transaction: {}.", entry.getKey(), e);
            }
        }
        return target;
    }
}
