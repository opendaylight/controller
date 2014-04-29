

package org.opendaylight.controller.config.yang.test.impl;
public class NetconfTestImplModule extends org.opendaylight.controller.config.yang.test.impl.AbstractNetconfTestImplModule {
    public NetconfTestImplModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier, org.opendaylight.controller.config.api.DependencyResolver dependencyResolver) {
        super(identifier, dependencyResolver);
    }

    public NetconfTestImplModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier, org.opendaylight.controller.config.api.DependencyResolver dependencyResolver, org.opendaylight.controller.config.yang.test.impl.NetconfTestImplModule oldModule, java.lang.AutoCloseable oldInstance) {
        super(identifier, dependencyResolver, oldModule, oldInstance);
    }

    @Override
    public void customValidation() {
        // add custom validation form module attributes here.
    }

    @Override
    public java.lang.AutoCloseable createInstance() {
return NetconfTestImplModuleUtil.registerRuntimeBeans(this);

    }

}
