package org.opendaylight.controller.sal.binding.impl;

import org.opendaylight.controller.sal.binding.api.BindingAwareBroker.ConsumerContext;
import org.opendaylight.controller.sal.binding.api.BindingAwareService;
import org.opendaylight.yangtools.yang.binding.RpcService;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.slf4j.LoggerFactory

class OsgiConsumerContext implements ConsumerContext {

	static val log = LoggerFactory.getLogger(OsgiConsumerContext)
	protected val BundleContext bundleContext;
	protected val BindingAwareBrokerImpl broker;
	
	new(BundleContext ctx,BindingAwareBrokerImpl broker) {
		this.bundleContext = ctx;
		this.broker = broker;
	}

	
	override def <T extends BindingAwareService> getSALService(Class<T> service) {
		// SAL Services are global
		var ref =  bundleContext.getServiceReference(service);
		return bundleContext.getService(ref) as T;
	}
	
	

	override def <T extends RpcService> T getRpcService(Class<T> module) {
		try {
			
			val services = bundleContext.getServiceReferences(module, getProxyFilter());
			
			// Proxy service found / using first implementation
			// FIXME: Add advanced logic to retrieve service with right set of models
			if(false == services.empty) {
				val ref = services.iterator().next() as ServiceReference<T>;
				return bundleContext.getService(ref) as T;
			} 
		} catch (InvalidSyntaxException e) {
			log.error("Created filter was invalid:", e.message,e)
		}
		return null;
		

	}

	private def getProxyFilter() {
		return '''(«Constants.SAL_SERVICE_TYPE»=«Constants.SAL_SERVICE_TYPE_CONSUMER_PROXY»)'''
	}
}
