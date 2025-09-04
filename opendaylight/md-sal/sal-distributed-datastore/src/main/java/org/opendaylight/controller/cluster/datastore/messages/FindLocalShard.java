/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.messages;

import static java.util.Objects.requireNonNull;

import org.eclipse.jdt.annotation.NonNullByDefault;

/**
 * FindLocalShard is a message that should be sent to the ShardManager when we need to find a reference to a LocalShard.
 */
@NonNullByDefault
public record FindLocalShard(String shardName, boolean waitUntilInitialized) {
    public FindLocalShard {
        requireNonNull(shardName);
    }
}
