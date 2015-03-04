package org.opendaylight.controller.messagebus.registration;

import java.util.concurrent.ConcurrentHashMap;

import org.opendaylight.controller.mdsal.MdSAL;
import org.opendaylight.controller.messagebus.app.impl.EventSourceTopology;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EventSourceNodeRegistry implements EventSourceRegistry<Node> {

	private static final Logger LOGGER = LoggerFactory.getLogger(EventSourceNodeRegistry.class);
	
    private final MdSAL mdSal;
    private final EventSourceTopology eventSourceTopology;
	private final ConcurrentHashMap<EventSource<Node>, EventSourceNodeRegistration> registrationMap;
	
	public EventSourceNodeRegistry(MdSAL mdSal,
                                   EventSourceTopology eventSourceTopology) {
	     this.mdSal = mdSal;
	     this.eventSourceTopology = eventSourceTopology;
	     this.registrationMap = new ConcurrentHashMap<EventSource<Node>, EventSourceNodeRegistration>();
	     LOGGER.debug("EventSourceNodeRegistry has been initialized.");
	}
	
	@Override
	public EventSourceNodeRegistration registerEventSource(EventSource<Node> eventSource) {
		if(eventSource == null){
			LOGGER.debug("There was attempt to register NULL eventSource.");
			return null;
		}
		if(registrationMap.containsKey(eventSource)){
			LOGGER.debug("EventSource was registered before, original EventSourceNodeRegistration is returned.");
			return registrationMap.get(eventSource);
		}
		EventSourceNodeRegistration esr = EventSourceNodeRegistration.register(mdSal, eventSourceTopology, eventSource);
		registrationMap.put(eventSource, esr);
		return esr;
	}

	@Override
	public void unRegisterEventSource(EventSource<Node> eventSource) {
		if(eventSource == null){
			LOGGER.debug("There was attempt to unregister NULL eventSource.");
			return;
		}	
		EventSourceNodeRegistration esr = registrationMap.get(eventSource);
		if(esr == null){
			LOGGER.debug("No EventSourceNodeRegistration was found for given eventSource {}.", eventSource);
			return;
		}
		esr.removeRegistration();
		registrationMap.remove(eventSource);
	}

	@Override
	public void close() throws Exception {
		for(EventSourceNodeRegistration esr : registrationMap.values()){
			esr.removeRegistration();
		}
	}

}

