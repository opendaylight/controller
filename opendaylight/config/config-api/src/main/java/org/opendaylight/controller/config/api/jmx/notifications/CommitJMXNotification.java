/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.config.api.jmx.notifications;

import javax.management.NotificationBroadcasterSupport;

public class CommitJMXNotification extends ConfigJMXNotification {

    private static final String AFTER_COMMIT_MESSAGE_TEMPLATE = "Commit successful: %s";

    CommitJMXNotification(NotificationBroadcasterSupport source, String message) {
        super(ConfigJMXNotification.NotificationType.COMMIT,  source, String.format(AFTER_COMMIT_MESSAGE_TEMPLATE, message));
    }

    @Override
    public String toString() {
        final StringBuffer sb = new StringBuffer("CommitJMXNotification{");
        sb.append('}');
        return sb.toString();
    }

    /**
     *
     */
    private static final long serialVersionUID = -8587623362011695514L;

}
