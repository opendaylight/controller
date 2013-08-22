package org.opendaylight.controller.sample.toaster.provider;
import java.util.Collection;
import java.util.Collections;


import org.opendaylight.controller.sal.binding.api.AbstractBindingAwareProvider;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker;
import org.opendaylight.controller.sal.binding.api.BindingAwareProvider;
import org.opendaylight.controller.sal.binding.api.NotificationProviderService;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker.ConsumerContext;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker.ProviderContext;
import org.opendaylight.yang.gen.v1.http.netconfcentral.org.ns.toaster.rev20091120.ToasterService;
import org.opendaylight.yangtools.yang.binding.RpcService;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class ToasterProvider extends AbstractBindingAwareProvider {
    private static final Logger log = LoggerFactory.getLogger(ToasterProvider.class);

	private ConsumerContext consumerContext;
	private ProviderContext providerContext;
	private OpendaylightToaster toaster;
	
	
	public ToasterProvider() {
		toaster = new OpendaylightToaster();
	}
	
	@Override
	public void onSessionInitialized(ConsumerContext session) {
		log.info("Consumer Session initialized");
		this.consumerContext = session;

	}

	@Override
	public void onSessionInitiated(ProviderContext session) {
		log.info("Provider Session initialized");
		
		this.providerContext = session;
		toaster.setNotificationProvider(session.getSALService(NotificationProviderService.class));
		providerContext.addRpcImplementation(ToasterService.class, toaster);
	}	
	
	
	@Override
	public Collection<? extends RpcService> getImplementations() {
		return Collections.emptySet();
	}

	@Override
	public Collection<? extends ProviderFunctionality> getFunctionality() {
		return Collections.emptySet();
	}
	
	@Override
	@Deprecated
	protected void startImpl(BundleContext context) {
	    // TODO Auto-generated method stub
	    
	}
}
