/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.connect.netconf;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import java.util.LinkedList;
import java.util.List;
import org.opendaylight.controller.netconf.api.NetconfMessage;
import org.opendaylight.controller.netconf.util.xml.XmlUtil;
import org.opendaylight.controller.sal.connect.api.MessageTransformer;
import org.opendaylight.controller.sal.connect.api.RemoteDeviceHandler;
import org.opendaylight.controller.sal.connect.util.RemoteDeviceId;
import org.opendaylight.yangtools.yang.data.api.CompositeNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handles incoming notifications. Either caches them(until onRemoteSchemaUp is called) or passes to sal Facade.
 */
final class NotificationHandler {

    private static final Logger logger = LoggerFactory.getLogger(NotificationHandler.class);

    private final RemoteDeviceHandler<?> salFacade;
    private final List<NetconfMessage> queue = new LinkedList<>();
    private final MessageTransformer<NetconfMessage> messageTransformer;
    private final RemoteDeviceId id;
    private boolean passNotifications = false;
    private NotificationFilter filter;

    NotificationHandler(final RemoteDeviceHandler<?> salFacade, final MessageTransformer<NetconfMessage> messageTransformer, final RemoteDeviceId id) {
        this.salFacade = Preconditions.checkNotNull(salFacade);
        this.messageTransformer = Preconditions.checkNotNull(messageTransformer);
        this.id = Preconditions.checkNotNull(id);
    }

    synchronized void handleNotification(final NetconfMessage notification) {
        if(passNotifications) {
            passNotification(messageTransformer.toNotification(notification));
        } else {
            queueNotification(notification);
        }
    }

    /**
     * Forward all cached notifications and pass all notifications from this point directly to sal facade.
     */
    synchronized void onRemoteSchemaUp() {
        passNotifications = true;

        for (final NetconfMessage cachedNotification : queue) {
            passNotification(messageTransformer.toNotification(cachedNotification));
        }

        queue.clear();
    }

    private void queueNotification(final NetconfMessage notification) {
        Preconditions.checkState(passNotifications == false);

        logger.debug("{}: Caching notification {}, remote schema not yet fully built", id, notification);
        if(logger.isTraceEnabled()) {
            logger.trace("{}: Caching notification {}", id, XmlUtil.toString(notification.getDocument()));
        }

        queue.add(notification);
    }

    private synchronized void passNotification(final CompositeNode parsedNotification) {
        logger.debug("{}: Forwarding notification {}", id, parsedNotification);
        Preconditions.checkNotNull(parsedNotification);

        if(filter == null || filter.filterNotification(parsedNotification).isPresent()) {
            salFacade.onNotification(parsedNotification);
        }
    }

    synchronized void addNotificationFilter(final NotificationFilter filter) {
        this.filter = filter;
    }

    static interface NotificationFilter {

        Optional<CompositeNode> filterNotification(CompositeNode notification);
    }
}
