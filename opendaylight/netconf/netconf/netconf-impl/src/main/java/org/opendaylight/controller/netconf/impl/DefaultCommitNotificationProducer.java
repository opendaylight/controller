/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.netconf.impl;

import java.util.Set;
import javax.management.InstanceAlreadyExistsException;
import javax.management.InstanceNotFoundException;
import javax.management.MBeanRegistrationException;
import javax.management.MBeanServer;
import javax.management.NotCompliantMBeanException;
import javax.management.NotificationBroadcasterSupport;
import javax.management.ObjectName;
import org.opendaylight.controller.netconf.api.jmx.CommitJMXNotification;
import org.opendaylight.controller.netconf.api.jmx.DefaultCommitOperationMXBean;
import org.opendaylight.controller.netconf.api.jmx.NetconfJMXNotification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;

public class DefaultCommitNotificationProducer extends NotificationBroadcasterSupport implements
        DefaultCommitOperationMXBean, AutoCloseable, CommitNotifier {

    private static final Logger LOG = LoggerFactory.getLogger(DefaultCommitNotificationProducer.class);

    private final MBeanServer mbeanServer;

    private final ObjectName on = DefaultCommitOperationMXBean.OBJECT_NAME;

    public DefaultCommitNotificationProducer(MBeanServer mBeanServer) {
        this.mbeanServer = mBeanServer;
        LOG.debug("Registering to JMX under {}", on);
        registerMBean(this, mbeanServer, on);
    }

    private static void registerMBean(final Object instance, final MBeanServer mbs, final ObjectName on) {
        try {
            mbs.registerMBean(instance, on);
        } catch (InstanceAlreadyExistsException | MBeanRegistrationException | NotCompliantMBeanException e) {
            throw new IllegalStateException("Unable to register " + instance + " as " + on, e);
        }
    }

    @Override
    public void sendCommitNotification(String message, Element cfgSnapshot, Set<String> capabilities) {
        CommitJMXNotification notif = NetconfJMXNotification.afterCommit(this, message, cfgSnapshot, capabilities);
        LOG.debug("Notification about commit {} sent", notif);
        sendNotification(notif);
    }

    @Override
    public void close() {
        try {
            mbeanServer.unregisterMBean(on);
        } catch (InstanceNotFoundException | MBeanRegistrationException e) {
            LOG.warn("Ignoring exception while unregistering {} as {}", this, on, e);
        }
    }
}
