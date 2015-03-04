package org.opendaylight.controller.messagebus.registration;

import org.opendaylight.controller.md.sal.binding.api.DataChangeListener;
import org.opendaylight.controller.sal.core.api.notify.NotificationListener;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.messagebus.eventsource.rev141202.EventSourceService;
import org.opendaylight.yangtools.yang.binding.BaseIdentity;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

/**
 * All sources of events (notifications) have to implement this interface. 
 * Implementations of methods give necessary information for registration sources of events for message-bus
 * @author madamjak
 *
 * @param <T>
 */
public interface EventSource<T> extends EventSourceService, NotificationListener, DataChangeListener {
	/**
	 * InstanceIdentifier is used for DataChangeListener registration 
	 * @return
	 */
	public InstanceIdentifier<?> getInstanceIdentifier();
	/**
	 * Source is used to registration of EventSource in an EventSource Topology
	 * @return
	 */
	public T getSource();
	/**
	 * It represents base identity for routed RPC registration 
	 * @return
	 */
	public Class<? extends BaseIdentity> getRpcPathBaseIdentity();
	/**
	 * It is used to registration path in RPC registration process
	 * @return
	 */
	public InstanceIdentifier<?> getRpcPathInstanceIdentifier();
}