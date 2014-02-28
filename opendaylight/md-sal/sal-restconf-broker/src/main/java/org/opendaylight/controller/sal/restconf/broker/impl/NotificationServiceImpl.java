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

import org.opendaylight.controller.sal.binding.api.NotificationListener;
import org.opendaylight.controller.sal.binding.api.NotificationService;
import org.opendaylight.controller.sal.restconf.broker.listeners.RemoteNotificationListener;
import org.opendaylight.controller.sal.restconf.broker.tools.RemoteStreamTools;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.remote.rev140114.QName;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.remote.rev140114.SalRemoteService;
import org.opendaylight.yangtools.concepts.AbstractListenerRegistration;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.restconf.client.api.RestconfClientContext;
import org.opendaylight.yangtools.restconf.client.api.event.EventStreamInfo;
import org.opendaylight.yangtools.yang.binding.Notification;

public class NotificationServiceImpl implements NotificationService {
    private final SalRemoteService salRemoteService;
    private final RestconfClientContext restconfClientContext;

    public NotificationServiceImpl(RestconfClientContext restconfClienetContext){
        this.restconfClientContext = restconfClienetContext;
        this.salRemoteService = this.restconfClientContext.getRpcServiceContext(SalRemoteService.class).getRpcService();
    }

    @Override
    public <T extends Notification> ListenerRegistration<NotificationListener<T>> registerNotificationListener(Class<T> notificationType, NotificationListener<T> listener) {
        //TODO implementation using sal-remote
        List<QName> notifications = new ArrayList<QName>();
        notifications.add(new QName(notificationType.toString()));
        String notificationStreamName = RemoteStreamTools.createNotificationStream(salRemoteService, notifications);
        final Map<String,EventStreamInfo> desiredEventStream = RemoteStreamTools.createEventStream(restconfClientContext, notificationStreamName);
        RemoteNotificationListener remoteNotificationListener = new RemoteNotificationListener(listener);

        final ListenerRegistration<?> listenerRegistration = restconfClientContext.getEventStreamContext(desiredEventStream.get(desiredEventStream.get(notificationStreamName)))
                .registerNotificationListener(remoteNotificationListener);

        return new AbstractListenerRegistration<NotificationListener<T>>(listener) {
            @Override
            protected void removeRegistration() {
                listenerRegistration.close();
            }
        };
    }

    @Override
    public ListenerRegistration<org.opendaylight.yangtools.yang.binding.NotificationListener> registerNotificationListener(org.opendaylight.yangtools.yang.binding.NotificationListener listener) {
        //TODO implementation using sal-remote
        String notificationStreamName = RemoteStreamTools.createNotificationStream(salRemoteService, null);
        final Map<String,EventStreamInfo> desiredEventStream = RemoteStreamTools.createEventStream(restconfClientContext, notificationStreamName);
        return restconfClientContext.getEventStreamContext(desiredEventStream.get(desiredEventStream.get(notificationStreamName))).registerNotificationListener(listener);
    }
}
