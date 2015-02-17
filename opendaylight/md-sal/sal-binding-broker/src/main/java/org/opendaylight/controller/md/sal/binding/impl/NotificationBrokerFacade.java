package org.opendaylight.controller.md.sal.binding.impl;

import com.google.common.collect.ImmutableSet;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicReference;
import javax.annotation.Nonnull;
import org.opendaylight.controller.md.sal.binding.api.NotificationPublishService;
import org.opendaylight.controller.md.sal.binding.api.NotificationService;
import org.opendaylight.controller.md.sal.dom.api.DOMNotification;
import org.opendaylight.controller.md.sal.dom.api.DOMNotificationListener;
import org.opendaylight.controller.md.sal.dom.api.DOMNotificationService;
import org.opendaylight.controller.sal.binding.api.NotificationProviderService;
import org.opendaylight.controller.sal.binding.codegen.impl.SingletonHolder;
import org.opendaylight.controller.sal.binding.spi.NotificationInvokerFactory;
import org.opendaylight.yangtools.binding.data.codec.api.BindingNormalizedNodeSerializer;
import org.opendaylight.yangtools.concepts.AbstractListenerRegistration;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.util.ListenerRegistry;
import org.opendaylight.yangtools.yang.binding.Notification;
import org.opendaylight.yangtools.yang.binding.util.BindingReflections;
import org.opendaylight.yangtools.yang.model.api.SchemaPath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NotificationBrokerFacade implements NotificationProviderService, AutoCloseable {
    private static final Logger LOG = LoggerFactory.getLogger(NotificationBrokerFacade.class);

    private final NotificationInvokerFactory invokerFactory;
    private final NotificationPublishService notificationPublishService;
    private final NotificationService notificationService;
    private final DOMNotificationService domNotifService;
    private final BindingNormalizedNodeSerializer codec;
    private final ListenerRegistry<NotificationInterestListener> interestListeners =
            ListenerRegistry.create();
    private AtomicReference<ListenersMap> knownTypes = new AtomicReference<>(new ListenersMap());

    public NotificationBrokerFacade(NotificationPublishService notificationPublishService,
                                    NotificationService notificationService,
                                    DOMNotificationService domNotifService,
                                    BindingNormalizedNodeSerializer codecRegistry) {
        this.notificationPublishService = notificationPublishService;
        this.notificationService = notificationService;
        this.invokerFactory = SingletonHolder.INVOKER_FACTORY;
        this.domNotifService = domNotifService;
        this.codec = codecRegistry;
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

    private void announceNotificationSubscription(final Class<? extends Notification> notification) {
        for (final ListenerRegistration<NotificationInterestListener> listener : interestListeners) {
            try {
                listener.getInstance().onNotificationSubscribtion(notification);
            } catch (Exception e) {
                LOG.warn("Listener {} reported unexpected error on notification {}",
                        listener.getInstance(), notification, e);
            }
        }
    }

    @Override
    public ListenerRegistration<NotificationInterestListener> registerInterestListener(NotificationInterestListener interestListener) {
        final ListenerRegistration<NotificationInterestListener> registration = this.interestListeners.register(interestListener);

        for (final Class<? extends Notification> notification : knownTypes.get().getKnownTypes()) {
            interestListener.onNotificationSubscribtion(notification);
        }
        return registration;
    }

    @Override
    public <T extends Notification> ListenerRegistration<org.opendaylight.controller.sal.binding.api.NotificationListener<T>> registerNotificationListener(Class<T> notificationType, org.opendaylight.controller.sal.binding.api.NotificationListener<T> listener) {
        final SchemaPath schemaPath = SchemaPath.create(true, BindingReflections.findQName(notificationType));
        final NotificationInvoker<T> notificationListener = new NotificationInvoker<>(listener);
        final ListenerRegistration<NotificationInvoker<T>> domListener = domNotifService.registerNotificationListener(notificationListener, schemaPath);
        announceNotificationSubscription(notificationType);
        return new ListenerRegistrationImpl<>(listener, domListener);
    }

    @Override
    public ListenerRegistration<org.opendaylight.yangtools.yang.binding.NotificationListener> registerNotificationListener(org.opendaylight.yangtools.yang.binding.NotificationListener listener) {
        final NotificationInvokerFactory.NotificationInvoker invoker = invokerFactory.invokerFor(listener);
        final Set<Class<? extends Notification>> types = invoker.getSupportedNotifications();
        synchronized (this) {
            final Set<Class<? extends Notification>> knownTypesMutable = new HashSet<>(knownTypes.get().getKnownTypes());
            for (Class<? extends Notification> type : types) {
                knownTypesMutable.add(type);
            }
            knownTypes.set(new ListenersMap(knownTypesMutable));
        }

        for (Class<? extends Notification> type : types) {
            announceNotificationSubscription(type);
        }

        return notificationService.registerNotificationListener(listener);
    }

    @Override
    public void close() throws Exception {

    }

    private static class ListenersMap {
        private final Set<Class<? extends Notification>> knownTypes;

        public ListenersMap() {
            this.knownTypes = ImmutableSet.of();
        }

        public ListenersMap(Set<Class<? extends Notification>> knownTypes) {
            this.knownTypes = ImmutableSet.copyOf(knownTypes);
        }

        public Set<Class<? extends Notification>> getKnownTypes() {
            return knownTypes;
        }
    }

    private class ListenerRegistrationImpl<T extends org.opendaylight.controller.sal.binding.api.NotificationListener<? extends Notification>>
            extends AbstractListenerRegistration<T> {
        private final ListenerRegistration<?> listenerRegistration;

        public ListenerRegistrationImpl(T listener, ListenerRegistration<?> listenerRegistration) {
            super(listener);
            this.listenerRegistration = listenerRegistration;
        }

        @Override
        protected void removeRegistration() {
            listenerRegistration.close();
        }
    }

    private class NotificationInvoker<T extends Notification> implements DOMNotificationListener {
        private final org.opendaylight.controller.sal.binding.api.NotificationListener<T> listener;

        public NotificationInvoker(org.opendaylight.controller.sal.binding.api.NotificationListener<T> listener) {
            this.listener = listener;
        }

        @SuppressWarnings("unchecked")
        @Override
        public void onNotification(@Nonnull DOMNotification notification) {
            final Notification baNotification =  codec.fromNormalizedNodeNotification(notification.getType(), notification.getBody());
            listener.onNotification((T) baNotification);

        }
    }
}
