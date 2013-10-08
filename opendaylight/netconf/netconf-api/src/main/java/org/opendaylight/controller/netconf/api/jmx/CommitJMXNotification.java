/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.netconf.api.jmx;

import org.w3c.dom.Element;

import javax.management.NotificationBroadcasterSupport;
import java.util.Set;

public class CommitJMXNotification extends NetconfJMXNotification {

    private final Element configSnapshot;

    private static final String afterCommitMessageTemplate = "Commit successful: %s";
    private final Set<String> capabilities;

    CommitJMXNotification(NotificationBroadcasterSupport source, String message, Element cfgSnapshot,
            Set<String> capabilities) {
        super(TransactionProviderJMXNotificationType.commit, source, String.format(afterCommitMessageTemplate, message));
        this.configSnapshot = cfgSnapshot;
        this.capabilities = capabilities;
    }

    public Element getConfigSnapshot() {
        return configSnapshot;
    }

    public Set<String> getCapabilities() {
        return capabilities;
    }

    @Override
    public String toString() {
        final StringBuffer sb = new StringBuffer("CommitJMXNotification{");
        sb.append("configSnapshot=").append(configSnapshot);
        sb.append(", capabilities=").append(getCapabilities());
        sb.append('}');
        return sb.toString();
    }

    /**
     *
     */
    private static final long serialVersionUID = -8587623362011695514L;

}
