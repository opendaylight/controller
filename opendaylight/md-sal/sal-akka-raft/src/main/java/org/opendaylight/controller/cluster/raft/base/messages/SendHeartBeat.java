/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.raft.base.messages;

import akka.dispatch.ControlMessage;

/**
 * This messages is sent via a schedule to the Leader to prompt it to send a heart beat to its followers.
 */
public final class SendHeartBeat implements ControlMessage {
    public static final SendHeartBeat INSTANCE = new SendHeartBeat();

    private SendHeartBeat() {
        // Hidden on purpose
    }

    private Object readResolve() {
        return INSTANCE;
    }
}
