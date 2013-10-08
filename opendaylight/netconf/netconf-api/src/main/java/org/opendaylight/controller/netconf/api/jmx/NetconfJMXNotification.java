/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.netconf.api.jmx;

import java.util.Set;

import javax.management.Notification;
import javax.management.NotificationBroadcasterSupport;

import org.w3c.dom.Element;

public abstract class NetconfJMXNotification extends Notification {

    /**
     *
     */
    private static final long serialVersionUID = 6754474563863772845L;

    private static long sequenceNumber = 1;

    private final TransactionProviderJMXNotificationType type;

    protected NetconfJMXNotification(TransactionProviderJMXNotificationType type,
            NotificationBroadcasterSupport source, String message) {
        super(type.toString(), source, sequenceNumber++, System.nanoTime(), message);
        this.type = type;
    }

    @Override
    public String toString() {
        return "TransactionProviderJMXNotification [type=" + type + "]";
    }

    /**
     * Sends this notification using source that created it
     */
    public void send() {
        ((NotificationBroadcasterSupport) getSource()).sendNotification(this);
    }

    /**
     * Creates notification about successful commit execution.
     *
     * Intended for config-persister.
     *
     * @param transactionName
     * @param cfgSnapshot
     */
    public static CommitJMXNotification afterCommit(NotificationBroadcasterSupport source, String message,
            Element cfgSnapshot, Set<String> capabilities) {
        return new CommitJMXNotification(source, message, cfgSnapshot, capabilities);
    }

    static enum TransactionProviderJMXNotificationType {
        commit;
    }

}
