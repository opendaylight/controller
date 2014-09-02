package test.mock.util;

import org.opendaylight.controller.sal.binding.codegen.impl.SingletonHolder;
import org.opendaylight.controller.sal.binding.impl.NotificationBrokerImpl;
import org.opendaylight.yangtools.yang.binding.Notification;

import java.util.Timer;
import java.util.TimerTask;

public class NotificationProviderServiceMock {
    private NotificationBrokerImpl notifBroker = new NotificationBrokerImpl(SingletonHolder.getDefaultNotificationExecutor());

    public NotificationBrokerImpl getNotifBroker() {
        return notifBroker;
    }

    public void pushDelayedNotif(final Notification notification) {
        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                notifBroker.publish(notification);
            }
        }, 100);
    }
}
