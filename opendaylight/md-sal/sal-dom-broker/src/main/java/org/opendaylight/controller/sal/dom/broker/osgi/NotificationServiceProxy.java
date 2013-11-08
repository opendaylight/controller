package org.opendaylight.controller.sal.dom.broker.osgi;

import org.opendaylight.controller.sal.core.api.notify.NotificationListener;
import org.opendaylight.controller.sal.core.api.notify.NotificationService;
import org.opendaylight.yangtools.concepts.Registration;
import org.opendaylight.yangtools.yang.common.QName;
import org.osgi.framework.ServiceReference;

public class NotificationServiceProxy extends AbstractBrokerServiceProxy<NotificationService> implements
        NotificationService {

    public NotificationServiceProxy(ServiceReference<NotificationService> ref, NotificationService delegate) {
        super(ref, delegate);
    }

    @Override
    public Registration<NotificationListener> addNotificationListener(QName notification, NotificationListener listener) {
        return addRegistration(getDelegate().addNotificationListener(notification, listener));
    }
}
