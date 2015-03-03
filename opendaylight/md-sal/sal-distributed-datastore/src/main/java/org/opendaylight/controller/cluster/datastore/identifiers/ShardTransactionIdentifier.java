/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.datastore.identifiers;

import com.google.common.base.Preconditions;

public class ShardTransactionIdentifier {
    private final String remoteTransactionId;

    public ShardTransactionIdentifier(String remoteTransactionId) {
        this.remoteTransactionId = Preconditions.checkNotNull(remoteTransactionId,
                "remoteTransactionId should not be null");
    }

    public String getRemoteTransactionId() {
        return remoteTransactionId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        ShardTransactionIdentifier that = (ShardTransactionIdentifier) o;

        if (!remoteTransactionId.equals(that.remoteTransactionId)) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        return remoteTransactionId.hashCode();
    }

    @Override public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("shard-").append(remoteTransactionId);
        return sb.toString();
    }

}
