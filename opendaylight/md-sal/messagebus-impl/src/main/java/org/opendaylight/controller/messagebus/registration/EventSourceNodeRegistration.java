package org.opendaylight.controller.messagebus.registration;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataChangeListener;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.mdsal.MdSAL;
import org.opendaylight.controller.messagebus.app.impl.EventSourceTopology;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.messagebus.eventsource.rev141202.EventSourceService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EventSourceNodeRegistration extends EventSourceRegistration<Node> {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(EventSourceNodeRegistration.class);
	
	private static LogicalDatastoreType dataStoreType = LogicalDatastoreType.OPERATIONAL;
		
	public static EventSourceNodeRegistration register(MdSAL mdSal,
            EventSourceTopology eventSourceTopology, EventSource<Node> instance){
		
		EventSourceNodeRegistration esr = new EventSourceNodeRegistration(instance);
		esr.registration( mdSal, eventSourceTopology);
		LOGGER.debug("EventSource {} was registered sucessfuly.",instance.getSource());
		return esr;
		
	}
	
	private ListenerRegistration<DataChangeListener> listenerRegistration;
	private EventSourceTopology eventSourceTopology;
	private BindingAwareBroker.RoutedRpcRegistration<EventSourceService> rpcRegistration;
	
	private EventSourceNodeRegistration(EventSource<Node> instance) {
		super(instance);
	}
	
	private void registration(MdSAL mdSal,
            				  EventSourceTopology eventSourceTopology){

		EventSource<Node> instance = getInstance();
		
		listenerRegistration = mdSal.getDataBroker().registerDataChangeListener(dataStoreType,
				instance.getInstanceIdentifier(), instance, DataBroker.DataChangeScope.SUBTREE);
		
		rpcRegistration = mdSal.getBindingAwareContext().addRoutedRpcImplementation(EventSourceService.class, instance);
		rpcRegistration.registerPath(instance.getRpcPathBaseIdentity(), instance.getRpcPathInstanceIdentifier());
		
		this.eventSourceTopology = eventSourceTopology;
		this.eventSourceTopology.insert(instance.getSource());	
		
	}
	
	public EventSource<Node> getEventSource(){
		return getInstance();
	}
	
	@Override
	protected void removeRegistration() {
		
		EventSource<Node> instance = getInstance();
		
		if(listenerRegistration != null){
			listenerRegistration.close();
		}
		if(eventSourceTopology != null){
			eventSourceTopology.remove(instance.getSource());
		}
		if(rpcRegistration != null){
			rpcRegistration.unregisterPath(instance.getRpcPathBaseIdentity(), instance.getRpcPathInstanceIdentifier());;
		}
		LOGGER.debug("EventSource {} was unregistered sucessfuly.",instance.getSource());
	}
	
	
}

