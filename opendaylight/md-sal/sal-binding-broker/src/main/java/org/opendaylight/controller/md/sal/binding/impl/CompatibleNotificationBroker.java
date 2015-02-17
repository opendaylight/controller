package org.opendaylight.controller.md.sal.binding.impl;

import java.util.concurrent.ExecutorService;
import org.opendaylight.controller.md.sal.binding.api.NotificationPublishService;
import org.opendaylight.controller.md.sal.binding.api.NotificationService;
import org.opendaylight.controller.sal.binding.api.NotificationListener;
import org.opendaylight.controller.sal.binding.api.NotificationProviderService;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.Notification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CompatibleNotificationBroker implements NotificationProviderService, AutoCloseable {
    private static final Logger LOG = LoggerFactory.getLogger(CompatibleNotificationBroker.class);

    private final NotificationPublishService notificationPublishService;
    private final NotificationService notificationService;

    public CompatibleNotificationBroker(NotificationPublishService notificationPublishService, NotificationService notificationService) {
        this.notificationPublishService = notificationPublishService;
        this.notificationService = notificationService;
    }

    @Override
    public void publish(Notification notification) {
        try {
            notificationPublishService.putNotification(notification);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void publish(Notification notification, ExecutorService executor) {
        try {
            notificationPublishService.putNotification(notification);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Override
    public ListenerRegistration<NotificationInterestListener> registerInterestListener(NotificationInterestListener interestListener) {
        return null;
    }

    @Override
    public <T extends Notification> ListenerRegistration<NotificationListener<T>> registerNotificationListener(Class<T> notificationType, NotificationListener<T> listener) {
        return null;
    }

    @Override
    public ListenerRegistration<org.opendaylight.yangtools.yang.binding.NotificationListener> registerNotificationListener(org.opendaylight.yangtools.yang.binding.NotificationListener listener) {
        return notificationService.registerNotificationListener(listener);
    }

    @Override
    public void close() throws Exception {

    }
}
