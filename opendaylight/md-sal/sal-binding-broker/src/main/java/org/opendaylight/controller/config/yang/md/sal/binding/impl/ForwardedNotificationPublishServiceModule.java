package org.opendaylight.controller.config.yang.md.sal.binding.impl;
public class ForwardedNotificationPublishServiceModule extends org.opendaylight.controller.config.yang.md.sal.binding.impl.AbstractForwardedNotificationPublishServiceModule {
    public ForwardedNotificationPublishServiceModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier, org.opendaylight.controller.config.api.DependencyResolver dependencyResolver) {
        super(identifier, dependencyResolver);
    }

    public ForwardedNotificationPublishServiceModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier, org.opendaylight.controller.config.api.DependencyResolver dependencyResolver, org.opendaylight.controller.config.yang.md.sal.binding.impl.ForwardedNotificationPublishServiceModule oldModule, java.lang.AutoCloseable oldInstance) {
        super(identifier, dependencyResolver, oldModule, oldInstance);
    }

    @Override
    public void customValidation() {
        // add custom validation form module attributes here.
    }

    @Override
    public java.lang.AutoCloseable createInstance() {
        // TODO:implement
        throw new java.lang.UnsupportedOperationException();
    }

}
