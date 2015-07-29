package org.opendaylight.controller.config.yang.md.sal.rest.docgen;

import org.opendaylight.controller.config.api.DependencyResolver;
import org.opendaylight.controller.config.api.ModuleIdentifier;
import org.opendaylight.controller.sal.core.api.Broker;
import org.opendaylight.controller.sal.rest.doc.DocgenProviderImpl;
import org.osgi.framework.BundleContext;

public class RestDocgenModule extends org.opendaylight.controller.config.yang.md.sal.rest.docgen.AbstractRestDocgenModule {

    private BundleContext bundleContext;

    /**
     * @param identifier
     * @param dependencyResolver
     */
    public RestDocgenModule(final org.opendaylight.controller.config.api.ModuleIdentifier identifier,
                               final org.opendaylight.controller.config.api.DependencyResolver dependencyResolver) {
        super(identifier, dependencyResolver);
    }

    /**
     * Constructor is made to hold BundleContext for {@link DocgenProviderImpl}
     *
     * @param identifier
     * @param dependencyResolver
     * @param bundleContext
     * @param oldInstance
     * @param oldModule
     */
    public RestDocgenModule(final org.opendaylight.controller.config.api.ModuleIdentifier identifier,
                               final org.opendaylight.controller.config.api.DependencyResolver dependencyResolver,
                               final RestDocgenModule oldModule, final AutoCloseable oldInstance, final BundleContext bundleContext) {
        super(identifier, dependencyResolver, oldModule, oldInstance);
        this.bundleContext = bundleContext;
    }

    /**
     * @param identifier
     * @param dependencyResolver
     * @param oldModule
     * @param oldInstance
     */
    public RestDocgenModule(final org.opendaylight.controller.config.api.ModuleIdentifier identifier,
                               final org.opendaylight.controller.config.api.DependencyResolver dependencyResolver,
                               final org.opendaylight.controller.config.yang.md.sal.rest.docgen.RestDocgenModule oldModule,
                               final java.lang.AutoCloseable oldInstance) {
        super(identifier, dependencyResolver, oldModule, oldInstance);
    }

    /**
     * Constructor is made to hold BundleContext for {@link DocgenProviderImpl}
     *
     * @param identifier
     * @param dependencyResolver
     * @param bundleContext
     */
    public RestDocgenModule(final ModuleIdentifier identifier, final DependencyResolver dependencyResolver,
                               final BundleContext bundleContext) {
        this(identifier, dependencyResolver);
        this.bundleContext = bundleContext;
    }


    @Override
    public void customValidation() {
        // add custom validation form module attributes here.
    }

    @Override
    public java.lang.AutoCloseable createInstance() {
        // Create an instance of our provider
        final DocgenProviderImpl instance = new DocgenProviderImpl(bundleContext);

        getDomBrokerDependency().registerProvider(instance);

        return instance;
    }

}
