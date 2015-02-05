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

    @GuardedBy("this")
    private final Multimap<StreamNameType, NetconfNotificationListener> notificationListeners = HashMultimap.create();
    @GuardedBy("this")
    private final Set<NetconfNotificationStreamListener> streamListeners = Sets.newHashSet();
    @GuardedBy("this")
    private final Map<StreamNameType, Stream> streamMetadata = Maps.newHashMap();
    @GuardedBy("this")
    private final Multiset<StreamNameType> publishers = HashMultiset.create();

    @Override
    public synchronized void onNotification(final StreamNameType stream, final NetconfNotification notification) {
        for (final NetconfNotificationListener netconfNotificationListener : notificationListeners.get(BASE_STREAM_NAME)) {
            netconfNotificationListener.onNotification(BASE_STREAM_NAME, notification);
        }
    }

    @Override
    public synchronized NotificationListenerRegistration registerNotificationListener(final StreamNameType stream, final NetconfNotificationListener listener) {
        Preconditions.checkNotNull(stream);
        Preconditions.checkNotNull(listener);

        notificationListeners.put(BASE_STREAM_NAME, listener);
        return new NotificationListenerRegistration() {
            @Override
            public void close() {
                notificationListeners.remove(BASE_STREAM_NAME, listener);
            }
        };
    }

    @Override
    public synchronized Streams getAvailableStreams() {
        return new StreamsBuilder().setStream(Lists.newArrayList(streamMetadata.values())).build();
    }

    @Override
    public synchronized boolean isStreamAvailable(final StreamNameType streamNameType) {
        return streamMetadata.containsKey(streamNameType);
    }

    @Override
    public NotificationRegistration registerStreamListener(final NetconfNotificationStreamListener listener) {
        streamListeners.add(listener);

        return new NotificationRegistration() {
            @Override
            public void close() {
                streamListeners.remove(listener);
            }
        };
    }

    @Override
    public synchronized void close() {
        notificationListeners.clear();
        //FIXME implement proper cleanup
    }

    @Override
    public synchronized NotificationPublisherRegistration registerNotificationPublisher(final Stream stream) {
        Preconditions.checkNotNull(stream);
        final StreamNameType streamName = stream.getName();

        if(streamMetadata.containsKey(streamName)) {
            LOG.warn("Notification stream {} already registered as: {}. Will be reused", streamName, streamMetadata.get(streamName));
        } else {
            streamMetadata.put(streamName, stream);
        }

        publishers.add(streamName);

        notifyStreamAdded(stream);
        return new GenericNotificationPublisherReg(this, streamName, new AutoCloseable() {
            @Override
            public void close() {
                publishers.remove(streamName);

                // Notify stream listeners if all publishers are gone
                if(publishers.contains(streamName) == false) {
                    notifyStreamRemoved(streamName);
                }
            }
        });
    }

    private void notifyStreamAdded(final Stream stream) {
        for (NetconfNotificationStreamListener streamListener : streamListeners) {
            streamListener.onStreamRegistered(stream);
        }
    }
    private void notifyStreamRemoved(final StreamNameType stream) {
        for (NetconfNotificationStreamListener streamListener : streamListeners) {
            streamListener.onStreamUnregistered(stream);
        }
    }

    @Override
    public BaseNotificationPublisherRegistration registerBaseNotificationPublisher() {
        final NotificationPublisherRegistration notificationPublisherRegistration = registerNotificationPublisher(BASE_NETCONF_STREAM);
        return new BaseNotificationPublisherReg(notificationPublisherRegistration);
    }

    private static final class GenericNotificationPublisherReg implements NotificationPublisherRegistration {
        private NetconfNotificationManager baseListener;
        private final StreamNameType registeredStream;
        private final AutoCloseable publisherPresence;

        public GenericNotificationPublisherReg(final NetconfNotificationManager baseListener, final StreamNameType registeredStream, final AutoCloseable publisherPresence) {
            this.baseListener = baseListener;
            this.registeredStream = registeredStream;
            this.publisherPresence = publisherPresence;
        }

        @Override
        public void close() {
            try {
                publisherPresence.close();
            } catch (final Exception e) {
                // not happening
                throw new RuntimeException(e);
            }
            baseListener = null;
        }

        @Override
        public void onNotification(final StreamNameType stream, final NetconfNotification notification) {
            Preconditions.checkState(baseListener != null, "Already closed");
            Preconditions.checkArgument(stream.equals(registeredStream));
            baseListener.onNotification(stream, notification);
        }
    }

    private static final class BaseNotificationPublisherReg implements BaseNotificationPublisherRegistration {

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
}
