package org.opendaylight.controller.config.yang.md.sal.rest.connector;

import com.google.common.base.Preconditions;
import org.opendaylight.controller.config.api.DependencyResolver;
import org.opendaylight.controller.config.api.ModuleIdentifier;
import org.opendaylight.controller.sal.restconf.impl.RestconfProviderImpl;
import org.osgi.framework.BundleContext;


public class RestConnectorModule extends org.opendaylight.controller.config.yang.md.sal.rest.connector.AbstractRestConnectorModule {

    private static RestConnectorRuntimeRegistration runtimeRegistration;
    private BundleContext bundleContext;
    /**
     * @param identifier
     * @param dependencyResolver
     */
    public RestConnectorModule(final org.opendaylight.controller.config.api.ModuleIdentifier identifier,
                               final org.opendaylight.controller.config.api.DependencyResolver dependencyResolver) {
        super(identifier, dependencyResolver);
    }

    /**
     * Constructor is made to hold BundleContext for {@link RestconfProviderImpl}
     *
     * @param identifier
     * @param dependencyResolver
     * @param bundleContext
     * @param oldInstance
     * @param oldModule
     */
    public RestConnectorModule(final org.opendaylight.controller.config.api.ModuleIdentifier identifier,
                               final org.opendaylight.controller.config.api.DependencyResolver dependencyResolver,
                               final RestConnectorModule oldModule, final AutoCloseable oldInstance, final BundleContext bundleContext) {
        super(identifier, dependencyResolver, oldModule, oldInstance);
        this.bundleContext = bundleContext;
    }

    /**
     * @param identifier
     * @param dependencyResolver
     * @param oldModule
     * @param oldInstance
     */
    public RestConnectorModule(final org.opendaylight.controller.config.api.ModuleIdentifier identifier,
                               final org.opendaylight.controller.config.api.DependencyResolver dependencyResolver,
                               final org.opendaylight.controller.config.yang.md.sal.rest.connector.RestConnectorModule oldModule,
                               final java.lang.AutoCloseable oldInstance) {
        super(identifier, dependencyResolver, oldModule, oldInstance);
    }

    /**
     * Constructor is made to hold BundleContext for {@link RestconfProviderImpl}
     *
     * @param identifier
     * @param dependencyResolver
     * @param bundleContext
     */
    public RestConnectorModule(final ModuleIdentifier identifier, final DependencyResolver dependencyResolver,
                               final BundleContext bundleContext) {
        this(identifier, dependencyResolver);
        this.bundleContext = bundleContext;
    }

    @Override
    public void customValidation() {
        // add custom validation form module attributes here.
        Preconditions.checkArgument(bundleContext != null, "BundleContext was not properly set up!");
    }

    @Override
    public java.lang.AutoCloseable createInstance() {
        // Create an instance of our provider
        RestconfProviderImpl instance = new RestconfProviderImpl(bundleContext, getWebsocketPort());
        // Register it with the Broker
        getDomBrokerDependency().registerProvider(instance);

        if(runtimeRegistration != null){
            runtimeRegistration.close();
        }

        runtimeRegistration =
            getRootRuntimeBeanRegistratorWrapper().register(instance);

        return instance;
    }
}

