package org.opendaylight.controller.messagebus.registration;

import org.opendaylight.yangtools.concepts.AbstractObjectRegistration;

public abstract class EventSourceRegistration<T> extends AbstractObjectRegistration<EventSource<T>>  {
		
	protected EventSourceRegistration(EventSource<T> instance) {
		super(instance);
	} 

}
