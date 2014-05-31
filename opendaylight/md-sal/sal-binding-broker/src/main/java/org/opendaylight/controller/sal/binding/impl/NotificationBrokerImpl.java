/**
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.binding.impl;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import org.eclipse.xtext.xbase.lib.Conversions;
import org.eclipse.xtext.xbase.lib.Exceptions;
import org.eclipse.xtext.xbase.lib.Functions.Function0;
import org.eclipse.xtext.xbase.lib.Functions.Function1;
import org.eclipse.xtext.xbase.lib.IterableExtensions;
import org.opendaylight.controller.sal.binding.api.NotificationListener;
import org.opendaylight.controller.sal.binding.api.NotificationProviderService;
import org.opendaylight.controller.sal.binding.codegen.impl.SingletonHolder;
import org.opendaylight.controller.sal.binding.spi.NotificationInvokerFactory.NotificationInvoker;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.concepts.Registration;
import org.opendaylight.yangtools.concepts.util.ListenerRegistry;
import org.opendaylight.yangtools.yang.binding.Notification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Objects;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSet.Builder;
import com.google.common.collect.Iterables;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import com.google.common.collect.SetMultimap;

public class NotificationBrokerImpl implements NotificationProviderService, AutoCloseable {
    private final ListenerRegistry<NotificationInterestListener> interestListeners = new Function0<ListenerRegistry<NotificationInterestListener>>() {
        @Override
        public ListenerRegistry<NotificationInterestListener> apply() {
            ListenerRegistry<NotificationInterestListener> _create = ListenerRegistry.<NotificationInterestListener>create();
            return _create;
        }
    }.apply();

    private final Multimap<Class<? extends Notification>,NotificationListener<? extends Object>> listeners;

    private ExecutorService _executor;

    public ExecutorService getExecutor() {
        return this._executor;
    }

    public void setExecutor(final ExecutorService executor) {
        this._executor = executor;
    }

    private final Logger logger = new Function0<Logger>() {
        @Override
        public Logger apply() {
            Logger _logger = LoggerFactory.getLogger(NotificationBrokerImpl.class);
            return _logger;
        }
    }.apply();

    public NotificationBrokerImpl() {
        HashMultimap<Class<? extends Notification>,NotificationListener<? extends Object>> _create = HashMultimap.<Class<? extends Notification>, NotificationListener<? extends Object>>create();
        SetMultimap<Class<? extends Notification>,NotificationListener<? extends Object>> _synchronizedSetMultimap = Multimaps.<Class<? extends Notification>, NotificationListener<? extends Object>>synchronizedSetMultimap(_create);
        this.listeners = _synchronizedSetMultimap;
    }

    @Deprecated
    public NotificationBrokerImpl(final ExecutorService executor) {
        HashMultimap<Class<? extends Notification>,NotificationListener<? extends Object>> _create = HashMultimap.<Class<? extends Notification>, NotificationListener<? extends Object>>create();
        SetMultimap<Class<? extends Notification>,NotificationListener<? extends Object>> _synchronizedSetMultimap = Multimaps.<Class<? extends Notification>, NotificationListener<? extends Object>>synchronizedSetMultimap(_create);
        this.listeners = _synchronizedSetMultimap;
        this.setExecutor(executor);
    }

    public Iterable<Class<? extends Object>> getNotificationTypes(final Notification notification) {
        Class<? extends Notification> _class = notification.getClass();
        Class<? extends Object>[] _interfaces = _class.getInterfaces();
        final Function1<Class<? extends Object>,Boolean> _function = new Function1<Class<? extends Object>,Boolean>() {
            @Override
            public Boolean apply(final Class<? extends Object> it) {
                boolean _and = false;
                boolean _notEquals = (!Objects.equal(it, Notification.class));
                if (!_notEquals) {
                    _and = false;
                } else {
                    boolean _isAssignableFrom = Notification.class.isAssignableFrom(it);
                    _and = (_notEquals && _isAssignableFrom);
                }
                return Boolean.valueOf(_and);
            }
        };
        Iterable<Class<? extends Object>> _filter = IterableExtensions.<Class<? extends Object>>filter(((Iterable<Class<? extends Object>>)Conversions.doWrapArray(_interfaces)), _function);
        return _filter;
    }

    @Override
    public void publish(final Notification notification) {
        ExecutorService _executor = this.getExecutor();
        this.publish(notification, _executor);
    }

    @Override
    public void publish(final Notification notification, final ExecutorService service) {
        final Iterable<Class<? extends Object>> allTypes = this.getNotificationTypes(notification);
        Iterable<NotificationListener<? extends Object>> listenerToNotify = Collections.<NotificationListener<? extends Object>>emptySet();
        for (final Class<? extends Object> type : allTypes) {
            Collection<NotificationListener<? extends Object>> _get = this.listeners.get(((Class<? extends Notification>) type));
            Iterable<NotificationListener<? extends Object>> _plus = Iterables.<NotificationListener<? extends Object>>concat(listenerToNotify, _get);
            listenerToNotify = _plus;
        }
        final Function1<NotificationListener<? extends Object>,NotifyTask> _function = new Function1<NotificationListener<? extends Object>,NotifyTask>() {
            @Override
            public NotifyTask apply(final NotificationListener<? extends Object> it) {
                NotifyTask _notifyTask = new NotifyTask(it, notification);
                return _notifyTask;
            }
        };
        Iterable<NotifyTask> _map = IterableExtensions.<NotificationListener<? extends Object>, NotifyTask>map(listenerToNotify, _function);
        final Set<NotifyTask> tasks = IterableExtensions.<NotifyTask>toSet(_map);
        ExecutorService _executor = this.getExecutor();
        this.submitAll(_executor, tasks);
    }

    public ImmutableSet<Future<Object>> submitAll(final ExecutorService service, final Set<NotifyTask> tasks) {
        final Builder<Future<Object>> ret = ImmutableSet.<Future<Object>>builder();
        for (final NotifyTask task : tasks) {
            Future<Object> _submit = service.<Object>submit(task);
            ret.add(_submit);
        }
        return ret.build();
    }

    @Override
    public <T extends Notification> Registration<NotificationListener<T>> registerNotificationListener(final Class<T> notificationType, final NotificationListener<T> listener) {
        GenericNotificationRegistration<T> _genericNotificationRegistration = new GenericNotificationRegistration<T>(notificationType, listener, this);
        final GenericNotificationRegistration<T> reg = _genericNotificationRegistration;
        this.listeners.put(notificationType, listener);
        this.announceNotificationSubscription(notificationType);
        return reg;
    }

    public void announceNotificationSubscription(final Class<? extends Notification> notification) {
        for (final ListenerRegistration<NotificationInterestListener> listener : this.interestListeners) {
            try {
                NotificationInterestListener _instance = listener.getInstance();
                _instance.onNotificationSubscribtion(notification);
            } catch (final Throwable _t) {
                if (_t instanceof Exception) {
                    final Exception e = (Exception)_t;
                    String _message = e.getMessage();
                    this.logger.error("", _message);
                } else {
                    throw Exceptions.sneakyThrow(_t);
                }
            }
        }
    }

    @Override
    public Registration<org.opendaylight.yangtools.yang.binding.NotificationListener> registerNotificationListener(final org.opendaylight.yangtools.yang.binding.NotificationListener listener) {
        final NotificationInvoker invoker = SingletonHolder.INVOKER_FACTORY.invokerFor(listener);
        Set<Class<? extends Notification>> _supportedNotifications = invoker.getSupportedNotifications();
        for (final Class<? extends Notification> notifyType : _supportedNotifications) {
            {
                NotificationListener<Notification> _invocationProxy = invoker.getInvocationProxy();
                this.listeners.put(notifyType, _invocationProxy);
                this.announceNotificationSubscription(notifyType);
            }
        }
        GeneratedListenerRegistration _generatedListenerRegistration = new GeneratedListenerRegistration(listener, invoker, this);
        final GeneratedListenerRegistration registration = _generatedListenerRegistration;
        return (registration);
    }

    protected boolean unregisterListener(final GenericNotificationRegistration<? extends Object> reg) {
        Class<? extends Notification> _type = reg.getType();
        NotificationListener<? extends Notification> _instance = reg.getInstance();
        boolean _remove = this.listeners.remove(_type, _instance);
        return _remove;
    }

    protected void unregisterListener(final GeneratedListenerRegistration reg) {
        NotificationInvoker _invoker = reg.getInvoker();
        Set<Class<? extends Notification>> _supportedNotifications = _invoker.getSupportedNotifications();
        for (final Class<? extends Notification> notifyType : _supportedNotifications) {
            NotificationInvoker _invoker_1 = reg.getInvoker();
            NotificationListener<Notification> _invocationProxy = _invoker_1.getInvocationProxy();
            this.listeners.remove(notifyType, _invocationProxy);
        }
    }

    @Override
    public void close() {
    }

    @Override
    public ListenerRegistration<NotificationInterestListener> registerInterestListener(final NotificationInterestListener interestListener) {
        final ListenerRegistration<NotificationInterestListener> registration = this.interestListeners.register(interestListener);
        Set<Class<? extends Notification>> _keySet = this.listeners.keySet();
        for (final Class<? extends Notification> notification : _keySet) {
            interestListener.onNotificationSubscribtion(notification);
        }
        return registration;
    }
}
