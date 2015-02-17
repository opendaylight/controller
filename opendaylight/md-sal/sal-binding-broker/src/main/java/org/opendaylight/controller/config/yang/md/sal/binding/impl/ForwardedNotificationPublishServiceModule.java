package org.opendaylight.controller.config.yang.md.sal.binding.impl;

import java.util.Collection;
import org.opendaylight.controller.md.sal.binding.impl.BindingToNormalizedNodeCodec;
import org.opendaylight.controller.md.sal.binding.impl.ForwardedNotificationPublishService;
import org.opendaylight.controller.md.sal.dom.api.DOMNotificationPublishService;
import org.opendaylight.controller.sal.core.api.Broker;
import org.opendaylight.controller.sal.core.api.Provider;

public class ForwardedNotificationPublishServiceModule extends org.opendaylight.controller.config.yang.md.sal.binding.impl.AbstractForwardedNotificationPublishServiceModule
        implements Provider {
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
        final BindingToNormalizedNodeCodec codec = getBindingMappingServiceNotifPubDependency();
        final Broker.ProviderSession session = getDomAsyncBrokerNotifPubDependency().registerProvider(this);
        final DOMNotificationPublishService publishService = session.getService(DOMNotificationPublishService.class);
        return new ForwardedNotificationPublishService(codec.getCodecRegistry(), publishService);
    }

    @Override
    public void onSessionInitiated(Broker.ProviderSession session) {

    }

    @Override
    public Collection<ProviderFunctionality> getProviderFunctionality() {
        return null;
    }
}
