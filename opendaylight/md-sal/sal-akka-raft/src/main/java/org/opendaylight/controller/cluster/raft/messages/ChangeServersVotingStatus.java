/*
 * Copyright (c) 2016 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft.messages;

import com.google.common.base.Preconditions;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.Nonnull;

/**
 * Message sent to change the raft voting status for servers.
 *
 * @author Thomas Pantelis
 */
public class ChangeServersVotingStatus implements Serializable {
    private static final long serialVersionUID = 1L;

    private final Map<String, Boolean> serverVotingStatusMap;

    public ChangeServersVotingStatus(@Nonnull Map<String, Boolean> serverVotingStatusMap) {
        this.serverVotingStatusMap = new HashMap<>(Preconditions.checkNotNull(serverVotingStatusMap));
    }

    public Map<String, Boolean> getServerVotingStatusMap() {
        return serverVotingStatusMap;
    }

    @Override
    public String toString() {
        return "ChangeServersVotingStatus [serverVotingStatusMap=" + serverVotingStatusMap + "]";
    }
}
