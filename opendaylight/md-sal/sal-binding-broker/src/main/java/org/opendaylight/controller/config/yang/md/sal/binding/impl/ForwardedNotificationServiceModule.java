package org.opendaylight.controller.config.yang.md.sal.binding.impl;

import java.util.Collection;
import org.opendaylight.controller.md.sal.binding.impl.BindingToNormalizedNodeCodec;
import org.opendaylight.controller.md.sal.binding.impl.ForwardedNotificationService;
import org.opendaylight.controller.md.sal.dom.api.DOMNotificationService;
import org.opendaylight.controller.sal.binding.codegen.impl.SingletonHolder;
import org.opendaylight.controller.sal.core.api.Broker;
import org.opendaylight.controller.sal.core.api.Provider;

public class ForwardedNotificationServiceModule extends AbstractForwardedNotificationServiceModule implements Provider {
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
        final Broker.ProviderSession session = getDomAsyncBrokerNotifDependency().registerProvider(this);
        final DOMNotificationService notifService = session.getService(DOMNotificationService.class);
        return new ForwardedNotificationService(codec.getCodecRegistry(), notifService, SingletonHolder.INVOKER_FACTORY);
    }

    @Override
    public void onSessionInitiated(Broker.ProviderSession session) {

    }

    @Override
    public Collection<ProviderFunctionality> getProviderFunctionality() {
        return null;
    }
}
