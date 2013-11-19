package org.opendaylight.controller.sal.rest.impl;

import java.util.Collection;
import java.util.Collections;

import org.opendaylight.controller.sal.core.api.Broker;
import org.opendaylight.controller.sal.core.api.Broker.ProviderSession;
import org.opendaylight.controller.sal.core.api.Provider;
import org.opendaylight.controller.sal.core.api.data.DataBrokerService;
import org.opendaylight.controller.sal.core.api.model.SchemaService;
import org.opendaylight.controller.sal.core.api.model.SchemaServiceListener;
import org.opendaylight.controller.sal.restconf.impl.BrokerFacade;
import org.opendaylight.controller.sal.restconf.impl.ControllerContext;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

public class RestconfProvider implements BundleActivator, Provider, ServiceTrackerCustomizer<Broker, Broker> {

    public final static String NOT_INITALIZED_MSG = "Restcof is not initialized yet. Please try again later";
    
    private ListenerRegistration<SchemaServiceListener> listenerRegistration;
    private ServiceTracker<Broker, Broker> brokerServiceTrancker;
    private BundleContext bundleContext;
    private ProviderSession session;

    @Override
    public void onSessionInitiated(ProviderSession session) {
        DataBrokerService dataService = session.getService(DataBrokerService.class);

        BrokerFacade.getInstance().setContext(session);
        BrokerFacade.getInstance().setDataService(dataService);

        SchemaService schemaService = session.getService(SchemaService.class);
        listenerRegistration = schemaService.registerSchemaServiceListener(ControllerContext.getInstance());
        ControllerContext.getInstance().setSchemas(schemaService.getGlobalContext());
    }

    @Override
    public void start(BundleContext context) throws Exception {
        bundleContext = context;
        brokerServiceTrancker = new ServiceTracker<>(context, Broker.class, this);
        brokerServiceTrancker.open();
    }

    @Override
    public void stop(BundleContext context) {
        if (listenerRegistration != null) {
            try {
                listenerRegistration.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        session.close();
        brokerServiceTrancker.close();
    }

    @Override
    public Collection<ProviderFunctionality> getProviderFunctionality() {
        return Collections.emptySet();
    }

    @Override
    public Broker addingService(ServiceReference<Broker> reference) {
        Broker broker = bundleContext.getService(reference);
        broker.registerProvider(this, bundleContext);
        return broker;
    }

    @Override
    public void modifiedService(ServiceReference<Broker> reference, Broker service) {
        // NOOP
    }

    @Override
    public void removedService(ServiceReference<Broker> reference, Broker service) {
        bundleContext.ungetService(reference);
        BrokerFacade.getInstance().setContext(null);
        BrokerFacade.getInstance().setDataService(null);
        ControllerContext.getInstance().setSchemas(null);
    }
}
