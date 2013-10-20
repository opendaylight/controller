package org.opendaylight.controller.sal.binding.spi;

import java.util.Set;

import org.opendaylight.controller.sal.binding.api.NotificationListener;
import org.opendaylight.yangtools.yang.binding.Notification;

public interface NotificationInvokerFactory {

    NotificationInvoker invokerFor(org.opendaylight.yangtools.yang.binding.NotificationListener instance);

    public interface NotificationInvoker {

        Set<Class<? extends Notification>> getSupportedNotifications();

        NotificationListener<Notification> getInvocationProxy();

        public abstract void close();

        org.opendaylight.yangtools.yang.binding.NotificationListener getDelegate();

    }
}
