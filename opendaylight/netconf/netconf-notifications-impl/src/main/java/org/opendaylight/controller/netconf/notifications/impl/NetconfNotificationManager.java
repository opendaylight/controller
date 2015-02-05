/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.netconf.notifications.impl;

import com.google.common.base.Preconditions;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.HashMultiset;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multiset;
import com.google.common.collect.Sets;
import java.util.Map;
import java.util.Set;
import javax.annotation.concurrent.GuardedBy;
import javax.annotation.concurrent.ThreadSafe;
import org.opendaylight.controller.netconf.notifications.BaseNotificationPublisherRegistration;
import org.opendaylight.controller.netconf.notifications.NetconfNotification;
import org.opendaylight.controller.netconf.notifications.NetconfNotificationCollector;
import org.opendaylight.controller.netconf.notifications.NetconfNotificationListener;
import org.opendaylight.controller.netconf.notifications.NetconfNotificationRegistry;
import org.opendaylight.controller.netconf.notifications.NotificationListenerRegistration;
import org.opendaylight.controller.netconf.notifications.NotificationPublisherRegistration;
import org.opendaylight.controller.netconf.notifications.NotificationRegistration;
import org.opendaylight.controller.netconf.notifications.impl.ops.NotificationsTransformUtil;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.netconf.notification._1._0.rev080714.StreamNameType;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.netmod.notification.rev080714.netconf.Streams;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.netmod.notification.rev080714.netconf.StreamsBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.netmod.notification.rev080714.netconf.streams.Stream;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.netmod.notification.rev080714.netconf.streams.StreamBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.netmod.notification.rev080714.netconf.streams.StreamKey;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.netconf.notifications.rev120206.NetconfCapabilityChange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ThreadSafe
public class NetconfNotificationManager implements NetconfNotificationCollector, NetconfNotificationRegistry, NetconfNotificationListener, AutoCloseable {

    public static final StreamNameType BASE_STREAM_NAME = new StreamNameType("NETCONF");
    public static final Stream BASE_NETCONF_STREAM;

    static {
        BASE_NETCONF_STREAM = new StreamBuilder()
                .setName(BASE_STREAM_NAME)
                .setKey(new StreamKey(BASE_STREAM_NAME))
                .setReplaySupport(false)
                .setDescription("Default Event Stream")
                .build();
    }

    private static final Logger LOG = LoggerFactory.getLogger(NetconfNotificationManager.class);

    // TODO excessive synchronization provides thread safety but is most likely not optimal (combination of concurrent collections might improve performance)
    // And also calling callbacks from a synchronized block is dangerous since the listeners/publishers can block the whole notification processing

    @GuardedBy("this")
    private final Multimap<StreamNameType, GenericNotificationListenerReg> notificationListeners = HashMultimap.create();

    @GuardedBy("this")
    private final Set<NetconfNotificationStreamListener> streamListeners = Sets.newHashSet();

    @GuardedBy("this")
    private final Map<StreamNameType, Stream> streamMetadata = Maps.newHashMap();

    @GuardedBy("this")
    private final Multiset<StreamNameType> availableStreams = HashMultiset.create();

    @GuardedBy("this")
    private final Set<GenericNotificationPublisherReg> notificationPublishers = Sets.newHashSet();

    @Override
    public synchronized void onNotification(final StreamNameType stream, final NetconfNotification notification) {
        LOG.debug("Notification of type {} detected", stream);
        if(LOG.isTraceEnabled()) {
            LOG.debug("Notification of type {} detected: {}", stream, notification);
        }

        for (final GenericNotificationListenerReg listenerReg : notificationListeners.get(BASE_STREAM_NAME)) {
            listenerReg.getListener().onNotification(BASE_STREAM_NAME, notification);
        }
    }

    @Override
    public synchronized NotificationListenerRegistration registerNotificationListener(final StreamNameType stream, final NetconfNotificationListener listener) {
        Preconditions.checkNotNull(stream);
        Preconditions.checkNotNull(listener);

        LOG.trace("Notification listener registered for stream: {}", stream);

        final GenericNotificationListenerReg genericNotificationListenerReg = new GenericNotificationListenerReg(listener) {
            @Override
            public void close() {
                synchronized (NetconfNotificationManager.this) {
                    LOG.trace("Notification listener unregistered for stream: {}", stream);
                    super.close();
                }
            }
        };

        notificationListeners.put(BASE_STREAM_NAME, genericNotificationListenerReg);
        return genericNotificationListenerReg;
    }

    @Override
    public synchronized Streams getNotificationPublishers() {
        return new StreamsBuilder().setStream(Lists.newArrayList(streamMetadata.values())).build();
    }

    @Override
    public synchronized boolean isStreamAvailable(final StreamNameType streamNameType) {
        return availableStreams.contains(streamNameType);
    }

    @Override
    public synchronized NotificationRegistration registerStreamListener(final NetconfNotificationStreamListener listener) {
        streamListeners.add(listener);

        // Notify about all already available
        for (final Stream availableStream : streamMetadata.values()) {
            listener.onStreamRegistered(availableStream);
        }

        return new NotificationRegistration() {
            @Override
            public void close() {
                synchronized(NetconfNotificationManager.this) {
                    streamListeners.remove(listener);
                }
            }
        };
    }

    @Override
    public synchronized void close() {
        // Unregister all listeners
        for (final GenericNotificationListenerReg genericNotificationListenerReg : notificationListeners.values()) {
            genericNotificationListenerReg.close();
        }
        notificationListeners.clear();

        // Unregister all publishers
        for (final GenericNotificationPublisherReg notificationPublisher : notificationPublishers) {
            notificationPublisher.close();
        }
        notificationPublishers.clear();

        // Clear stream Listeners
        streamListeners.clear();
    }

    @Override
    public synchronized NotificationPublisherRegistration registerNotificationPublisher(final Stream stream) {
        Preconditions.checkNotNull(stream);
        final StreamNameType streamName = stream.getName();

        LOG.debug("Notification publisher registered for stream: {}", streamName);
        if(LOG.isTraceEnabled()) {
            LOG.trace("Notification publisher registered for stream: {}", stream);
        }

        if(streamMetadata.containsKey(streamName)) {
            LOG.warn("Notification stream {} already registered as: {}. Will be reused", streamName, streamMetadata.get(streamName));
        } else {
            streamMetadata.put(streamName, stream);
        }

        availableStreams.add(streamName);

        final GenericNotificationPublisherReg genericNotificationPublisherReg = new GenericNotificationPublisherReg(this, streamName) {
            @Override
            public void close() {
                synchronized (NetconfNotificationManager.this) {
                    super.close();
                }
            }
        };

        notificationPublishers.add(genericNotificationPublisherReg);

        notifyStreamAdded(stream);
        return genericNotificationPublisherReg;
    }

    private void unregisterNotificationPublisher(final StreamNameType streamName, final GenericNotificationPublisherReg genericNotificationPublisherReg) {
        availableStreams.remove(streamName);
        notificationPublishers.remove(genericNotificationPublisherReg);

        LOG.debug("Notification publisher unregistered for stream: {}", streamName);

        // Notify stream listeners if all publishers are gone and also clear metadata for stream
        if (!isStreamAvailable(streamName)) {
            LOG.debug("Notification stream: {} became unavailable", streamName);
            streamMetadata.remove(streamName);
            notifyStreamRemoved(streamName);
        }
    }

    private synchronized void notifyStreamAdded(final Stream stream) {
        for (final NetconfNotificationStreamListener streamListener : streamListeners) {
            streamListener.onStreamRegistered(stream);
        }
    }
    private synchronized void notifyStreamRemoved(final StreamNameType stream) {
        for (final NetconfNotificationStreamListener streamListener : streamListeners) {
            streamListener.onStreamUnregistered(stream);
        }
    }

    @Override
    public BaseNotificationPublisherRegistration registerBaseNotificationPublisher() {
        final NotificationPublisherRegistration notificationPublisherRegistration = registerNotificationPublisher(BASE_NETCONF_STREAM);
        return new BaseNotificationPublisherReg(notificationPublisherRegistration);
    }

    private static class GenericNotificationPublisherReg implements NotificationPublisherRegistration {
        private NetconfNotificationManager baseListener;
        private final StreamNameType registeredStream;

        public GenericNotificationPublisherReg(final NetconfNotificationManager baseListener, final StreamNameType registeredStream) {
            this.baseListener = baseListener;
            this.registeredStream = registeredStream;
        }

        @Override
        public void close() {
            baseListener.unregisterNotificationPublisher(registeredStream, this);
            baseListener = null;
        }

        @Override
        public void onNotification(final StreamNameType stream, final NetconfNotification notification) {
            Preconditions.checkState(baseListener != null, "Already closed");
            Preconditions.checkArgument(stream.equals(registeredStream));
            baseListener.onNotification(stream, notification);
        }
    }

    private static class BaseNotificationPublisherReg implements BaseNotificationPublisherRegistration {

        private final NotificationPublisherRegistration baseRegistration;

        public BaseNotificationPublisherReg(final NotificationPublisherRegistration baseRegistration) {
            this.baseRegistration = baseRegistration;
        }

        @Override
        public void close() {
            baseRegistration.close();
        }

        @Override
        public void onCapabilityChanged(final NetconfCapabilityChange capabilityChange) {
            baseRegistration.onNotification(BASE_STREAM_NAME, serializeNotification(capabilityChange));
        }

        private static NetconfNotification serializeNotification(final NetconfCapabilityChange capabilityChange) {
            return NotificationsTransformUtil.transform(capabilityChange);
        }
    }

    private class GenericNotificationListenerReg implements NotificationListenerRegistration {
        private final NetconfNotificationListener listener;

        public GenericNotificationListenerReg(final NetconfNotificationListener listener) {
            this.listener = listener;
        }

        public NetconfNotificationListener getListener() {
            return listener;
        }

        @Override
        public void close() {
            notificationListeners.remove(BASE_STREAM_NAME, this);
        }
    }
}
