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
 */
public class ShardLeaderNotRespondingException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public ShardLeaderNotRespondingException(String message, Throwable cause) {
        super(message, cause);
    }
}
