/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.connect.netconf;

import org.opendaylight.controller.netconf.api.NetconfMessage;
import org.opendaylight.controller.netconf.client.AbstractNetconfClientNotifySessionListener;
import org.opendaylight.controller.netconf.client.NetconfClientSession;
import org.opendaylight.controller.sal.core.api.mount.MountProvisionInstance;
import org.opendaylight.yangtools.yang.data.api.CompositeNode;

import com.google.common.base.Preconditions;

class NetconfDeviceListener extends AbstractNetconfClientNotifySessionListener {
    private final NetconfDevice device;

    public NetconfDeviceListener(final NetconfDevice device) {
        this.device = Preconditions.checkNotNull(device);
    }

    /**
     * Method intended to customize notification processing.
     * 
     * @param session
     *            {@see
     *            NetconfClientSessionListener#onMessage(NetconfClientSession,
     *            NetconfMessage)}
     * @param message
     *            {@see
     *            NetconfClientSessionListener#onMessage(NetconfClientSession,
     *            NetconfMessage)}
     */
    @Override
    public void onNotification(final NetconfClientSession session, final NetconfMessage message) {
        this.device.logger.debug("Received NETCONF notification.", message);
        CompositeNode domNotification = null;
        if (message != null) {
            domNotification = NetconfMapping.toNotificationNode(message, device.getSchemaContext());
        }
        if (domNotification != null) {
            MountProvisionInstance _mountInstance = null;
            if (this.device != null) {
                _mountInstance = this.device.getMountInstance();
            }
            if (_mountInstance != null) {
                _mountInstance.publish(domNotification);
            }
        }
    }
}
