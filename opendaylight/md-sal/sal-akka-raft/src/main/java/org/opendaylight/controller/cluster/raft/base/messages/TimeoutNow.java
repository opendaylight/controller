/*
 * Copyright (c) 2016 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft.base.messages;

import java.io.Serializable;
import org.apache.pekko.dispatch.ControlMessage;

/**
 * Message sent to a follower to force an immediate election time out.
 *
 * @author Thomas Pantelis
 */
public final class TimeoutNow implements Serializable, ControlMessage {
    @java.io.Serial
    private static final long serialVersionUID = 1L;

    public static final TimeoutNow INSTANCE = new TimeoutNow();

    private TimeoutNow() {
        // Hidden on purpose
    }

    @java.io.Serial
    @SuppressWarnings("static-method")
    private Object readResolve() {
        return INSTANCE;
    }
}
