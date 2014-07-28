package org.opendaylight.controller.config.yang.md.sal.rest.connector;

import org.opendaylight.controller.sal.rest.impl.RestconfProviderImpl;
import org.osgi.framework.BundleContext;

public class RestConnectorModule extends org.opendaylight.controller.config.yang.md.sal.rest.connector.AbstractRestConnectorModule {

    private BundleContext bundleContext;

    public RestConnectorModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier, org.opendaylight.controller.config.api.DependencyResolver dependencyResolver) {
        super(identifier, dependencyResolver);
    }

    public RestConnectorModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier, org.opendaylight.controller.config.api.DependencyResolver dependencyResolver, org.opendaylight.controller.config.yang.md.sal.rest.connector.RestConnectorModule oldModule, java.lang.AutoCloseable oldInstance) {
        super(identifier, dependencyResolver, oldModule, oldInstance);
    }

    public void setBundleContext(final BundleContext bundleContext) {
        this.bundleContext = bundleContext;
    }

    public BundleContext getBundleContext() {
        return bundleContext;
    }

    @Override
    public void customValidation() {
        // add custom validation form module attributes here.
    }

    @Override
    public java.lang.AutoCloseable createInstance() {
        // Create an instance of our provider
        RestconfProviderImpl instance = new RestconfProviderImpl();
        // Set its port
        instance.setWebsocketPort(getWebsocketPort());

        instance.start(bundleContext);
        // Register it with the Broker
        getDomBrokerDependency().registerProvider(instance);
        return instance;
    }
}
