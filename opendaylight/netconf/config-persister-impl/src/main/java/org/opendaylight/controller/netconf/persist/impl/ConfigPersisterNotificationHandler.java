/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.netconf.persist.impl;

import java.io.Closeable;
import java.io.IOException;
import javax.annotation.concurrent.ThreadSafe;
import javax.management.InstanceNotFoundException;
import javax.management.MBeanServerConnection;
import javax.management.Notification;
import javax.management.NotificationListener;
import javax.management.ObjectName;
import org.opendaylight.controller.config.persist.api.Persister;
import org.opendaylight.controller.netconf.api.jmx.CommitJMXNotification;
import org.opendaylight.controller.netconf.api.jmx.DefaultCommitOperationMXBean;
import org.opendaylight.controller.netconf.api.jmx.NetconfJMXNotification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Responsible for listening for notifications from netconf (via JMX) containing latest
 * committed configuration that should be persisted, and also for loading last
 * configuration.
 */
@ThreadSafe
public class ConfigPersisterNotificationHandler implements Closeable {

    private static final Logger LOG = LoggerFactory.getLogger(ConfigPersisterNotificationHandler.class);
    private final MBeanServerConnection mBeanServerConnection;
    private final NotificationListener listener;


    public ConfigPersisterNotificationHandler(final MBeanServerConnection mBeanServerConnection, final Persister persisterAggregator) {
        this(mBeanServerConnection, new ConfigPersisterNotificationListener(persisterAggregator));
    }

    public ConfigPersisterNotificationHandler(final MBeanServerConnection mBeanServerConnection, final NotificationListener notificationListener) {
        this.mBeanServerConnection = mBeanServerConnection;
        this.listener = notificationListener;
        registerAsJMXListener(mBeanServerConnection, listener);
    }

    private static void registerAsJMXListener(final MBeanServerConnection mBeanServerConnection, final NotificationListener listener) {
        LOG.trace("Called registerAsJMXListener");
        try {
            mBeanServerConnection.addNotificationListener(DefaultCommitOperationMXBean.OBJECT_NAME, listener, null, null);
        } catch (InstanceNotFoundException | IOException e) {
            throw new IllegalStateException("Cannot register as JMX listener to netconf", e);
        }
    }

    @Override
    public synchronized void close() {
        // unregister from JMX
        final ObjectName on = DefaultCommitOperationMXBean.OBJECT_NAME;
        try {
            if (mBeanServerConnection.isRegistered(on)) {
                mBeanServerConnection.removeNotificationListener(on, listener);
            }
        } catch (final Exception e) {
            LOG.warn("Unable to unregister {} as listener for {}", listener, on, e);
        }
    }
}

class ConfigPersisterNotificationListener implements NotificationListener {
    private static final Logger LOG = LoggerFactory.getLogger(ConfigPersisterNotificationListener.class);

    private final Persister persisterAggregator;

    ConfigPersisterNotificationListener(final Persister persisterAggregator) {
        this.persisterAggregator = persisterAggregator;
    }

    @Override
    public void handleNotification(final Notification notification, final Object handback) {
        if (!(notification instanceof NetconfJMXNotification)) {
            return;
        }

        // Socket should not be closed at this point
        // Activator unregisters this as JMX listener before close is called

        LOG.trace("Received notification {}", notification);
        if (notification instanceof CommitJMXNotification) {
            try {
                handleAfterCommitNotification((CommitJMXNotification) notification);
            } catch (final Exception e) {
                // log exceptions from notification Handler here since
                // notificationBroadcastSupport logs only DEBUG level
                LOG.warn("Failed to handle notification {}", notification, e);
                throw e;
            }
        } else {
            throw new IllegalStateException("Unknown config registry notification type " + notification);
        }
    }

    private void handleAfterCommitNotification(final CommitJMXNotification notification) {
        try {
            persisterAggregator.persistConfig(new CapabilityStrippingConfigSnapshotHolder(notification.getConfigSnapshot(),
                    notification.getCapabilities()));
            LOG.trace("Configuration persisted successfully");
        } catch (final IOException e) {
            throw new RuntimeException("Unable to persist configuration snapshot", e);
        }
    }
}
