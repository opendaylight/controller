/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.netconf.notifications;

import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.netconf.notification._1._0.rev080714.StreamNameType;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.netmod.notification.rev080714.netconf.Streams;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.netmod.notification.rev080714.netconf.streams.Stream;

public interface NetconfNotificationRegistry {

    NotificationListenerRegistration registerNotificationListener(StreamNameType stream, NetconfNotificationListener listener);

    Streams getAvailableStreams();

    boolean isStreamAvailable(StreamNameType streamNameType);

    /**
     * Users of the registry have an option to get notification each time new notification stream gets registered
     */
    NotificationRegistration registerStreamListener(NetconfNotificationStreamListener listener);

    public interface NetconfNotificationStreamListener {

        void onStreamRegistered(Stream stream);

        void onStreamUnregistered(StreamNameType stream);
    }
}
