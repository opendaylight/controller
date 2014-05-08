package org.opendaylight.controller.sal.binding.impl.connect.dom;

import java.util.Map;
import java.util.concurrent.ConcurrentMap;

import org.opendaylight.controller.md.sal.common.api.data.DataCommitHandler;
import org.opendaylight.controller.md.sal.common.api.data.DataModification;
import org.opendaylight.controller.sal.common.util.CommitHandlerTransactions;
import org.opendaylight.controller.sal.core.api.data.DataModificationTransaction;
import org.opendaylight.controller.sal.core.api.data.DataProviderService;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.CompositeNode;
import org.opendaylight.yangtools.yang.data.impl.codec.BindingIndependentMappingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @deprecated This is part of the legacy DataBrokerService
 */
@Deprecated
class BindingToDomCommitHandler implements
    DataCommitHandler<InstanceIdentifier<? extends DataObject>, DataObject> {

    private final Logger LOG = LoggerFactory.getLogger(BindingToDomCommitHandler.class);

    private final ConcurrentMap<Object, DomToBindingTransaction> bindingOpenedTransactions;
    private final ConcurrentMap<Object, BindingToDomTransaction> domOpenedTransactions;
    private org.opendaylight.controller.sal.core.api.data.DataProviderService biDataService;
    private BindingIndependentMappingService mappingService;

    BindingToDomCommitHandler(final ConcurrentMap<Object, DomToBindingTransaction> bindingOpenedTransactions,
        final ConcurrentMap<Object, BindingToDomTransaction> domOpenedTransactions) {
        this.bindingOpenedTransactions = bindingOpenedTransactions;
        this.domOpenedTransactions = domOpenedTransactions;
    }

    public void setBindingIndependentDataService(final DataProviderService biDataService) {
        this.biDataService = biDataService;
    }

    public void setMappingService(final BindingIndependentMappingService mappingService) {
        this.mappingService = mappingService;
    }

    @Override
    public org.opendaylight.controller.md.sal.common.api.data.DataCommitHandler.DataCommitTransaction<InstanceIdentifier<? extends DataObject>, DataObject> requestCommit(
        final DataModification<InstanceIdentifier<? extends DataObject>, DataObject> bindingTransaction) {

        /**
         * Transaction was created as DOM transaction, in that case we do
         * not need to forward it back.
         */
        if (bindingOpenedTransactions.containsKey(bindingTransaction.getIdentifier())) {
            return CommitHandlerTransactions.allwaysSuccessfulTransaction(bindingTransaction);
        }
        DataModificationTransaction domTransaction = createBindingToDomTransaction(bindingTransaction);
        BindingToDomTransaction wrapped = new BindingToDomTransaction(domTransaction, bindingTransaction, domOpenedTransactions);
        LOG.trace("Forwarding Binding Transaction: {} as DOM Transaction: {} .",
            bindingTransaction.getIdentifier(), domTransaction.getIdentifier());
        return wrapped;
    }

    private DataModificationTransaction createBindingToDomTransaction(
        final DataModification<InstanceIdentifier<? extends DataObject>, DataObject> source) {
        if (biDataService == null) {
            final String msg = "Binding Independent Service is not initialized correctly! Binding to DOM Transaction cannot be created for ";
            LOG.error(msg + "{}", source);
            throw new IllegalStateException(msg + source);
        }
        if (mappingService == null) {
            final String msg = "Mapping Service is not initialized correctly! Binding to DOM Transaction cannot be created for ";
            LOG.error(msg + "{}", source);
            throw new IllegalStateException(msg + source);
        }
        DataModificationTransaction target = biDataService.beginTransaction();
        LOG.debug("Created DOM Transaction {} for {},", target.getIdentifier(), source.getIdentifier());
        for (InstanceIdentifier<? extends DataObject> entry : source.getRemovedConfigurationData()) {
            org.opendaylight.yangtools.yang.data.api.InstanceIdentifier biEntry = mappingService.toDataDom(entry);
            target.removeConfigurationData(biEntry);
            LOG.debug("Delete of Binding Configuration Data {} is translated to {}", entry, biEntry);
        }
        for (InstanceIdentifier<? extends DataObject> entry : source.getRemovedOperationalData()) {
            org.opendaylight.yangtools.yang.data.api.InstanceIdentifier biEntry = mappingService.toDataDom(entry);
            target.removeOperationalData(biEntry);
            LOG.debug("Delete of Binding Operational Data {} is translated to {}", entry, biEntry);
        }
        for (Map.Entry<InstanceIdentifier<? extends DataObject>, DataObject> entry : source.getUpdatedConfigurationData()
            .entrySet()) {
            Map.Entry<org.opendaylight.yangtools.yang.data.api.InstanceIdentifier, CompositeNode> biEntry = mappingService
                .toDataDom(entry);
            target.putConfigurationData(biEntry.getKey(), biEntry.getValue());
            LOG.debug("Update of Binding Configuration Data {} is translated to {}", entry, biEntry);
        }
        for (Map.Entry<InstanceIdentifier<? extends DataObject>, DataObject> entry : source.getUpdatedOperationalData()
            .entrySet()) {
            Map.Entry<org.opendaylight.yangtools.yang.data.api.InstanceIdentifier, CompositeNode> biEntry = mappingService
                .toDataDom(entry);
            target.putOperationalData(biEntry.getKey(), biEntry.getValue());
            LOG.debug("Update of Binding Operational Data {} is translated to {}", entry, biEntry);
        }
        return target;
    }
}
