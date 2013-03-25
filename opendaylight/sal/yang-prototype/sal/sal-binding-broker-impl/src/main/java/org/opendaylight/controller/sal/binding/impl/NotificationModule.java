package org.opendaylight.controller.sal.binding.impl;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.opendaylight.controller.sal.binding.api.BindingAwareBroker;
import org.opendaylight.controller.sal.binding.api.BindingAwareService;
import org.opendaylight.controller.sal.binding.api.NotificationProviderService;
import org.opendaylight.controller.sal.binding.api.NotificationService;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker.ConsumerSession;
import org.opendaylight.controller.sal.binding.spi.SALBindingModule;
import org.opendaylight.controller.sal.binding.spi.Mapper;
import org.opendaylight.controller.sal.binding.spi.MappingProvider;
import org.opendaylight.controller.sal.binding.spi.MappingProvider.MappingExtensionFactory;
import org.opendaylight.controller.sal.core.api.Provider;
import org.opendaylight.controller.sal.core.api.Broker.ProviderSession;
import org.opendaylight.controller.yang.binding.DataObject;
import org.opendaylight.controller.yang.binding.Notification;
import org.opendaylight.controller.yang.binding.NotificationListener;
import org.opendaylight.controller.yang.common.QName;
import org.opendaylight.controller.yang.data.api.CompositeNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NotificationModule implements SALBindingModule {

    private ProviderSession biSession;
    private org.opendaylight.controller.sal.core.api.notify.NotificationProviderService biNotifyService;
    private BindingAwareBroker broker;
    private MappingProvider mappingProvider;

    private Map<Class<? extends Notification>, List<NotificationListener>> listeners = new HashMap<Class<? extends Notification>, List<NotificationListener>>();
    private static Logger log = LoggerFactory.getLogger(NotificationModule.class);

    @Override
    public Set<Class<? extends BindingAwareService>> getProvidedServices() {

        Set<Class<? extends BindingAwareService>> ret = new HashSet<Class<? extends BindingAwareService>>();
        ret.add(NotificationService.class);
        ret.add(NotificationProviderService.class);
        return ret;
    }

    @Override
    public <T extends BindingAwareService> T getServiceForSession(
            Class<T> service, ConsumerSession session) {
        if (service == null)
            throw new IllegalArgumentException("Service should not be null");
        if (session == null)
            throw new IllegalArgumentException("Session should not be null");

        if (NotificationProviderSession.class.equals(service)) {
            if (session instanceof org.opendaylight.controller.sal.binding.api.BindingAwareBroker.ProviderSession) {
                @SuppressWarnings("unchecked")
                T ret = (T) new NotificationProviderSession(session);
                return ret;
            } else {
                throw new IllegalArgumentException(
                        "NotificationProviderService is available only to ProviderSession");
            }
        }

        if (NotificationService.class.equals(service)) {
            @SuppressWarnings("unchecked")
            T ret = (T) new NotificationSession(session);
            return ret;
        }
        return null;
    }

    @Override
    public Set<Class<? extends org.opendaylight.controller.sal.binding.api.BindingAwareProvider.ProviderFunctionality>> getSupportedProviderFunctionality() {
        return Collections.emptySet();
    }

    @Override
    public void setBroker(BindingAwareBroker broker) {
        this.broker = broker;
    }

    @Override
    public void setMappingProvider(MappingProvider provider) {
        this.mappingProvider = provider;
    }

    @Override
    public void onBISessionAvailable(ProviderSession session) {
        biSession = session;
        if (biSession != null) {
            biNotifyService = session
                    .getService(org.opendaylight.controller.sal.core.api.notify.NotificationProviderService.class);
        }
    }

    private void notify(Notification notification) {
        notifyBindingIndependent(notification);
        notifyBindingAware(notification);
    }

    private void notifyBindingAware(Notification notification) {
        Class<? extends Notification> type = notification.getClass();
        List<NotificationListener> toNotify = listeners.get(type);

        // Invocation of notification on registered listeners
        if (toNotify != null) {

            // We get factory for Notification Invoker
            MappingExtensionFactory<NotificationInvoker> invokerFactory = mappingProvider
                    .getExtensionFactory(NotificationInvoker.class);

            // We get generated invoker for NoficiationListener interface
            // associated to Notification Type
            NotificationInvoker invoker = invokerFactory.forClass(type);
            for (NotificationListener listener : toNotify) {
                try {
                    // Invoker invokes the right method on subtype of
                    // NotificationListener
                    // associated to the type of notification
                    invoker.notify(notification, listener);
                } catch (Exception e) {

                }
            }
        }
    }

    private void notifyBindingIndependent(Notification notification) {
        Class<? extends Notification> type = notification.getClass();

        if (biSession == null) {
            return;
        }
        if (biSession.isClosed()) {
            return;
        }
        if (biNotifyService == null) {
            return;
        }

        // FIXME: Somehow we need to resolve this for class hierarchy.
        // probably use type.getInterfaces()
        Mapper<? extends Notification> mapper = mappingProvider.getMapper(type);
        CompositeNode domNotification = mapper.domFromObject(notification);

        biNotifyService.sendNotification(domNotification);
    }

    private class NotificationSession implements NotificationService {
        private final ConsumerSession session;

        public NotificationSession(ConsumerSession session) {
            this.session = session;
        }

        private Map<Class<? extends Notification>, List<NotificationListener>> sessionListeners = new HashMap<Class<? extends Notification>, List<NotificationListener>>();

        @Override
        public void addNotificationListener(
                Class<? extends Notification> notificationType,
                NotificationListener listener) {
            // TODO Implement this method
            throw new UnsupportedOperationException("Not implemented");
        }

        @Override
        public void removeNotificationListener(
                Class<? extends Notification> notificationType,
                NotificationListener listener) {
            // TODO Implement this method
            throw new UnsupportedOperationException("Not implemented");
        }

    }

    private class NotificationProviderSession extends NotificationSession
            implements NotificationProviderService {

        public NotificationProviderSession(ConsumerSession session) {
            super(session);
        }

        @Override
        public void notify(Notification notification) {
            NotificationModule.this.notify(notification);
        }

    }
    
    private class BindingIndependentListener implements org.opendaylight.controller.sal.core.api.notify.NotificationListener {

        @Override
        public Set<QName> getSupportedNotifications() {
            return Collections.emptySet();
        }

        @Override
        public void onNotification(CompositeNode notification) {
            NotificationModule.this.onBindingIndependentNotification(notification);
        }
        
    }

    private void onBindingIndependentNotification(CompositeNode biNotification) {
        QName biType = biNotification.getNodeType();
        
        Mapper<DataObject> mapper = mappingProvider.getMapper(biType);
        if(mapper == null) {
            log.info("Received notification does not have a binding defined.");
            return;
        }
        Class<DataObject> type = mapper.getDataObjectClass();
        
        // We check if the received QName / type is really Notification
        if(Notification.class.isAssignableFrom(type)) {
            Notification notification = (Notification) mapper.objectFromDom(biNotification);
            notifyBindingAware(notification);
        } else {
            // The generated type for this QName does not inherits from notification
            // something went wrong - generated APIs and/or provider sending notification
            // which was incorectly described in the YANG schema.
            log.error("Received notification "+  biType +" is not binded as notification");
        }
        
    }
}
