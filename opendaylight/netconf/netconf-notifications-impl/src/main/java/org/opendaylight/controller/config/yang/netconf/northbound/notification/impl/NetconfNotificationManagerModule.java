
package org.opendaylight.controller.config.yang.netconf.northbound.notification.impl;

import org.opendaylight.controller.netconf.notifications.impl.NetconfNotificationManager;

public class NetconfNotificationManagerModule extends org.opendaylight.controller.config.yang.netconf.northbound.notification.impl.AbstractNetconfNotificationManagerModule {
    public NetconfNotificationManagerModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier, org.opendaylight.controller.config.api.DependencyResolver dependencyResolver) {
        super(identifier, dependencyResolver);
    }

    public NetconfNotificationManagerModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier, org.opendaylight.controller.config.api.DependencyResolver dependencyResolver, org.opendaylight.controller.config.yang.netconf.northbound.notification.impl.NetconfNotificationManagerModule oldModule, java.lang.AutoCloseable oldInstance) {
        super(identifier, dependencyResolver, oldModule, oldInstance);
    }

    @Override
    public void customValidation() {
        // add custom validation form module attributes here.
    }

    @Override
    public java.lang.AutoCloseable createInstance() {
        return new NetconfNotificationManager();
    }

}
