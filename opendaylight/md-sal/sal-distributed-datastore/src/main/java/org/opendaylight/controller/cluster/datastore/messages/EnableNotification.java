/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.datastore.messages;

public class EnableNotification {
    private final boolean enabled;
    private final String logContext;

    public EnableNotification(boolean enabled, String logContext) {
        this.enabled = enabled;
        this.logContext = logContext;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public String getLogContext() {
        return logContext;
    }
}
