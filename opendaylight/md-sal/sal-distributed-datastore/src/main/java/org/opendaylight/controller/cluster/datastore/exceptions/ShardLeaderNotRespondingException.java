/*
 * Copyright (c) 2015 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.exceptions;

/**
 * Exception indicating a shard leader is not responding to messages.
 *
 * @author Thomas Pantelis
 * @deprecated This exception is not reported from anywhere and will be removed in the next major release.
 */
@Deprecated(since = "10.0.12", forRemoval = true)
public class ShardLeaderNotRespondingException extends RuntimeException {
    @java.io.Serial
    private static final long serialVersionUID = 1L;

    public ShardLeaderNotRespondingException(final String message, final Throwable cause) {
        super(message, cause);
    }
}
