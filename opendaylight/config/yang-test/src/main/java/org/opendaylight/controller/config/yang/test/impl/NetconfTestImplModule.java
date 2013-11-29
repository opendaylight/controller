package org.opendaylight.controller.config.yang.test.impl;

/**
*
*/
public final class NetconfTestImplModule extends org.opendaylight.controller.config.yang.test.impl.AbstractNetconfTestImplModule
 {

    public NetconfTestImplModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier, org.opendaylight.controller.config.api.DependencyResolver dependencyResolver) {
        super(identifier, dependencyResolver);
    }

    public NetconfTestImplModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier, org.opendaylight.controller.config.api.DependencyResolver dependencyResolver,
            NetconfTestImplModule oldModule, java.lang.AutoCloseable oldInstance) {

        super(identifier, dependencyResolver, oldModule, oldInstance);
    }

    @Override
    protected void customValidation(){
        // Add custom validation for module attributes here.
    }

    @Override
    public java.lang.AutoCloseable createInstance() {
        return NetconfTestImplModuleUtil.registerRuntimeBeans(this);
    }
}
