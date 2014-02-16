/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.restconf.broker.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;

import org.opendaylight.controller.sal.binding.api.NotificationListener;
import org.opendaylight.controller.sal.binding.api.NotificationService;
import org.opendaylight.controller.sal.restconf.broker.listeners.RemoteNotificationListener;
import org.opendaylight.controller.sal.restconf.broker.tools.RemoteStreamTools;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.remote.rev140114.QName;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.remote.rev140114.SalRemoteService;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.concepts.Registration;
import org.opendaylight.yangtools.restconf.client.api.RestconfClientContext;
import org.opendaylight.yangtools.restconf.client.api.event.EventStreamInfo;
import org.opendaylight.yangtools.yang.binding.Notification;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import com.google.common.collect.SetMultimap;

public class NotificationServiceImpl implements NotificationService {
    private final SalRemoteService salRemoteService;
    private final RestconfClientContext restconfClientContext;

    private final Multimap<Class<? extends Notification>,NotificationListener<? extends Object>> listeners;
    private ExecutorService _executor;

    public NotificationServiceImpl(RestconfClientContext restconfClienetContext){
        this.restconfClientContext = restconfClienetContext;
        this.salRemoteService = this.restconfClientContext.getRpcServiceContext(SalRemoteService.class).getRpcService();

        HashMultimap<Class<? extends Notification>,NotificationListener<? extends Object>> _create = HashMultimap.<Class<? extends Notification>, NotificationListener<? extends Object>>create();
        SetMultimap<Class<? extends Notification>,NotificationListener<? extends Object>> _synchronizedSetMultimap = Multimaps.<Class<? extends Notification>, NotificationListener<? extends Object>>synchronizedSetMultimap(_create);
        this.listeners = _synchronizedSetMultimap;

    }
    public ExecutorService getExecutor() {
        return this._executor;
    }

    public void setExecutor(final ExecutorService executor) {
        this._executor = executor;
    }

    @Override
    public <T extends Notification> void addNotificationListener(Class<T> notificationType, NotificationListener<T> listener) {
        this.listeners.put(notificationType, listener);
    }

    @Override
    public void addNotificationListener(org.opendaylight.yangtools.yang.binding.NotificationListener listener) {
        UnsupportedOperationException _unsupportedOperationException = new UnsupportedOperationException("Deprecated method. Use registerNotificationListener instead.");
        throw _unsupportedOperationException;
    }

    @Override
    public void removeNotificationListener(org.opendaylight.yangtools.yang.binding.NotificationListener listener) {
        UnsupportedOperationException _unsupportedOperationException = new UnsupportedOperationException(
                "Deprecated method. Use RegisterNotificationListener returned value to close registration.");
        throw _unsupportedOperationException;
    }

    @Override
    public <T extends Notification> void removeNotificationListener(Class<T> notificationType, NotificationListener<T> listener) {
        this.listeners.remove(notificationType, listener);
    }

    @Override
    public <T extends Notification> Registration<NotificationListener<T>> registerNotificationListener(Class<T> notificationType, NotificationListener<T> listener) {
        //TODO implementation using sal-remote
        List<QName> notifications = new ArrayList<QName>();
        notifications.add(new QName(notificationType.toString()));
        String notificationStreamName = RemoteStreamTools.createNotificationStream(salRemoteService, notifications);
        final Map<String,EventStreamInfo> desiredEventStream = RemoteStreamTools.createEventStream(restconfClientContext, notificationStreamName);
        RemoteNotificationListener remoteNotificationListener = new RemoteNotificationListener(listener);
        ListenerRegistration<?> listenerRegistration = restconfClientContext.getEventStreamContext(desiredEventStream.get(desiredEventStream.get(notificationStreamName))).registerNotificationListener(remoteNotificationListener);
        return new SalNotificationRegistration<T>(listenerRegistration);
    }

    @Override
    public Registration<org.opendaylight.yangtools.yang.binding.NotificationListener> registerNotificationListener(org.opendaylight.yangtools.yang.binding.NotificationListener listener) {
        //TODO implementation using sal-remote
        String notificationStreamName = RemoteStreamTools.createNotificationStream(salRemoteService, null);
        final Map<String,EventStreamInfo> desiredEventStream = RemoteStreamTools.createEventStream(restconfClientContext, notificationStreamName);
        return restconfClientContext.getEventStreamContext(desiredEventStream.get(desiredEventStream.get(notificationStreamName))).registerNotificationListener(listener);
    }

    private class SalNotificationRegistration<T extends Notification> implements Registration<NotificationListener<T>>{
        private final Registration<?> registration;

        public SalNotificationRegistration(ListenerRegistration<?> listenerRegistration){
            this.registration = listenerRegistration;
        }

        @Override
        public NotificationListener<T> getInstance() {
            return this.getInstance();
        }

        @Override
        public void close() throws Exception {
            this.registration.close();
        }
    }


}
