/*
 * Copyright (c) 2014 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.exceptions;

import com.google.common.base.Strings;
import org.opendaylight.controller.cluster.datastore.identifiers.ShardIdentifier;

/**
 * Exception indicating a shard has no current leader.
 *
 * @author Thomas Pantelis
 */
public class NoShardLeaderException extends RuntimeException {
    @java.io.Serial
    private static final long serialVersionUID = 1L;

    public NoShardLeaderException(final String message) {
        super(message);
    }

    public NoShardLeaderException(final String message, final String shardName) {
        super(String.format("%sShard %s currently has no leader. Try again later.",
                Strings.isNullOrEmpty(message) ? "" : message + ". ", shardName));
    }

    public NoShardLeaderException(final ShardIdentifier shardId) {
        this("Shard " + shardId + " currently has no leader. Try again later.");
    }
}
