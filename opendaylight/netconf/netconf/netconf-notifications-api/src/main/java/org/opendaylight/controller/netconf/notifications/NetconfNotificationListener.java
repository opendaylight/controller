/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.netconf.notifications;

import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.netconf.notification._1._0.rev080714.StreamNameType;

/**
 * Generic listener for netconf notifications
 */
public interface NetconfNotificationListener {

    /**
     * Callback used to notify the listener about any new notification
     */
    void onNotification(StreamNameType stream, NetconfNotification notification);

}
