package org.opendaylight.controller.forwardingrulesmanager.consumer.impl;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.opendaylight.controller.clustering.services.IClusterContainerServices;
import org.opendaylight.controller.md.sal.common.api.data.DataCommitHandler;
import org.opendaylight.controller.md.sal.common.api.data.DataCommitHandler.DataCommitTransaction;
import org.opendaylight.controller.md.sal.common.api.data.DataModification;
import org.opendaylight.controller.sal.common.util.Rpcs;
import org.opendaylight.controller.sal.core.IContainer;
import org.opendaylight.controller.sal.utils.ServiceHelper;
import org.opendaylight.controller.sal.utils.Status;
import org.opendaylight.controller.sal.utils.StatusCode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.table.config.rev131024.Tables;
import org.opendaylight.yang.gen.v1.urn.opendaylight.table.config.rev131024.tables.Table;
import org.opendaylight.yang.gen.v1.urn.opendaylight.table.service.rev131026.SalTableService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.table.service.rev131026.UpdateTableInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.table.service.rev131026.table.update.UpdatedTableBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.table.types.rev131026.table.features.TableFeatures;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TableFeaturesConsumerImpl {
    protected static final Logger logger = LoggerFactory.getLogger(TableFeaturesConsumerImpl.class);
    private SalTableService tableService;
    private TableDataCommitHandler commitHandler;
    private final IClusterContainerServices clusterContainerService = null;
    private IContainer container;
    private static final String NAMEREGEX = "^[a-zA-Z0-9]+$";
    private boolean inContainerMode; // being used by global instance only

    public TableFeaturesConsumerImpl() {
        InstanceIdentifier<? extends DataObject> path = InstanceIdentifier.builder(Tables.class).child(Table.class)
                .toInstance();
        tableService = FRMConsumerImpl.getProviderSession().getRpcService(SalTableService.class);

        if (null == tableService) {
            logger.error("Consumer SAL Service is down or NULL. FRM may not function as intended");
            System.out.println("Consumer SAL Service is down or NULL.");
            return;
        }

        System.out.println("-------------------------------------------------------------------");
        commitHandler = new TableDataCommitHandler();
        FRMConsumerImpl.getDataProviderService().registerCommitHandler(path, commitHandler);
        container = (IContainer) ServiceHelper.getGlobalInstance(IContainer.class, this);
    }

    /**
     * Updates TableFeatures to the southbound plugin and our internal database
     *
     * @param path
     * @param dataObject
     */
    private void updateTableFeatures(InstanceIdentifier<?> path, TableFeatures dataObject) {

        UpdateTableInputBuilder input = new UpdateTableInputBuilder();
        UpdatedTableBuilder updatedtablebuilder = new UpdatedTableBuilder();
        updatedtablebuilder.fieldsFrom(dataObject);
        List<TableFeatures> features = updatedtablebuilder.build().getTableFeatures();
        for (TableFeatures feature : features) {
            if (feature != null && feature.getMaxEntries() != null) {
                logger.error("Max Entries field is read-only, cannot be changed");
                return;
            }
        }
        input.setUpdatedTable(updatedtablebuilder.build());

        // We send table feature update request to the sounthbound plugin
        tableService.updateTable(input.build());
    }

    @SuppressWarnings("unchecked")
    private void commitToPlugin(internalTransaction transaction) {

        for (@SuppressWarnings("unused")
        Entry<InstanceIdentifier<?>, TableFeatures> entry : transaction.updates.entrySet()) {
            System.out.println("Coming update cc in TableDatacommitHandler");
            updateTableFeatures(entry.getKey(), entry.getValue());
        }

    }

    private final class TableDataCommitHandler implements DataCommitHandler<InstanceIdentifier<?>, DataObject> {

        @SuppressWarnings("unchecked")
        @Override
        public DataCommitTransaction requestCommit(DataModification<InstanceIdentifier<?>, DataObject> modification) {
            // We should verify transaction
            System.out.println("Coming in TableFeaturesDatacommitHandler");
            internalTransaction transaction = new internalTransaction(modification);
            transaction.prepareUpdate();
            return transaction;
        }
    }

    private final class internalTransaction implements DataCommitTransaction<InstanceIdentifier<?>, DataObject> {

        private final DataModification<InstanceIdentifier<?>, DataObject> modification;

        @Override
        public DataModification<InstanceIdentifier<?>, DataObject> getModification() {
            return modification;
        }

        public internalTransaction(DataModification<InstanceIdentifier<?>, DataObject> modification) {
            this.modification = modification;
        }

        Map<InstanceIdentifier<?>, TableFeatures> updates = new HashMap<>();

        /**
         * We create a plan which table features will be updated.
         *
         */
        void prepareUpdate() {

            Set<Entry<InstanceIdentifier<?>, DataObject>> puts = modification.getUpdatedConfigurationData().entrySet();
            for (Entry<InstanceIdentifier<?>, DataObject> entry : puts) {

                // validating the DataObject

                Status status = validate(container, (TableFeatures) entry);
                if (!status.isSuccess()) {
                    logger.warn("Invalid Configuration for table features The failure is {}", entry,
                            status.getDescription());
                    String error = "Invalid Configuration (" + status.getDescription() + ")";
                    logger.error(error);
                    return;
                }
                if (entry.getValue() instanceof TableFeatures) {
                    TableFeatures tablefeatures = (TableFeatures) entry.getValue();
                    preparePutEntry(entry.getKey(), tablefeatures);
                }

            }
        }

        private void preparePutEntry(InstanceIdentifier<?> key, TableFeatures tablefeatures) {
            if (tablefeatures != null) {
                // Updating the Map
                System.out.println("Coming update  in TableFeaturesDatacommitHandler");
                updates.put(key, tablefeatures);
            }
        }

        /**
         * We are OK to go with execution of plan
         *
         */
        @Override
        public RpcResult<Void> finish() throws IllegalStateException {

            commitToPlugin(this);
            // We return true if internal transaction is successful.
            // return Rpcs.getRpcResult(true, null, Collections.emptySet());
            return Rpcs.getRpcResult(true, null, null);
        }

        /**
         *
         * We should rollback our preparation
         *
         */
        @Override
        public RpcResult<Void> rollback() throws IllegalStateException {
            // NOOP - we did not modified any internal state during
            // requestCommit phase
            // return Rpcs.getRpcResult(true, null, Collections.emptySet());
            return Rpcs.getRpcResult(true, null, null);

        }

        public Status validate(IContainer container, TableFeatures dataObject) {

            String tablename = dataObject.getName();
            if (tablename == null || tablename.trim().isEmpty() || !tablename.matches(NAMEREGEX)
                    || tablename.length() != 32) {
                return new Status(StatusCode.BADREQUEST, "Invalid table name");
            }

            return new Status(StatusCode.SUCCESS);
        }
    }
}
