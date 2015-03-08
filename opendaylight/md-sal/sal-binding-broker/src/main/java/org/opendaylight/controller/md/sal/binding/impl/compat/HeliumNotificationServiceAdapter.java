package org.opendaylight.controller.md.sal.binding.impl.compat;

import org.opendaylight.controller.md.sal.binding.api.NotificationService;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.Notification;
import org.opendaylight.yangtools.yang.binding.NotificationListener;

public class HeliumNotificationServiceAdapter implements org.opendaylight.controller.sal.binding.api.NotificationService, AutoCloseable {

    private final NotificationService notificationService;

    public HeliumNotificationServiceAdapter(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @Override
    public <T extends Notification> ListenerRegistration<org.opendaylight.controller.sal.binding.api.NotificationListener<T>> registerNotificationListener(
            final Class<T> notificationType, final org.opendaylight.controller.sal.binding.api.NotificationListener<T> listener) {
        throw new UnsupportedOperationException("Not supported type of listener.");
    }

    @Override
    public ListenerRegistration<NotificationListener> registerNotificationListener(
            final NotificationListener listener) {
        return notificationService.registerNotificationListener(listener);
    }

    @Override
    public void close() throws Exception {

    }
}
