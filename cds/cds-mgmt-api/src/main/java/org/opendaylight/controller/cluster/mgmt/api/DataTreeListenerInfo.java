/*
 * Copyright (c) 2017 Inocybe Technologies and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.mgmt.api;

import static java.util.Objects.requireNonNull;

import javax.management.ConstructorParameters;
import org.eclipse.jdt.annotation.NonNullByDefault;

/**
 * Information about a registered listener.
 *
 * @author Thomas Pantelis
 */
@NonNullByDefault
public final class DataTreeListenerInfo {
    private final String listener;
    private final String registeredPath;
    private final boolean isEnabled;
    private final long notificationCount;

    @ConstructorParameters({"listener","registeredPath", "isEnabled", "notificationCount"})
    public DataTreeListenerInfo(final String listener, final String registeredPath, final boolean isEnabled,
            final long notificationCount) {
        this.listener = requireNonNull(listener);
        this.registeredPath = requireNonNull(registeredPath);
        this.isEnabled = isEnabled;
        this.notificationCount = notificationCount;
    }

    public String getListener() {
        return listener;
    }

    public String getRegisteredPath() {
        return registeredPath;
    }

    public boolean isEnabled() {
        return isEnabled;
    }

    public long getNotificationCount() {
        return notificationCount;
    }
}
