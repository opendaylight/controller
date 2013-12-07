package org.opendaylight.controller.forwardingrulesmanager.consumer.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import org.opendaylight.controller.md.sal.common.api.data.DataCommitHandler;
import org.opendaylight.controller.md.sal.common.api.data.DataCommitHandler.DataCommitTransaction;
import org.opendaylight.controller.md.sal.common.api.data.DataModification;
import org.opendaylight.controller.sal.common.util.Rpcs;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.config.rev130819.Flows;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.config.rev130819.flows.Flow;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.service.rev130819.AddFlowInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.service.rev130819.FlowAdded;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.service.rev130819.FlowRemoved;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.service.rev130819.FlowTableRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.service.rev130819.FlowUpdated;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.service.rev130819.NodeErrorNotification;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.service.rev130819.NodeExperimenterErrorNotification;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.service.rev130819.RemoveFlowInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.service.rev130819.SalFlowListener;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.service.rev130819.SalFlowService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.service.rev130819.SwitchFlowRemoved;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.service.rev130819.UpdateFlowInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.service.rev130819.flow.update.OriginalFlowBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.service.rev130819.flow.update.UpdatedFlowBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.table.config.rev131024.Tables;
import org.opendaylight.yang.gen.v1.urn.opendaylight.table.config.rev131024.tables.Table;
import org.opendaylight.yang.gen.v1.urn.opendaylight.table.config.rev131024.tables.TableBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.table.config.rev131024.tables.TableKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.table.types.rev131026.TableRef;
import org.opendaylight.yangtools.concepts.Registration;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.binding.NotificationListener;
import org.opendaylight.yangtools.yang.common.RpcError;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FlowConsumerImpl {
    protected static final Logger logger = LoggerFactory.getLogger(FlowConsumerImpl.class);
    private final FlowEventListener flowEventListener = new FlowEventListener();
    private Registration<NotificationListener> listener1Reg;
    private SalFlowService flowService;
    // private FlowDataListener listener;
    private FlowDataCommitHandler commitHandler;    

    public FlowConsumerImpl() {
        InstanceIdentifier<? extends DataObject> path = InstanceIdentifier.builder(Flows.class).toInstance();
        flowService = FRMConsumerImpl.getProviderSession().getRpcService(SalFlowService.class);

        if (null == flowService) {
            logger.error("Consumer SAL Service is down or NULL. FRM may not function as intended");
            return;
        }
        
        // For switch events
        listener1Reg = FRMConsumerImpl.getNotificationService().registerNotificationListener(flowEventListener);

        if (null == listener1Reg) {
            logger.error("Listener to listen on flow data modifcation events");
            return;
        }
        // addFlowTest();
        commitHandler = new FlowDataCommitHandler();
        FRMConsumerImpl.getDataProviderService().registerCommitHandler(path, commitHandler);        
    }
    
    /**
     * Adds flow to the southbound plugin and our internal database
     *
     * @param path
     * @param dataObject
     */
    private void addFlow(InstanceIdentifier<?> path, Flow dataObject) {

        AddFlowInputBuilder input = new AddFlowInputBuilder();
        input.fieldsFrom(dataObject);
        input.setNode((dataObject).getNode());
        input.setFlowTable(new FlowTableRef(createTableInstance(dataObject.getId(), dataObject.getNode())));
        // We send flow to the sounthbound plugin
        flowService.addFlow(input.build());
    }

    /**
     * Removes flow to the southbound plugin and our internal database
     *
     * @param path
     * @param dataObject
     */
    private void removeFlow(InstanceIdentifier<?> path, Flow dataObject) {
        
        RemoveFlowInputBuilder input = new RemoveFlowInputBuilder();
        input.fieldsFrom(dataObject);
        input.setNode((dataObject).getNode());
        input.setTableId(dataObject.getTableId());
        input.setFlowTable(new FlowTableRef(createTableInstance((long)dataObject.getTableId(), (dataObject).getNode())));
        // We send flow to the sounthbound plugin
        flowService.removeFlow(input.build());
    }

    /**
     * Update flow to the southbound plugin and our internal database
     *
     * @param path
     * @param dataObject
     */
    private void updateFlow(InstanceIdentifier<?> path, Flow updatedFlow, Flow originalFlow) {

        UpdateFlowInputBuilder input = new UpdateFlowInputBuilder();
        UpdatedFlowBuilder updatedflowbuilder = new UpdatedFlowBuilder();
        updatedflowbuilder.fieldsFrom(updatedFlow);
        input.setNode(updatedFlow.getNode());
        input.setUpdatedFlow(updatedflowbuilder.build());  
        OriginalFlowBuilder ofb = new OriginalFlowBuilder(originalFlow);
        input.setOriginalFlow(ofb.build());
        // We send flow to the sounthbound plugin
        flowService.updateFlow(input.build());
    }
 
    private void commitToPlugin(internalTransaction transaction) {
        Set<Entry<InstanceIdentifier<?>, DataObject>> createdEntries = transaction.getModification()
                .getCreatedConfigurationData().entrySet();

        /*
         * This little dance is because updatedEntries contains both created and
         * modified entries The reason I created a new HashSet is because the
         * collections we are returned are immutable.
         */
        Set<Entry<InstanceIdentifier<?>, DataObject>> updatedEntries = new HashSet<Entry<InstanceIdentifier<?>, DataObject>>();
        updatedEntries.addAll(transaction.getModification().getUpdatedConfigurationData().entrySet());
        updatedEntries.removeAll(createdEntries);

        Set<InstanceIdentifier<?>> removeEntriesInstanceIdentifiers = transaction.getModification()
                .getRemovedConfigurationData();
        transaction.getModification().getOriginalConfigurationData();
        for (Entry<InstanceIdentifier<?>, DataObject> entry : createdEntries) {
            if (entry.getValue() instanceof Flow) {
                logger.debug("Coming add cc in FlowDatacommitHandler");
                Flow flow = (Flow) entry.getValue();
                boolean status = validate(flow);
                if (!status) {
                    return;
                }
                addFlow(entry.getKey(), (Flow) entry.getValue());
            }
        }
       
        for (Entry<InstanceIdentifier<?>, DataObject> entry : updatedEntries) {
            if (entry.getValue() instanceof Flow) {
                logger.debug("Coming update cc in FlowDatacommitHandler");
                Flow updatedFlow = (Flow) entry.getValue();
                Flow originalFlow = (Flow) transaction.modification.getOriginalConfigurationData().get(entry.getKey());
                boolean status = validate(updatedFlow);
                if (!status) {
                    return;
                }
                updateFlow(entry.getKey(), updatedFlow, originalFlow);
            }
        }

        for (InstanceIdentifier<?> instanceId : removeEntriesInstanceIdentifiers) {
            DataObject removeValue = transaction.getModification().getOriginalConfigurationData().get(instanceId);
            if (removeValue instanceof Flow) {
                logger.debug("Coming remove cc in FlowDatacommitHandler");
                Flow flow = (Flow) removeValue;
                boolean status = validate(flow);
                
                if (!status) {
                    return;
                }
                
                removeFlow(instanceId, (Flow) removeValue);
            }
        }
    }

    private final class FlowDataCommitHandler implements DataCommitHandler<InstanceIdentifier<?>, DataObject> {   
     
        @SuppressWarnings("unchecked")
        public DataCommitTransaction<InstanceIdentifier<?>, DataObject> requestCommit(DataModification<InstanceIdentifier<?>, DataObject> modification) {
            // We should verify transaction
            logger.debug("Coming in FlowDatacommitHandler");
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
        
        /**
         * We create a plan which flows will be added, which will be updated and
         * which will be removed based on our internal state.
         *
         */
        void prepareUpdate() {          

        }
       
        /**
         * We are OK to go with execution of plan
         *
         */
        @Override
        public RpcResult<Void> finish() throws IllegalStateException {
            commitToPlugin(this);            
            return Rpcs.getRpcResult(true, null, Collections.<RpcError> emptySet());
        }

        /**
         *
         * We should rollback our preparation
         *
         */
        @Override
        public RpcResult<Void> rollback() throws IllegalStateException {       
            rollBackFlows(modification);
            return Rpcs.getRpcResult(true, null, Collections.<RpcError> emptySet());

        }       
    }

    private void rollBackFlows(DataModification<InstanceIdentifier<?>, DataObject> modification) {
     Set<Entry<InstanceIdentifier<? extends DataObject>, DataObject>> createdEntries = modification.getCreatedConfigurationData().entrySet();

    /*
     * This little dance is because updatedEntries contains both created and modified entries
     * The reason I created a new HashSet is because the collections we are returned are immutable.
     */
    Set<Entry<InstanceIdentifier<? extends DataObject>, DataObject>> updatedEntries = new HashSet<Entry<InstanceIdentifier<? extends DataObject>, DataObject>>();
    updatedEntries.addAll(modification.getUpdatedConfigurationData().entrySet());
    updatedEntries.removeAll(createdEntries);

    Set<InstanceIdentifier<? >> removeEntriesInstanceIdentifiers = modification.getRemovedConfigurationData();
    for (Entry<InstanceIdentifier<?>, DataObject> entry : createdEntries) {
        if(entry.getValue() instanceof Flow) {
            removeFlow(entry.getKey(),(Flow) entry.getValue()); // because we are rolling back, remove what we would have added.
        }
    }
    
    for (Entry<InstanceIdentifier<?>, DataObject> entry : updatedEntries) {
        if(entry.getValue() instanceof Flow) {            
            Flow updatedFlow = (Flow) entry.getValue();
            Flow originalFlow = (Flow) modification.getOriginalConfigurationData().get(entry.getKey());
            updateFlow(entry.getKey(), updatedFlow ,originalFlow);// because we are rolling back, replace the updated with the original
        }
    }

    for (InstanceIdentifier<?> instanceId : removeEntriesInstanceIdentifiers ) {
        DataObject removeValue = (Flow) modification.getOriginalConfigurationData().get(instanceId);
        if(removeValue instanceof Flow) {
            addFlow(instanceId,(Flow) removeValue);// because we are rolling back, add what we would have removed.

        }
    }
}
    final class FlowEventListener implements SalFlowListener {

        List<FlowAdded> addedFlows = new ArrayList<>();
        List<FlowRemoved> removedFlows = new ArrayList<>();
        List<FlowUpdated> updatedFlows = new ArrayList<>();

        @Override
        public void onFlowAdded(FlowAdded notification) {
            addedFlows.add(notification);
        }

        @Override
        public void onFlowRemoved(FlowRemoved notification) {
            removedFlows.add(notification);
        }

        @Override
        public void onFlowUpdated(FlowUpdated notification) {
            updatedFlows.add(notification);
        }

        @Override
        public void onNodeErrorNotification(NodeErrorNotification notification) {
            // TODO Auto-generated method stub

        }

        @Override
        public void onNodeExperimenterErrorNotification(NodeExperimenterErrorNotification notification) {
            // TODO Auto-generated method stub

        }

        @Override
        public void onSwitchFlowRemoved(SwitchFlowRemoved notification) {
            // TODO Auto-generated method stub

        }
    }

    public boolean validate(Flow flow) {
        String msg = ""; // Specific part of warn/error log

        boolean result = true;
        // flow Name validation
        if (!FRMUtil.isNameValid(flow.getFlowName())) {
            msg = "Invalid Flow name";
            result = false;
        }
        
        // Node Validation
        if (result == true && flow.getNode() == null) {
            msg = "Node is null";
            result = false;
        }

        // TODO: Validate we are seeking to program a flow against a valid
        // Node

        if (result == true && flow.getPriority() != null) {
            if (flow.getPriority() < 0 || flow.getPriority() > 65535) {
                msg = String.format("priority %s is not in the range 0 - 65535", flow.getPriority());
                result = false;
            }
        }
       
        if (!FRMUtil.validateMatch(flow)) {
            logger.error("Not a valid Match");
            result = false;
        }
        if (!FRMUtil.validateInstructions(flow)) {
            logger.error("Not a valid Instruction");
            result = false;
        }
        if (result == false) {
            logger.warn("Invalid Configuration for flow {}. The failure is {}", flow, msg);
            logger.error("Invalid Configuration ({})", msg);
        }
        return result;
    }
    
    private InstanceIdentifier<?> createTableInstance(Long tableId, NodeRef nodeRef) {        
        Table table;
        InstanceIdentifier<Table> tableInstance;
        TableBuilder builder = new TableBuilder();
        builder.setId(tableId);
        builder.setKey(new TableKey(tableId, nodeRef));
        table = builder.build();
        tableInstance = InstanceIdentifier.builder(Tables.class).child(Table.class, table.getKey()).toInstance();
        return tableInstance;
    }
}