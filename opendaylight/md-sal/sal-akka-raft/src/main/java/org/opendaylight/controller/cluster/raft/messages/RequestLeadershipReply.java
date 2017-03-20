/*
 * Copyright (c) 2017 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.raft.messages;

import java.io.Serializable;

/**
 * Reply to RequestLeadership request containing result of the leadership
 * transfer
 */
// TODO Maybe we should provide reason why the transfer failed, who was the
// leader, who should be leader etc.
public class RequestLeadershipReply implements Serializable {
    private static final long serialVersionUID = 1L;

    private final boolean success;

    public RequestLeadershipReply(final boolean success) {
        this.success = success;
    }

    public boolean isSuccess() {
        return success;
    }
}
