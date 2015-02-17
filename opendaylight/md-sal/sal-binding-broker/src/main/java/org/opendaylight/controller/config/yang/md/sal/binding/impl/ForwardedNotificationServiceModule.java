package org.opendaylight.controller.config.yang.md.sal.binding.impl;

import org.opendaylight.controller.md.sal.binding.impl.BindingToNormalizedNodeCodec;
import org.opendaylight.controller.md.sal.binding.impl.ForwardedNotificationService;
import org.opendaylight.controller.sal.binding.codegen.impl.SingletonHolder;

public class ForwardedNotificationServiceModule extends org.opendaylight.controller.config.yang.md.sal.binding.impl.AbstractForwardedNotificationServiceModule {
    public ForwardedNotificationServiceModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier, org.opendaylight.controller.config.api.DependencyResolver dependencyResolver) {
        super(identifier, dependencyResolver);
    }

    public ForwardedNotificationServiceModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier, org.opendaylight.controller.config.api.DependencyResolver dependencyResolver, org.opendaylight.controller.config.yang.md.sal.binding.impl.ForwardedNotificationServiceModule oldModule, java.lang.AutoCloseable oldInstance) {
        super(identifier, dependencyResolver, oldModule, oldInstance);
    }

    @Override
    public void customValidation() {
        // add custom validation form module attributes here.
    }

    @Override
    public java.lang.AutoCloseable createInstance() {
        final BindingToNormalizedNodeCodec codec = getBindingMappingServiceNotifDependency();
        //TODO: add DOMNotificationRouter service dependency
        return new ForwardedNotificationService(codec.getCodecRegistry(), null, SingletonHolder.INVOKER_FACTORY);
    }

}
