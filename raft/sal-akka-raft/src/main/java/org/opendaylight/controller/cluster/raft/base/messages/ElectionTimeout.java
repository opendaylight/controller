/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.raft.base.messages;

import org.apache.pekko.dispatch.ControlMessage;

/**
 * Local message sent to indicate the current election term has timed out.
 */
public final class ElectionTimeout implements ControlMessage {
    public static final ElectionTimeout INSTANCE = new ElectionTimeout();

    private ElectionTimeout() {
        // Hidden on purpose
    }

    private Object readResolve() {
        return INSTANCE;
    }
}
