/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.netconf.api;

import org.opendaylight.protocol.framework.TerminationReason;

public class NetconfTerminationReason implements TerminationReason {

    private final String reason;

    public NetconfTerminationReason(String reason) {
        this.reason = reason;
    }

    @Override
    public String getErrorMessage() {
        return reason;
    }

    @Override
    public String toString() {
        return reason;
    }
}
