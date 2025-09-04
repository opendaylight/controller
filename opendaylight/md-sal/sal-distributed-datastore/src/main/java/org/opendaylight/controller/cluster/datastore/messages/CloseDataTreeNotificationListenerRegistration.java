/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.messages;

import java.io.Serializable;
import org.eclipse.jdt.annotation.NonNullByDefault;

// FIXME: define an alternative message with replyTo
@NonNullByDefault
public final class CloseDataTreeNotificationListenerRegistration implements Serializable {
    @java.io.Serial
    private static final long serialVersionUID = 1L;

    public static final CloseDataTreeNotificationListenerRegistration INSTANCE =
            new CloseDataTreeNotificationListenerRegistration();

    private CloseDataTreeNotificationListenerRegistration() {
        // Hidden on purpose
    }

    @java.io.Serial
    private Object readResolve() {
        return INSTANCE;
    }
}
