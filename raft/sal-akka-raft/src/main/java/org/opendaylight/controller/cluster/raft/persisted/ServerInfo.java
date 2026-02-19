/*
 * Copyright (c) 2015 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft.persisted;

import static java.util.Objects.requireNonNull;

import java.io.Serializable;
import org.eclipse.jdt.annotation.NonNull;

/**
 * Server information. This class is not directly Serializable, as it is serialized directly as part of
 * {@link ServerConfigurationPayload}.
 *
 * @author Thomas Pantelis
 */
public record ServerInfo(@NonNull String peerId, boolean isVoting) implements Serializable {
    public ServerInfo {
        requireNonNull(peerId);
    }
}
