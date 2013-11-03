package org.opendaylight.controller.sal.dom.broker.spi;

import org.opendaylight.controller.sal.core.api.notify.NotificationListener;
import org.opendaylight.yangtools.concepts.Registration;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.CompositeNode;

public interface NotificationRouter {

    void publish(CompositeNode notification);

    /**
     * Registers a notification listener for supplied notification type.
     * 
     * @param notification
     * @param listener
     */
    Registration<NotificationListener> addNotificationListener(QName notification,
            NotificationListener listener);

}
