package org.opendaylight.controller.messagebus.registration;

import org.opendaylight.yangtools.concepts.AbstractObjectRegistration;

/**
 * Registration of EventSource have to be provided by object implements this interface
 * @author madamjak
 *
 * @param <T>
 */
public interface EventSourceRegistry<T> extends AutoCloseable {
	/**
	 * implementation has to do all necessary procedures to register given event source
	 * insert into Event source topology and register DataChangeListener and Routed RPC is a minimal
	 * @param es
	 * @return
	 */
	public AbstractObjectRegistration<EventSource<T>> registerEventSource(EventSource<T> es);
	/**
	 * implementation has to be able to find corresponding AbstractObjectRegistration object
	 * and use removeRegistration method to unregister event source
	 * @param es
	 */
	void unRegisterEventSource(EventSource<T> es);
}
