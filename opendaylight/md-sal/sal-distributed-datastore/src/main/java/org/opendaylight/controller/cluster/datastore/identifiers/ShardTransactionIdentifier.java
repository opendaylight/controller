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
    private final String stringRepresentation;

    private ShardTransactionIdentifier(String remoteTransactionId) {
        this.remoteTransactionId = Preconditions.checkNotNull(remoteTransactionId,
                "remoteTransactionId should not be null");

        stringRepresentation = new StringBuilder(remoteTransactionId.length() + 6).append("shard-").
                append(remoteTransactionId).toString();
    }

    public static Builder builder(){
        return new Builder();
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
        return stringRepresentation;
    }

    public static class Builder {
        private String remoteTransactionId;

        public Builder remoteTransactionId(String remoteTransactionId){
            this.remoteTransactionId = remoteTransactionId;
            return this;
        }

        public ShardTransactionIdentifier build(){
            return new ShardTransactionIdentifier(remoteTransactionId);
        }

    }
}
