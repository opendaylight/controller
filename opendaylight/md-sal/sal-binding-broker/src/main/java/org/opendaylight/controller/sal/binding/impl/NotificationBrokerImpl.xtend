/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.binding.impl

import com.google.common.collect.HashMultimap
import com.google.common.collect.ImmutableSet
import com.google.common.collect.Multimap
import com.google.common.collect.Multimaps
import java.util.Collections
import java.util.concurrent.Callable
import java.util.concurrent.ExecutorService
import java.util.concurrent.Future
import java.util.Set
import org.opendaylight.controller.sal.binding.api.NotificationListener
import org.opendaylight.controller.sal.binding.api.NotificationProviderService
import org.opendaylight.controller.sal.binding.api.NotificationProviderService.NotificationInterestListener
import org.opendaylight.controller.sal.binding.codegen.impl.SingletonHolder
import org.opendaylight.controller.sal.binding.spi.NotificationInvokerFactory.NotificationInvoker
import org.opendaylight.yangtools.concepts.AbstractObjectRegistration
import org.opendaylight.yangtools.concepts.ListenerRegistration
import org.opendaylight.yangtools.concepts.Registration
import org.opendaylight.yangtools.concepts.util.ListenerRegistry
import org.opendaylight.yangtools.yang.binding.Notification
import org.slf4j.LoggerFactory

class NotificationBrokerImpl implements NotificationProviderService, AutoCloseable {
    
    val ListenerRegistry<NotificationInterestListener> interestListeners = ListenerRegistry.create;
    
    val Multimap<Class<? extends Notification>, NotificationListener<?>> listeners;

    @Property
    var ExecutorService executor;
    
    val logger = LoggerFactory.getLogger(NotificationBrokerImpl)

    new() {
        listeners = Multimaps.synchronizedSetMultimap(HashMultimap.create())
    }

    @Deprecated
    new(ExecutorService executor) {
        listeners = Multimaps.synchronizedSetMultimap(HashMultimap.create())
        this.executor = executor;
    }

    @Deprecated
    override <T extends Notification> addNotificationListener(Class<T> notificationType,
        NotificationListener<T> listener) {
        listeners.put(notificationType, listener)
    }

    @Deprecated
    override <T extends Notification> removeNotificationListener(Class<T> notificationType,
        NotificationListener<T> listener) {
        listeners.remove(notificationType, listener)
    }

    override notify(Notification notification) {
        publish(notification)
    }

    def getNotificationTypes(Notification notification) {
        notification.class.interfaces.filter[it != Notification && Notification.isAssignableFrom(it)]
    }

    @Deprecated
    override addNotificationListener(org.opendaylight.yangtools.yang.binding.NotificationListener listener) {
        throw new UnsupportedOperationException("Deprecated method. Use registerNotificationListener instead.");

    }

    @Deprecated
    override removeNotificationListener(org.opendaylight.yangtools.yang.binding.NotificationListener listener) {
        throw new UnsupportedOperationException(
            "Deprecated method. Use RegisterNotificationListener returned value to close registration.")
    }

    @Deprecated
    override notify(Notification notification, ExecutorService service) {
        publish(notification, service)
    }

    override publish(Notification notification) {
        publish(notification, executor)
    }

    override publish(Notification notification, ExecutorService service) {
        val allTypes = notification.notificationTypes

        var Iterable<NotificationListener<?>> listenerToNotify = Collections.emptySet();
        for (type : allTypes) {
            listenerToNotify = listenerToNotify + listeners.get(type as Class<? extends Notification>)
        }
        val tasks = listenerToNotify.map[new NotifyTask(it, notification)].toSet;
        submitAll(executor,tasks);
    }
    
    def submitAll(ExecutorService service, Set<NotifyTask> tasks) {
        val ret = ImmutableSet.<Future<Object>>builder();
        for(task : tasks) {
            ret.add(service.submit(task));
        }
        return ret.build();
    }
    
    override <T extends Notification> registerNotificationListener(Class<T> notificationType,
        NotificationListener<T> listener) {
        val reg = new GenericNotificationRegistration<T>(notificationType, listener, this);
        listeners.put(notificationType, listener);
        announceNotificationSubscription(notificationType);
        return reg;
    }
    
    def announceNotificationSubscription(Class<? extends Notification> notification) {
        for (listener : interestListeners) {
            try {
                listener.instance.onNotificationSubscribtion(notification);
            } catch (Exception e) {
                logger.error("", e.message)
            }
        }
    }

    override registerNotificationListener(
        org.opendaylight.yangtools.yang.binding.NotificationListener listener) {
        val invoker = SingletonHolder.INVOKER_FACTORY.invokerFor(listener);
        for (notifyType : invoker.supportedNotifications) {
            listeners.put(notifyType, invoker.invocationProxy)
            announceNotificationSubscription(notifyType)
        }
        val registration = new GeneratedListenerRegistration(listener, invoker,this);
        return registration as Registration<org.opendaylight.yangtools.yang.binding.NotificationListener>;
    }

    protected def unregisterListener(GenericNotificationRegistration<?> reg) {
        listeners.remove(reg.type, reg.instance);
    }

    protected def unregisterListener(GeneratedListenerRegistration reg) {
        for (notifyType : reg.invoker.supportedNotifications) {
            listeners.remove(notifyType, reg.invoker.invocationProxy)
        }
    }
    
    override close()  {
        //FIXME: implement properly.
    }
    
    override registerInterestListener(NotificationInterestListener interestListener) {
        val registration = interestListeners.register(interestListener);
        
        for(notification : listeners.keySet) {
            interestListener.onNotificationSubscribtion(notification);
        }
        return registration
    }
}

class GenericNotificationRegistration<T extends Notification> extends AbstractObjectRegistration<NotificationListener<T>> implements ListenerRegistration<NotificationListener<T>> {

    @Property
    val Class<T> type;

    var NotificationBrokerImpl notificationBroker;

    public new(Class<T> type, NotificationListener<T> instance, NotificationBrokerImpl broker) {
        super(instance);
        _type = type;
        notificationBroker = broker;
    }

    override protected removeRegistration() {
        notificationBroker.unregisterListener(this);
        notificationBroker = null;
    }
}

class GeneratedListenerRegistration extends AbstractObjectRegistration<org.opendaylight.yangtools.yang.binding.NotificationListener> implements ListenerRegistration<org.opendaylight.yangtools.yang.binding.NotificationListener> {

    @Property
    val NotificationInvoker invoker;
    
    var NotificationBrokerImpl notificationBroker;
    

    new(org.opendaylight.yangtools.yang.binding.NotificationListener instance, NotificationInvoker invoker, NotificationBrokerImpl broker) {
        super(instance);
        _invoker = invoker;
        notificationBroker = broker;
    }

    override protected removeRegistration() {
        notificationBroker.unregisterListener(this);
        notificationBroker = null;
        invoker.close();
    }
}

@Data
class NotifyTask implements Callable<Object> {

    private static val log = LoggerFactory.getLogger(NotifyTask);

    @SuppressWarnings("rawtypes")
    val NotificationListener listener;
    val Notification notification;

    override call() {
        //Only logging the complete notification in debug mode
        try {
            if(log.isDebugEnabled){
                log.debug("Delivering notification {} to {}",notification,listener);
            } else {
                log.trace("Delivering notification {} to {}",notification.class.name,listener);
            }
            listener.onNotification(notification);
            if(log.isDebugEnabled){
                log.debug("Notification delivered {} to {}",notification,listener);
            } else {
                log.trace("Notification delivered {} to {}",notification.class.name,listener);
            }
        } catch (Exception e) {
            log.error("Unhandled exception thrown by listener: {}", listener, e);
        }
        return null;
    }

}
