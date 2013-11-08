package org.opendaylight.controller.sal.dom.broker.osgi;

import org.opendaylight.controller.sal.core.api.notify.NotificationListener;
import org.opendaylight.controller.sal.core.api.notify.NotificationPublishService;
import org.opendaylight.yangtools.concepts.Registration;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.CompositeNode;
import org.osgi.framework.ServiceReference;

public class NotificationPublishServiceProxy extends AbstractBrokerServiceProxy<NotificationPublishService> implements NotificationPublishService {

    public NotificationPublishServiceProxy(ServiceReference<NotificationPublishService> ref,
            NotificationPublishService delegate) {
        super(ref, delegate);
    }

    public void sendNotification(CompositeNode notification) {
        getDelegate().sendNotification(notification);
    }

    public Registration<NotificationListener> addNotificationListener(QName notification, NotificationListener listener) {
        return addRegistration(getDelegate().addNotificationListener(notification, listener));

    }

    public void publish(CompositeNode notification) {
        getDelegate().publish(notification);
    }
}
