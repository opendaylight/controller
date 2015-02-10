/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.netconf.notifications;

import com.google.common.base.Preconditions;
import java.text.SimpleDateFormat;
import java.util.Date;
import org.opendaylight.controller.netconf.api.NetconfMessage;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Special kind of netconf message that contains a timestamp.
 */
public final class NetconfNotification extends NetconfMessage {

    public static final String NOTIFICATION = "notification";
    public static final String NOTIFICATION_NAMESPACE = "urn:ietf:params:netconf:capability:notification:1.0";
    public static final String RFC3339_DATE_FORMAT_BLUEPRINT = "yyyy-MM-dd'T'HH:mm:ssXXX";
    public static final String EVENT_TIME = "eventTime";

    /**
     * Create new notification and capture the timestamp in the constructor
     */
    public NetconfNotification(final Document notificationContent) {
        this(notificationContent, new Date());
    }

    /**
     * Create new notification with provided timestamp
     */
    public NetconfNotification(final Document notificationContent, final Date eventTime) {
        super(wrapNotification(notificationContent, eventTime));
    }

    private static Document wrapNotification(final Document notificationContent, final Date eventTime) {
        Preconditions.checkNotNull(notificationContent);
        Preconditions.checkNotNull(eventTime);

        final Element baseNotification = notificationContent.getDocumentElement();
        final Element entireNotification = notificationContent.createElementNS(NOTIFICATION_NAMESPACE, NOTIFICATION);
        entireNotification.appendChild(baseNotification);

        final Element eventTimeElement = notificationContent.createElementNS(NOTIFICATION_NAMESPACE, EVENT_TIME);
        eventTimeElement.setTextContent(getSerializedEventTime(eventTime));
        entireNotification.appendChild(eventTimeElement);

        notificationContent.appendChild(entireNotification);
        return notificationContent;
    }

    private static String getSerializedEventTime(final Date eventTime) {
        // SimpleDateFormat is not threadsafe, cannot be in a constant
        return new SimpleDateFormat(RFC3339_DATE_FORMAT_BLUEPRINT).format(eventTime);
    }
}
