/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.binding.impl

import org.opendaylight.controller.sal.binding.api.NotificationProviderService
import org.opendaylight.yangtools.yang.binding.Notification
import com.google.common.collect.Multimap
import org.opendaylight.controller.sal.binding.api.NotificationListener
import com.google.common.collect.HashMultimap
import java.util.concurrent.ExecutorService
import java.util.Collection
import org.opendaylight.yangtools.concepts.Registration
import org.opendaylight.controller.sal.binding.codegen.RuntimeCodeGenerator
import org.opendaylight.controller.sal.binding.spi.NotificationInvokerFactory
import org.opendaylight.yangtools.concepts.ListenerRegistration
import org.opendaylight.controller.sal.binding.spi.NotificationInvokerFactory.NotificationInvoker
import org.opendaylight.yangtools.concepts.AbstractObjectRegistration
import java.util.Collections
import org.slf4j.LoggerFactory
import java.util.concurrent.Callable

class NotificationBrokerImpl implements NotificationProviderService, AutoCloseable {

    val Multimap<Class<? extends Notification>, NotificationListener<?>> listeners;

    @Property
    var ExecutorService executor;

    new(ExecutorService executor) {
        listeners = HashMultimap.create()
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

    @SuppressWarnings("unchecked")
    private def notifyAll(Collection<NotificationListener<?>> listeners, Notification notification) {
        listeners.forEach[(it as NotificationListener).onNotification(notification)]
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
        executor.invokeAll(tasks);
    }

    override <T extends Notification> registerNotificationListener(Class<T> notificationType,
        NotificationListener<T> listener) {
        val reg = new GenericNotificationRegistration<T>(notificationType, listener, this);
        listeners.put(notificationType, listener);
        return reg;
    }

    override registerNotificationListener(
        org.opendaylight.yangtools.yang.binding.NotificationListener listener) {
        val invoker = BindingAwareBrokerImpl.generator.invokerFactory.invokerFor(listener);
        for (notifyType : invoker.supportedNotifications) {
            listeners.put(notifyType, invoker.invocationProxy)
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

    val NotificationListener listener;
    val Notification notification;

    override call() {
        try {
            log.info("Delivering notification {} to {}",notification,listener);
            listener.onNotification(notification);
            log.info("Notification delivered {} to {}",notification,listener);
        } catch (Exception e) {
            log.error("Unhandled exception thrown by listener: {}", listener, e);
        }
        return null;
    }

}
