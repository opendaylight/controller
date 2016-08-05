/*
 * Copyright (c) 2015 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft.persisted;

import com.google.common.base.Preconditions;
import javax.annotation.Nonnull;

/**
 * Server information. This class is not directly Serializable, as it is serialized directly as part of
 * {@link ServerConfigurationPayload}.
 *
 * @author Thomas Pantelis
 */
public final class ServerInfo {
    private final String id;
    private final boolean isVoting;

    public ServerInfo(@Nonnull String id, boolean isVoting) {
        this.id = Preconditions.checkNotNull(id);
        this.isVoting = isVoting;
    }

    @Nonnull
    public String getId() {
        return id;
    }

    public boolean isVoting() {
        return isVoting;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + Boolean.hashCode(isVoting);
        result = prime * result + id.hashCode();
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof ServerInfo)) {
            return false;
        }

        final ServerInfo other = (ServerInfo) obj;
        return isVoting == other.isVoting && id.equals(other.id);
    }

    @Override
    public String toString() {
        return "ServerInfo [id=" + id + ", isVoting=" + isVoting + "]";
    }
}