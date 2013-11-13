package org.opendaylight.controller.sal.binding.impl.connect.dom;

import java.util.Collection;
import java.util.Collections;

import javassist.ClassPool;

import org.opendaylight.controller.sal.binding.api.data.DataProviderService;
import org.opendaylight.controller.sal.binding.dom.serializer.impl.TransformerGenerator;
import org.opendaylight.controller.sal.core.api.Broker;
import org.opendaylight.controller.sal.core.api.Provider;
import org.opendaylight.controller.sal.core.api.Broker.ProviderSession;
import org.opendaylight.controller.sal.core.api.data.DataBrokerService;
import org.opendaylight.controller.sal.core.api.model.SchemaService;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

public class ConnectorActivator implements Provider, ServiceTrackerCustomizer<Broker, Broker> {

    BindingIndependentDataServiceConnector dataConnector;
    BindingIndependentMappingService mappingService;

    private final DataProviderService baDataService;
    private BundleContext context;

    private ServiceTracker<Broker, Broker> brokerTracker;

    public ConnectorActivator(DataProviderService dataService, BundleContext context) {
        baDataService = dataService;
        this.context = context;
        brokerTracker = new ServiceTracker<>(context, Broker.class, this);
    }

    @Override
    public Collection<ProviderFunctionality> getProviderFunctionality() {
        return Collections.emptySet();
    }

    @Override
    public void onSessionInitiated(ProviderSession session) {

        RuntimeGeneratedMappingServiceImpl mappingImpl = new RuntimeGeneratedMappingServiceImpl();
        SchemaService schemaService = (session.getService(SchemaService.class));
        ClassPool pool = new ClassPool();
        mappingImpl.setBinding(new TransformerGenerator(pool));
        mappingImpl.start();
        schemaService.registerSchemaServiceListener(mappingImpl);
        mappingService = mappingImpl;
        dataConnector = new BindingIndependentDataServiceConnector();
        dataConnector.setBaDataService(baDataService);
        dataConnector.setBiDataService(session.getService(DataBrokerService.class));
        dataConnector.setMappingService(mappingService);
        dataConnector.start();
    }

    @Override
    public Broker addingService(ServiceReference<Broker> reference) {
        Broker br= context.getService(reference);
        br.registerProvider(this, context);
        return br;
    }

    @Override
    public void modifiedService(ServiceReference<Broker> reference, Broker service) {
        // NOOP
    }

    @Override
    public void removedService(ServiceReference<Broker> reference, Broker service) {
        // NOOP
    }

    public void start() {
        brokerTracker.open();
    }
}
