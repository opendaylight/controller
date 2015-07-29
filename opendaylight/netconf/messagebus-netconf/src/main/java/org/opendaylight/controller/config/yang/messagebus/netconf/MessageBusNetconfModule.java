package org.opendaylight.controller.config.yang.messagebus.netconf;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.MountPointService;
import org.opendaylight.controller.md.sal.dom.api.DOMMountPointService;
import org.opendaylight.controller.md.sal.dom.api.DOMNotificationPublishService;
import org.opendaylight.controller.messagebus.app.util.Providers;
import org.opendaylight.controller.messagebus.eventsources.netconf.NetconfEventSourceManager;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker;
import org.opendaylight.controller.sal.core.api.Broker;

public class MessageBusNetconfModule extends org.opendaylight.controller.config.yang.messagebus.netconf.AbstractMessageBusNetconfModule {
    public MessageBusNetconfModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier, org.opendaylight.controller.config.api.DependencyResolver dependencyResolver) {
        super(identifier, dependencyResolver);
    }

    public MessageBusNetconfModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier, org.opendaylight.controller.config.api.DependencyResolver dependencyResolver, org.opendaylight.controller.config.yang.messagebus.netconf.MessageBusNetconfModule oldModule, java.lang.AutoCloseable oldInstance) {
        super(identifier, dependencyResolver, oldModule, oldInstance);
    }

    @Override
    public void customValidation() {}

    @Override
    public java.lang.AutoCloseable createInstance() {
        final BindingAwareBroker.ProviderContext bindingCtx = getBindingBrokerDependency().registerProvider(new Providers.BindingAware());
        final Broker.ProviderSession domCtx = getDomBrokerDependency().registerProvider(new Providers.BindingIndependent());

        final MountPointService mountPointService = bindingCtx.getSALService(MountPointService.class);
        final DataBroker dataBroker = bindingCtx.getSALService(DataBroker.class);

        final DOMNotificationPublishService domPublish = domCtx.getService(DOMNotificationPublishService.class);
        final DOMMountPointService domMount = domCtx.getService(DOMMountPointService.class);

        return NetconfEventSourceManager.create(dataBroker, domPublish, domMount,
            mountPointService, getEventSourceRegistryDependency(), getNamespaceToStream());
    }

}
