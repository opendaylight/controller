/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.netconf.notifications;

import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.netconf.notification._1._0.rev080714.StreamNameType;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.netmod.notification.rev080714.netconf.streams.Stream;

/**
 * Collector of all notifications. Base or generic
 */
public interface NetconfNotificationCollector  {

    /**
     * Add notification publisher for a particular stream
     *
     * Implementations should allow for multiple publishers of a single stream
     * and its up to implementations to decide how to merge metadata (e.g. description)
     * for the same stream when providing information about available stream
     *
     */
    NotificationPublisherRegistration registerNotificationPublisher(Stream stream);

    /**
     * Register base notification publisher
     */
    BaseNotificationPublisherRegistration registerBaseNotificationPublisher();

    /**
     * Users of the registry have an option to get notification each time new notification stream gets registered
     * This allows for a push model in addition to pull model for retrieving information about available streams.
     *
     * The listener should receive callbacks for each stream available prior to the registration when its registered
     */
    NotificationRegistration registerStreamListener(NetconfNotificationStreamListener listener);

    /**
     * Simple listener that receives notifications about changes in stream availability
     */
    public interface NetconfNotificationStreamListener {

        /**
         * Stream becomes available in the collector (first publisher is registered)
         */
        void onStreamRegistered(Stream stream);

        /**
         * Stream is not available anymore in the collector (last publisher is unregistered)
         */
        void onStreamUnregistered(StreamNameType stream);
    }

}
