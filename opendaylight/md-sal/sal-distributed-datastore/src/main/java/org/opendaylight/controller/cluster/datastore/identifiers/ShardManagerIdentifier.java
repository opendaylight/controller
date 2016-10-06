/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.datastore.identifiers;

public class ShardManagerIdentifier {
    private final String type;

    public ShardManagerIdentifier(String type) {
        this.type = type;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }

        ShardManagerIdentifier that = (ShardManagerIdentifier) obj;

        if (!type.equals(that.type)) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        return type.hashCode();
    }

    @Override public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("shardmanager-").append(type);
        return builder.toString();
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String type;

        public Builder type(String newType) {
            this.type = newType;
            return this;
        }

        public ShardManagerIdentifier build() {
            return new ShardManagerIdentifier(this.type);
        }

    }
}
