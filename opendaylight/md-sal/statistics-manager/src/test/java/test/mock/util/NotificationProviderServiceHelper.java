package test.mock.util;

import org.opendaylight.controller.sal.binding.codegen.impl.SingletonHolder;
import org.opendaylight.controller.sal.binding.impl.NotificationBrokerImpl;
import org.opendaylight.yangtools.yang.binding.Notification;

import java.util.Timer;
import java.util.TimerTask;

public class NotificationProviderServiceHelper {
    private NotificationBrokerImpl notifBroker = new NotificationBrokerImpl(SingletonHolder.getDefaultNotificationExecutor());

    public NotificationBrokerImpl getNotifBroker() {
        return notifBroker;
    }

    public void pushDelayedNotification(final Notification notification, int delay) {
        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                notifBroker.publish(notification);
            }
        }, delay);
    }

    public void pushNotification(final Notification notification) {
        notifBroker.publish(notification);
    }
}
