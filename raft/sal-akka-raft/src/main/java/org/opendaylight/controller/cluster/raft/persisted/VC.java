/*
 * Copyright (c) 2026 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft.persisted;

import java.io.Serializable;
import java.util.List;
import org.eclipse.jdt.annotation.NonNullByDefault;

/**
 *
 */
@NonNullByDefault
record VC(List<ServerInfo> serverInfo) implements Serializable {
    VC {
        serverInfo = List.copyOf(serverInfo);
    }

    @java.io.Serial
    private Object readResolve() {
        return new VotingConfig(serverInfo);
    }
}
