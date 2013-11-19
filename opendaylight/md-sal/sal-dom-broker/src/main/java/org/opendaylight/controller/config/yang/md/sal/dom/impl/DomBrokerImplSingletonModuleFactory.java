package org.opendaylight.controller.config.yang.md.sal.dom.impl;

import java.util.Collections;
import java.util.Set;

import org.opendaylight.controller.config.api.DependencyResolver;
import org.opendaylight.controller.config.api.DependencyResolverFactory;
import org.opendaylight.controller.config.api.DynamicMBeanWithInstance;
import org.opendaylight.controller.config.api.ModuleIdentifier;
import org.opendaylight.controller.config.spi.Module;
import org.osgi.framework.BundleContext;

/**
*
*/
public class DomBrokerImplSingletonModuleFactory extends org.opendaylight.controller.config.yang.md.sal.dom.impl.AbstractDomBrokerImplSingletonModuleFactory
{

    private static final String SINGLETON_NAME = "dom-broker-singleton";
    public static ModuleIdentifier SINGLETON_IDENTIFIER = new ModuleIdentifier(NAME, SINGLETON_NAME);
    
    
    @Override
    public Module createModule(String instanceName, DependencyResolver dependencyResolver, BundleContext bundleContext) {
        throw new UnsupportedOperationException("Only default instance supported.");
    }

    @Override
    public Module createModule(String instanceName, DependencyResolver dependencyResolver,
            DynamicMBeanWithInstance old, BundleContext bundleContext) throws Exception {
        Module instance = super.createModule(instanceName, dependencyResolver, old, bundleContext);
        ((DomBrokerImplSingletonModule)instance).setBundleContext(bundleContext);
        return instance;
    }

    @Override
    public Set<DomBrokerImplSingletonModule> getDefaultModules(DependencyResolverFactory dependencyResolverFactory,
            BundleContext bundleContext) {

        DependencyResolver dependencyResolver = dependencyResolverFactory
                .createDependencyResolver(SINGLETON_IDENTIFIER);
        DomBrokerImplSingletonModule instance = new DomBrokerImplSingletonModule(SINGLETON_IDENTIFIER,
                dependencyResolver);
        instance.setBundleContext(bundleContext);

        return Collections.singleton(instance);
    }
}
