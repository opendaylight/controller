package org.opendaylight.controller.forwardingrulesmanager.consumer.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Future;

import org.opendaylight.controller.clustering.services.CacheConfigException;
import org.opendaylight.controller.clustering.services.CacheExistException;
import org.opendaylight.controller.clustering.services.IClusterContainerServices;
import org.opendaylight.controller.clustering.services.IClusterServices;
import org.opendaylight.controller.md.sal.common.api.data.DataCommitHandler;
import org.opendaylight.controller.md.sal.common.api.data.DataCommitHandler.DataCommitTransaction;
import org.opendaylight.controller.md.sal.common.api.data.DataModification;
import org.opendaylight.controller.sal.common.util.Rpcs;
import org.opendaylight.controller.sal.core.IContainer;
import org.opendaylight.controller.sal.utils.ServiceHelper;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.config.rev130819.Flows;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.config.rev130819.flows.Flow;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.config.rev130819.flows.FlowKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.service.rev130819.AddFlowInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.service.rev130819.AddFlowInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.service.rev130819.AddFlowOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.service.rev130819.FlowAdded;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.service.rev130819.FlowRemoved;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.service.rev130819.FlowUpdated;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.service.rev130819.NodeErrorNotification;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.service.rev130819.NodeExperimenterErrorNotification;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.service.rev130819.NodeFlow;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.service.rev130819.RemoveFlowInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.service.rev130819.SalFlowListener;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.service.rev130819.SalFlowService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.service.rev130819.SwitchFlowRemoved;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.service.rev130819.UpdateFlowInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.service.rev130819.flow.update.UpdatedFlowBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.list.Instruction;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.Nodes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeKey;
import org.opendaylight.yangtools.concepts.Registration;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.binding.NotificationListener;
import org.opendaylight.yangtools.yang.common.RpcError;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FlowConsumerImpl implements IForwardingRulesManager {
    protected static final Logger logger = LoggerFactory.getLogger(FlowConsumerImpl.class);
    private final FlowEventListener flowEventListener = new FlowEventListener();
    private Registration<NotificationListener> listener1Reg;
    private SalFlowService flowService;
    // private FlowDataListener listener;
    private FlowDataCommitHandler commitHandler;
    private static ConcurrentHashMap<FlowKey, Flow> originalSwView;
    private static ConcurrentMap<FlowKey, Flow> installedSwView;
    private IClusterContainerServices clusterContainerService = null;
    private IContainer container;
    private static final String NAMEREGEX = "^[a-zA-Z0-9]+$";
    private static ConcurrentMap<Integer, Flow> staticFlows;
    private static ConcurrentMap<Integer, Integer> staticFlowsOrdinal = new ConcurrentHashMap<Integer, Integer>();
    /*
     * Inactive flow list. This is for the global instance of FRM It will
     * contain all the flow entries which were installed on the global container
     * when the first container is created.
     */
    private static ConcurrentMap<FlowKey, Flow> inactiveFlows;

    /*
     * /* Per node indexing
     */
    private static ConcurrentMap<Node, List<Flow>> nodeFlows;
    private boolean inContainerMode; // being used by global instance only

    public FlowConsumerImpl() {
        InstanceIdentifier<? extends DataObject> path = InstanceIdentifier.builder(Flows.class).toInstance();
        flowService = FRMConsumerImpl.getProviderSession().getRpcService(SalFlowService.class);

        if (null == flowService) {
            logger.error("Consumer SAL Service is down or NULL. FRM may not function as intended");
            System.out.println("Consumer SAL Service is down or NULL.");
            return;
        }

        // listener = new FlowDataListener();

        // if (null ==
        // FRMConsumerImpl.getDataBrokerService().registerDataChangeListener(path,
        // listener)) {
        // logger.error("Failed to listen on flow data modifcation events");
        // System.out.println("Consumer SAL Service is down or NULL.");
        // return;
        // }

        // For switch events
        listener1Reg = FRMConsumerImpl.getNotificationService().registerNotificationListener(flowEventListener);

        if (null == listener1Reg) {
            logger.error("Listener to listen on flow data modifcation events");
            System.out.println("Consumer SAL Service is down or NULL.");
            return;
        }
        // addFlowTest();
        System.out.println("-------------------------------------------------------------------");
        commitHandler = new FlowDataCommitHandler();
        FRMConsumerImpl.getDataProviderService().registerCommitHandler(path, commitHandler);
        clusterContainerService = (IClusterContainerServices) ServiceHelper.getGlobalInstance(
                IClusterContainerServices.class, this);
        allocateCaches();
        /*
         * If we are not the first cluster node to come up, do not initialize
         * the static flow entries ordinal
         */
        if (staticFlowsOrdinal.size() == 0) {
            staticFlowsOrdinal.put(0, Integer.valueOf(0));
        }
    }

    private void allocateCaches() {

        if (this.clusterContainerService == null) {
            logger.warn("Un-initialized clusterContainerService, can't create cache");
            return;
        }

        try {
            clusterContainerService.createCache("frm.originalSwView",
                    EnumSet.of(IClusterServices.cacheMode.TRANSACTIONAL));
            clusterContainerService.createCache("frm.installedSwView",
                    EnumSet.of(IClusterServices.cacheMode.TRANSACTIONAL));
            clusterContainerService
                    .createCache("frm.staticFlows", EnumSet.of(IClusterServices.cacheMode.TRANSACTIONAL));
            clusterContainerService.createCache("frm.staticFlowsOrdinal",
                    EnumSet.of(IClusterServices.cacheMode.TRANSACTIONAL));
            clusterContainerService.createCache("frm.inactiveFlows",
                    EnumSet.of(IClusterServices.cacheMode.TRANSACTIONAL));
            clusterContainerService.createCache("frm.nodeFlows", EnumSet.of(IClusterServices.cacheMode.TRANSACTIONAL));
            clusterContainerService.createCache("frm.groupFlows", EnumSet.of(IClusterServices.cacheMode.TRANSACTIONAL));
        } catch (CacheConfigException cce) {
            logger.error("CacheConfigException");
        } catch (CacheExistException cce) {
            logger.error("CacheExistException");
        }
    }

    private void addFlowTest() {
        try {
            NodeRef nodeOne = createNodeRef("foo:node:1");
            AddFlowInputBuilder input1 = new AddFlowInputBuilder();

            input1.setNode(nodeOne);
            AddFlowInput firstMsg = input1.build();

            if (null != flowService) {
                System.out.println(flowService.toString());
            } else {
                System.out.println("ConsumerFlowService is NULL");
            }
            @SuppressWarnings("unused")
            Future<RpcResult<AddFlowOutput>> result1 = flowService.addFlow(firstMsg);

        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    /**
     * Adds flow to the southbound plugin and our internal database
     *
     * @param path
     * @param dataObject
     */
    private void addFlow(InstanceIdentifier<?> path, Flow dataObject) {

        AddFlowInputBuilder input = new AddFlowInputBuilder();
        List<Instruction> inst = (dataObject).getInstructions().getInstruction();
        input.setNode((dataObject).getNode());
        input.setPriority((dataObject).getPriority());
        input.setMatch((dataObject).getMatch());
        input.setCookie((dataObject).getCookie());
        input.setInstructions((dataObject).getInstructions());
        dataObject.getMatch().getLayer3Match();
        for (int i = 0; i < inst.size(); i++) {
            System.out.println("i = " + i + inst.get(i).getInstruction().toString());
            System.out.println("i = " + i + inst.get(i).toString());
        }

        System.out.println("Instruction list" + (dataObject).getInstructions().getInstruction().toString());

        // updating the staticflow cache
        /*
         *  Commented out... as in many other places... use of ClusteringServices is breaking things
         *  insufficient time to debug
        Integer ordinal = staticFlowsOrdinal.get(0);
        staticFlowsOrdinal.put(0, ++ordinal);
        staticFlows.put(ordinal, dataObject);
        */

        // We send flow to the sounthbound plugin
        flowService.addFlow(input.build());
        /*
         * Commented out as this will also break due to improper use of ClusteringServices
        updateLocalDatabase((NodeFlow) dataObject, true);
        */
    }

    /**
     * Removes flow to the southbound plugin and our internal database
     *
     * @param path
     * @param dataObject
     */
    private void removeFlow(InstanceIdentifier<?> path, Flow dataObject) {

        RemoveFlowInputBuilder input = new RemoveFlowInputBuilder();
        List<Instruction> inst = (dataObject).getInstructions().getInstruction();
        input.setNode((dataObject).getNode());
        input.setPriority((dataObject).getPriority());
        input.setMatch((dataObject).getMatch());
        input.setCookie((dataObject).getCookie());
        input.setInstructions((dataObject).getInstructions());
        dataObject.getMatch().getLayer3Match();
        for (int i = 0; i < inst.size(); i++) {
            System.out.println("i = " + i + inst.get(i).getInstruction().toString());
            System.out.println("i = " + i + inst.get(i).toString());
        }

        System.out.println("Instruction list" + (dataObject).getInstructions().getInstruction().toString());

        // updating the staticflow cache
        /*
         * Commented out due to problems caused by improper use of ClusteringServices
        Integer ordinal = staticFlowsOrdinal.get(0);
        staticFlowsOrdinal.put(0, ++ordinal);
        staticFlows.put(ordinal, dataObject);
        */

        // We send flow to the sounthbound plugin
        flowService.removeFlow(input.build());
        /*
         * Commented out due to problems caused by improper use of ClusteringServices
        updateLocalDatabase((NodeFlow) dataObject, false);
        */
    }

    /**
     * Update flow to the southbound plugin and our internal database
     *
     * @param path
     * @param dataObject
     */
    private void updateFlow(InstanceIdentifier<?> path, Flow dataObject) {

        UpdateFlowInputBuilder input = new UpdateFlowInputBuilder();
        UpdatedFlowBuilder updatedflowbuilder = new UpdatedFlowBuilder();
        updatedflowbuilder.fieldsFrom(dataObject);
        input.setNode(dataObject.getNode());
        input.setUpdatedFlow(updatedflowbuilder.build());

        // updating the staticflow cache
        /*
         * Commented out due to problems caused by improper use of ClusteringServices.
        Integer ordinal = staticFlowsOrdinal.get(0);
        staticFlowsOrdinal.put(0, ++ordinal);
        staticFlows.put(ordinal, dataObject);
        */

        // We send flow to the sounthbound plugin
        flowService.updateFlow(input.build());
        /*
         * Commented out due to problems caused by improper use of ClusteringServices.
        updateLocalDatabase((NodeFlow) dataObject, true);
        */
    }

    @SuppressWarnings("unchecked")
    private void commitToPlugin(internalTransaction transaction) {
        Set<Entry<InstanceIdentifier<?>, DataObject>> createdEntries = transaction.getModification().getCreatedConfigurationData().entrySet();

        /*
         * This little dance is because updatedEntries contains both created and modified entries
         * The reason I created a new HashSet is because the collections we are returned are immutable.
         */
        Set<Entry<InstanceIdentifier<?>, DataObject>> updatedEntries = new HashSet<Entry<InstanceIdentifier<?>, DataObject>>();
        updatedEntries.addAll(transaction.getModification().getUpdatedConfigurationData().entrySet());
        updatedEntries.removeAll(createdEntries);

        Set<InstanceIdentifier<?>> removeEntriesInstanceIdentifiers = transaction.getModification().getRemovedConfigurationData();
        transaction.getModification().getOriginalConfigurationData();
        for (Entry<InstanceIdentifier<?>, DataObject> entry : createdEntries) {
            if(entry.getValue() instanceof Flow) {
                System.out.println("Coming add cc in FlowDatacommitHandler");
                addFlow(entry.getKey(), (Flow) entry.getValue());
            }
        }
        for (@SuppressWarnings("unused")
        Entry<InstanceIdentifier<?>, DataObject> entry : updatedEntries) {
            if(entry.getValue() instanceof Flow) {
                System.out.println("Coming update cc in FlowDatacommitHandler");
                updateFlow(entry.getKey(), (Flow) entry.getValue());
            }
        }

        for (InstanceIdentifier<?> instanceId : removeEntriesInstanceIdentifiers ) {
            DataObject removeValue = transaction.getModification().getOriginalConfigurationData().get(instanceId);
            if(removeValue instanceof Flow) {
                System.out.println("Coming remove cc in FlowDatacommitHandler");
                removeFlow(instanceId, (Flow) removeValue);

            }
        }

    }

    private final class FlowDataCommitHandler implements DataCommitHandler<InstanceIdentifier<?>, DataObject> {

        @SuppressWarnings("unchecked")
        @Override
        public DataCommitTransaction requestCommit(DataModification<InstanceIdentifier<?>, DataObject> modification) {
            // We should verify transaction
            System.out.println("Coming in FlowDatacommitHandler");
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

        Map<InstanceIdentifier<?>, Flow> additions = new HashMap<>();
        Map<InstanceIdentifier<?>, Flow> updates = new HashMap<>();
        Map<InstanceIdentifier<?>, Flow> removals = new HashMap<>();

        /**
         * We create a plan which flows will be added, which will be updated and
         * which will be removed based on our internal state.
         *
         */
        void prepareUpdate() {

            Set<Entry<InstanceIdentifier<?>, DataObject>> puts = modification.getUpdatedConfigurationData().entrySet();
            for (Entry<InstanceIdentifier<?>, DataObject> entry : puts) {

                // validating the DataObject
                DataObject value = entry.getValue();
                if(value instanceof Flow ) {
                    Flow flow = (Flow)value;
                    boolean status = validate(flow);
                    if (!status) {
                        return;
                    }
                    // Presence check
                    /*
                     * This is breaking due to some improper use of caches...
                     *
                    if (flowEntryExists(flow)) {
                        String error = "Entry with this name on specified table already exists";
                        logger.warn("Entry with this name on specified table already exists: {}", entry);
                        logger.error(error);
                        return;
                    }
                    if (originalSwView.containsKey(entry)) {
                        logger.warn("Operation Rejected: A flow with same match and priority exists on the target node");
                        logger.trace("Aborting to install {}", entry);
                        continue;
                    }
                    */
                    if (!FRMUtil.validateMatch(flow)) {
                        logger.error("Not a valid Match");
                        return;
                    }
                    if (!FRMUtil.validateInstructions(flow)) {
                        logger.error("Not a valid Instruction");
                        return;
                    }
                    /*
                     * Commented out due to Clustering Services issues
                     * preparePutEntry(entry.getKey(), flow);
                     */
                }
            }

            // removals = modification.getRemovedConfigurationData();
            Set<InstanceIdentifier<?>> removedData = modification.getRemovedConfigurationData();
            for (InstanceIdentifier<?> removal : removedData) {
                DataObject value = modification.getOriginalConfigurationData().get(removal);
                if (value instanceof Flow) {
                    removals.put(removal, (Flow) value);
                }
            }

        }

        private void preparePutEntry(InstanceIdentifier<?> key, Flow flow) {
            Flow original = originalSwView.get(key);
            if (original != null) {
                // It is update for us
                System.out.println("Coming update  in FlowDatacommitHandler");
                updates.put(key, flow);
            } else {
                // It is addition for us
                System.out.println("Coming add in FlowDatacommitHandler");
                additions.put(key, flow);
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
            return Rpcs.getRpcResult(true, null, Collections.<RpcError>emptySet());
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
            return Rpcs.getRpcResult(true, null, Collections.<RpcError>emptySet());

        }

        public boolean validate(Flow flow) {

            String msg = ""; // Specific part of warn/error log

            boolean result  = true;
            // flow Name validation
            if (flow.getFlowName() == null || flow.getFlowName().trim().isEmpty()
                    || !flow.getFlowName().matches(NAMEREGEX)) {
                msg = "Invalid Flow name";
                result = false;
            }
            // Node Validation
            if (result == true && flow.getNode() == null) {
                msg = "Node is null";
                result = false;
            }

            // TODO: Validate we are seeking to program a flow against a valid Node

            if (result == true && flow.getPriority() != null) {
                if (flow.getPriority() < 0 || flow.getPriority() > 65535) {
                    msg = String.format("priority %s is not in the range 0 - 65535",
                            flow.getPriority());
                    result = false;
                }
            }
            if (result == false) {
                logger.warn("Invalid Configuration for flow {}. The failure is {}",flow,msg);
                logger.error("Invalid Configuration ({})",msg);
            }
            return result;
        }

        private boolean flowEntryExists(Flow flow) {
            // Flow name has to be unique on per table id basis
            for (ConcurrentMap.Entry<FlowKey, Flow> entry : originalSwView.entrySet()) {
                if (entry.getValue().getFlowName().equals(flow.getFlowName())
                        && entry.getValue().getTableId().equals(flow.getTableId())) {
                    return true;
                }
            }
            return false;
        }
    }

    final class FlowEventListener implements SalFlowListener {

        List<FlowAdded> addedFlows = new ArrayList<>();
        List<FlowRemoved> removedFlows = new ArrayList<>();
        List<FlowUpdated> updatedFlows = new ArrayList<>();

        @Override
        public void onFlowAdded(FlowAdded notification) {
            System.out.println("added flow..........................");
            addedFlows.add(notification);
        }

        @Override
        public void onFlowRemoved(FlowRemoved notification) {
            removedFlows.add(notification);
        };

        @Override
        public void onFlowUpdated(FlowUpdated notification) {
            updatedFlows.add(notification);
        }

        @Override
        public void onSwitchFlowRemoved(SwitchFlowRemoved notification) {
            // TODO
        }

        @Override
        public void onNodeErrorNotification(NodeErrorNotification notification) {
            // TODO Auto-generated method stub

        }

        @Override
        public void onNodeExperimenterErrorNotification(NodeExperimenterErrorNotification notification) {
            // TODO Auto-generated method stub

        };

    }

    // Commented out DataChangeListene - to be used by Stats

    // final class FlowDataListener implements DataChangeListener {
    // private SalFlowService flowService;
    //
    // public FlowDataListener() {
    //
    // }
    //
    // @Override
    // public void onDataChanged(
    // DataChangeEvent<InstanceIdentifier<?>, DataObject> change) {
    // System.out.println("Coming in onDataChange..............");
    // @SuppressWarnings("unchecked")
    // Collection<DataObject> additions = (Collection<DataObject>)
    // change.getCreatedConfigurationData();
    // // we can check for getCreated, getDeleted or getUpdated from DataChange
    // Event class
    // for (DataObject dataObject : additions) {
    // if (dataObject instanceof NodeFlow) {
    // NodeRef nodeOne = createNodeRef("foo:node:1");
    // // validating the dataObject here
    // AddFlowInputBuilder input = new AddFlowInputBuilder();
    // input.setNode(((NodeFlow) dataObject).getNode());
    // input.setNode(nodeOne);
    // // input.setPriority(((NodeFlow) dataObject).getPriority());
    // //input.setMatch(((NodeFlow) dataObject).getMatch());
    // //input.setFlowTable(((NodeFlow) dataObject).getFlowTable());
    // //input.setCookie(((NodeFlow) dataObject).getCookie());
    // //input.setAction(((NodeFlow) dataObject).getAction());
    //
    // @SuppressWarnings("unused")
    // Future<RpcResult<java.lang.Void>> result =
    // flowService.addFlow(input.build());
    // }
    // }
    // }
    // }

    private static void updateLocalDatabase(NodeFlow entry, boolean add) {

        updateSwViewes(entry, add);

        updateNodeFlowsDB(entry, add);

    }

    /*
     * Update the node mapped flows database
     */
    private static void updateSwViewes(NodeFlow entry, boolean add) {
        if (add) {
            FlowConsumerImpl.originalSwView.put((FlowKey) entry, (Flow) entry);
            installedSwView.put((FlowKey) entry, (Flow) entry);
        } else {
            originalSwView.remove(entry);
            installedSwView.remove(entry);

        }
    }

    @Override
    public List<DataObject> get() {

        List<DataObject> orderedList = new ArrayList<DataObject>();
        ConcurrentMap<Integer, Flow> flowMap = staticFlows;
        int maxKey = staticFlowsOrdinal.get(0).intValue();
        for (int i = 0; i <= maxKey; i++) {
            Flow entry = flowMap.get(i);
            if (entry != null) {
                orderedList.add(entry);
            }
        }
        return orderedList;
    }

    @Override
    public DataObject getWithName(String name, org.opendaylight.controller.sal.core.Node n) {
        if (this instanceof FlowConsumerImpl) {
            for (ConcurrentMap.Entry<Integer, Flow> flowEntry : staticFlows.entrySet()) {
                Flow flow = flowEntry.getValue();
                if (flow.getNode().equals(n) && flow.getFlowName().equals(name)) {

                    return flowEntry.getValue();
                }
            }
        }
        return null;
    }

    /*
     * Update the node mapped flows database
     */
    private static void updateNodeFlowsDB(NodeFlow entry, boolean add) {
        Node node = (Node) entry.getNode();

        List<Flow> nodeIndeces = nodeFlows.get(node);
        if (nodeIndeces == null) {
            if (!add) {
                return;
            } else {
                nodeIndeces = new ArrayList<Flow>();
            }
        }

        if (add) {
            nodeIndeces.add((Flow) entry);
        } else {
            nodeIndeces.remove(entry);
        }

        // Update cache across cluster
        if (nodeIndeces.isEmpty()) {
            nodeFlows.remove(node);
        } else {
            nodeFlows.put(node, nodeIndeces);
        }
    }

    private static NodeRef createNodeRef(String string) {
        NodeKey key = new NodeKey(new NodeId(string));
        InstanceIdentifier<Node> path = InstanceIdentifier.builder().node(Nodes.class).node(Node.class, key)
                .toInstance();

        return new NodeRef(path);
    }
}
