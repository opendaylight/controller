/*
 * Copyright (c) 2014 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.exceptions;

import com.google.common.base.Strings;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import javax.annotation.Nullable;

/**
 * Exception indicating a shard has no current leader.
 *
 * @author Thomas Pantelis
 */
public class NoShardLeaderException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public NoShardLeaderException(String message) {
        super(message);
    }

    @SuppressFBWarnings(value = "NP_PARAMETER_MUST_BE_NONNULL_BUT_MARKED_AS_NULLABLE",
            justification = "Unrecognised NullableDecl")
    public NoShardLeaderException(@Nullable String message, String shardName) {
        super(String.format("%sShard %s currently has no leader. Try again later.",
                (Strings.isNullOrEmpty(message) ? "" : message + ". "), shardName));
    }
}
