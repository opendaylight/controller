/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.raft.base.messages;

import java.io.Serializable;

/**
 * This messages is sent to the Leader to prompt it to send a heartbeat
 * to it's followers.
 *
 * Typically the Leader to itself on a schedule
 */
public final class SendHeartBeat implements Serializable {
    private static final long serialVersionUID = 1L;
    public static final SendHeartBeat INSTANCE = new SendHeartBeat();

    private SendHeartBeat() {
        // Hidden on purpose
    }

    @SuppressWarnings({ "static-method", "unused" })
    private SendHeartBeat readResolve() {
        return INSTANCE;
    }
}
