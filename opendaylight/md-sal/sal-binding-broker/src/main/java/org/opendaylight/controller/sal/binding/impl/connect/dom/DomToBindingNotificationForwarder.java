package org.opendaylight.controller.sal.binding.impl.connect.dom;

import java.lang.ref.WeakReference;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.opendaylight.controller.sal.binding.api.NotificationProviderService;
import org.opendaylight.controller.sal.core.api.notify.NotificationListener;
import org.opendaylight.controller.sal.core.api.notify.NotificationPublishService;
import org.opendaylight.yangtools.yang.binding.DataContainer;
import org.opendaylight.yangtools.yang.binding.Notification;
import org.opendaylight.yangtools.yang.binding.util.BindingReflections;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.CompositeNode;
import org.opendaylight.yangtools.yang.data.impl.codec.BindingIndependentMappingService;

class DomToBindingNotificationForwarder implements NotificationProviderService.NotificationInterestListener,
    NotificationListener {

    private final ConcurrentMap<QName, WeakReference<Class<? extends Notification>>> notifications = new ConcurrentHashMap<>();
    private final Set<QName> supportedNotifications = new HashSet<>();

    private final BindingIndependentMappingService mappingService;
    private final NotificationProviderService baNotifyService;
    private final NotificationPublishService domNotificationService;

    DomToBindingNotificationForwarder(final BindingIndependentMappingService mappingService, final NotificationProviderService baNotifyService,
        final NotificationPublishService domNotificationService) {
        this.mappingService = mappingService;
        this.baNotifyService = baNotifyService;
        this.domNotificationService = domNotificationService;
    }

    @Override
    public Set<QName> getSupportedNotifications() {
        return Collections.unmodifiableSet(supportedNotifications);
    }

    @Override
    public void onNotification(final CompositeNode notification) {
        QName qname = notification.getNodeType();
        WeakReference<Class<? extends Notification>> potential = notifications.get(qname);
        if (potential != null) {
            Class<? extends Notification> potentialClass = potential.get();
            if (potentialClass != null) {
                final DataContainer baNotification = mappingService.dataObjectFromDataDom(potentialClass,
                    notification);

                if (baNotification instanceof Notification) {
                    baNotifyService.publish((Notification) baNotification);
                }
            }
        }
    }

    @Override
    public void onNotificationSubscribtion(final Class<? extends Notification> notificationType) {
        QName qname = BindingReflections.findQName(notificationType);
        if (qname != null) {
            WeakReference<Class<? extends Notification>> already = notifications.putIfAbsent(qname,
                new WeakReference<Class<? extends Notification>>(notificationType));
            if (already == null) {
                domNotificationService.addNotificationListener(qname, this);
                supportedNotifications.add(qname);
            }
        }
    }
}
