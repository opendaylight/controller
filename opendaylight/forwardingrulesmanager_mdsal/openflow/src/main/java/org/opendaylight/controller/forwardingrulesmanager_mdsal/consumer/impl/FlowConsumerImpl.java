package org.opendaylight.controller.forwardingrulesmanager_mdsal.consumer.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;

import org.opendaylight.controller.md.sal.common.api.data.DataChangeEvent;
import org.opendaylight.controller.md.sal.common.api.data.DataCommitHandler;
import org.opendaylight.controller.md.sal.common.api.data.DataCommitHandler.DataCommitTransaction;
import org.opendaylight.controller.md.sal.common.api.data.DataModification;
import org.opendaylight.controller.sal.binding.api.data.DataChangeListener;
import org.opendaylight.controller.sal.common.util.Rpcs;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.config.rev130819.Flows;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.config.rev130819.flows.Flow;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.service.rev130819.AddFlowInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.service.rev130819.AddFlowInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.service.rev130819.FlowAdded;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.service.rev130819.FlowRemoved;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.service.rev130819.FlowUpdated;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.service.rev130819.NodeFlow;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.service.rev130819.SalFlowListener;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.service.rev130819.SalFlowService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.Nodes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.config.rev130819.flows.FlowKey;
import org.opendaylight.yangtools.concepts.Registration;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.binding.NotificationListener;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FlowConsumerImpl {
	protected static final Logger logger = LoggerFactory.getLogger(FlowConsumerImpl.class);
	private FlowEventListener flowEventListener = new FlowEventListener();
    private Registration<NotificationListener> listener1Reg;
	private SalFlowService flowService;
	private FlowDataListener listener;
	private FlowDataCommitHandler commitHandler;
	private ConcurrentHashMap<FlowKey, Flow> originalSwView;
	
    public FlowConsumerImpl() {    	
		InstanceIdentifier<? extends DataObject> path = InstanceIdentifier.builder().node(Flows.class).toInstance();
    	flowService = FRMConsumerImpl.getProviderSession().getRpcService(SalFlowService.class);
		
		if (null == flowService) {
			logger.error("Consumer SAL Service is down or NULL. FRM may not function as intended");
        	System.out.println("Consumer SAL Service is down or NULL.");
        	return;
		}
		
		listener = new FlowDataListener();
		if (null == FRMConsumerImpl.getDataBrokerService().registerDataChangeListener(path, listener)) {
			logger.error("Failed to listen on flow data modifcation events");
        	System.out.println("Consumer SAL Service is down or NULL.");
        	return;
		}	
			
		// For switch events
		listener1Reg = FRMConsumerImpl.getNotificationService().registerNotificationListener(flowEventListener);
		
		if (null == listener1Reg) {
			logger.error("Listener to listen on flow data modifcation events");
        	System.out.println("Consumer SAL Service is down or NULL.");
        	return;
		}
		addFlowTest();
		System.out.println("-------------------------------------------------------------------");
		allocateCaches();
		commitHandler = new FlowDataCommitHandler();
		FRMConsumerImpl.getDataProviderService().registerCommitHandler(path, commitHandler);
    }
    
    private void allocateCaches() {
    	originalSwView = new ConcurrentHashMap<FlowKey, Flow>();
    }
    
    private void addFlowTest()
    {
    	try {
			NodeRef nodeOne = createNodeRef("foo:node:1");
			AddFlowInputBuilder input1 = new AddFlowInputBuilder();
			
			input1.setNode(nodeOne);
			AddFlowInput firstMsg = input1.build();
			
			if(null != flowService) {
				System.out.println(flowService.toString());
			}
			else
			{
				System.out.println("ConsumerFlowService is NULL");
			}
			@SuppressWarnings("unused")
			Future<RpcResult<java.lang.Void>> result1 = flowService.addFlow(firstMsg);
			
			
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
        input.setNode((dataObject).getNode());
        input.setPriority((dataObject).getPriority());
        input.setMatch((dataObject).getMatch());
        input.setCookie((dataObject).getCookie());
        input.setAction((dataObject).getAction());

        // We send flow to the sounthbound plugin
        flowService.addFlow(input.build());
    }
    
    private void commitToPlugin(internalTransaction transaction) {
        for(Entry<InstanceIdentifier<?>, Flow> entry :transaction.additions.entrySet()) {
            addFlow(entry.getKey(),entry.getValue());
        }
        for(@SuppressWarnings("unused") Entry<InstanceIdentifier<?>, Flow> entry :transaction.additions.entrySet()) {
           // updateFlow(entry.getKey(),entry.getValue());
        }
        
        for(@SuppressWarnings("unused") InstanceIdentifier<?> removal : transaction.removals) {
           // removeFlow(removal);
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
        Set<InstanceIdentifier<?>> removals = new HashSet<>();

        /**
         * We create a plan which flows will be added, which will be updated and
         * which will be removed based on our internal state.
         * 
         */
        void prepareUpdate() {

            Set<Entry<InstanceIdentifier<?>, DataObject>> puts = modification.getUpdatedConfigurationData().entrySet();
            for (Entry<InstanceIdentifier<?>, DataObject> entry : puts) {
                if (entry.getValue() instanceof Flow) {
                    Flow flow = (Flow) entry.getValue();
                    preparePutEntry(entry.getKey(), flow);
                }

            }

            removals = modification.getRemovedConfigurationData();
        }

        private void preparePutEntry(InstanceIdentifier<?> key, Flow flow) {
            Flow original = originalSwView.get(key);
            if (original != null) {
                // It is update for us
                updates.put(key, flow);
            } else {
                // It is addition for us
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
          //  return Rpcs.getRpcResult(true, null, Collections.emptySet());
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
	
	}
	
	final class FlowDataListener implements DataChangeListener {
		private SalFlowService flowService;
		
		public FlowDataListener() {
			
		}
		
		@Override
		public void onDataChanged(
				DataChangeEvent<InstanceIdentifier<?>, DataObject> change) {			
			System.out.println("Coming in onDataChange..............");
			@SuppressWarnings("unchecked")
			Collection<DataObject> additions = (Collection<DataObject>) change.getCreatedConfigurationData();
			// we can check for getCreated, getDeleted or getUpdated from DataChange Event class
			for (DataObject dataObject : additions) {
			    if (dataObject instanceof NodeFlow) {
			    	NodeRef nodeOne = createNodeRef("foo:node:1");
					// validating the dataObject here
				    AddFlowInputBuilder input = new AddFlowInputBuilder();
				    input.setNode(((NodeFlow) dataObject).getNode());
				    input.setNode(nodeOne);
					  //  input.setPriority(((NodeFlow) dataObject).getPriority());
					//input.setMatch(((NodeFlow) dataObject).getMatch());
					//input.setFlowTable(((NodeFlow) dataObject).getFlowTable());
					//input.setCookie(((NodeFlow) dataObject).getCookie());
					//input.setAction(((NodeFlow) dataObject).getAction());
	
			    	@SuppressWarnings("unused")
					Future<RpcResult<java.lang.Void>> result = flowService.addFlow(input.build());
	            }
			}	
		} 
	}
				
	    
	    
    private static NodeRef createNodeRef(String string) {
        NodeKey key = new NodeKey(new NodeId(string));
        InstanceIdentifier<Node> path = InstanceIdentifier.builder().node(Nodes.class).node(Node.class, key)
                .toInstance();

        return new NodeRef(path);
    }
	    
	  /*  private void loadFlowData() {
	
		    DataModification modification = (DataModification) dataservice.beginTransaction();
		    String id = "abc";
		    FlowKey key = new FlowKey(id, new NodeRef());
		    InstanceIdentifier<?> path1;
		    FlowBuilder flow = new FlowBuilder();
		    flow.setKey(key);
		    path1 = InstanceIdentifier.builder().node(Flows.class).node(Flow.class, key).toInstance();
		    DataObject cls = (DataObject) modification.readConfigurationData(path);
		    modification.putConfigurationData(path, flow.build());
		    modification.commit();
		}*/

}
