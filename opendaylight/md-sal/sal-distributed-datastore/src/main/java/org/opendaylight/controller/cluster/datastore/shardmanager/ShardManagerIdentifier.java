/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.shardmanager;

import static java.util.Objects.requireNonNull;

import org.eclipse.jdt.annotation.NonNullByDefault;

@NonNullByDefault
public record ShardManagerIdentifier(String type) {
    public ShardManagerIdentifier {
        requireNonNull(type);
    }

    public String toActorName() {
        return "shardmanager-" + type;
    }

    @Override
    public String toString() {
        return toActorName();
    }
}
