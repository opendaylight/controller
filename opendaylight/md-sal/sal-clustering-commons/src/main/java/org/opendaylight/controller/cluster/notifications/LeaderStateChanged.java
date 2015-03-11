/*
 * Copyright (c) 2015 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.notifications;

import java.io.Serializable;

/**
 * A message initiated internally from the RaftActor when some state of a leader has changed
 *
 * @author Thomas Pantelis
 */
public class LeaderStateChanged implements Serializable {
    private static final long serialVersionUID = 1L;

    private final String memberId;
    private final String leaderId;

    public LeaderStateChanged(String memberId, String leaderId) {
        this.memberId = memberId;
        this.leaderId = leaderId;
    }

    public String getMemberId() {
        return memberId;
    }

    public String getLeaderId() {
        return leaderId;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("LeaderStateChanged [memberId=").append(memberId).append(", leaderId=").append(leaderId)
                .append("]");
        return builder.toString();
    }
}
