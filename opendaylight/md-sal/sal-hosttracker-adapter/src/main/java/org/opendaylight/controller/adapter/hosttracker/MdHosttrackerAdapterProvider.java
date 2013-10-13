package org.opendaylight.controller.adapter.hosttracker;

import org.opendaylight.controller.sal.binding.api.AbstractBindingAwareProvider;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker.ProviderContext;
import org.opendaylight.controller.sal.binding.api.NotificationProviderService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.hosttracker.rev131013.OpendaylightHosttrackerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MdHosttrackerAdapterProvider extends AbstractBindingAwareProvider {

	private static final Logger log = LoggerFactory.getLogger(MdHosttrackerAdapterProvider.class);

    private ProviderContext providerContext;
    private MdHostTrackerAdapter adapter;

    public MdHosttrackerAdapterProvider() {
    	adapter = new MdHostTrackerAdapter();
    }

	@Override
	public void onSessionInitiated(ProviderContext session) {
	    log.info("Provider Session initialization started");

        this.providerContext = session;
        adapter.setNotificationProvider(session.getSALService(NotificationProviderService.class));
        log.info("NotificationProvider set");
        providerContext.addRpcImplementation(OpendaylightHosttrackerService.class, adapter);
        log.info("RPC Implementation provided");
        log.info("Provider Session initialization finished");

	}

}
