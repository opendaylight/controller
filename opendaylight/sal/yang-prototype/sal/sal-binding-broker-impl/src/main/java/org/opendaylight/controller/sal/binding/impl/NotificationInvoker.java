package org.opendaylight.controller.sal.binding.impl;

import org.opendaylight.controller.sal.binding.spi.MappingProvider.MappingExtension;
import org.opendaylight.controller.yang.binding.Notification;
import org.opendaylight.controller.yang.binding.NotificationListener;

public interface NotificationInvoker extends MappingExtension {
    void notify(Notification notification, NotificationListener listener);
}