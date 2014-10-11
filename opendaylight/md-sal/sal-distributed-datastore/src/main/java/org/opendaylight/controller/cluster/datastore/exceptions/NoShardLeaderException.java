/*
 * Copyright (c) 2014 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.exceptions;

/**
 * Exception indicating a shard has no current leader.
 *
 * @author Thomas Pantelis
 */
public class NoShardLeaderException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public NoShardLeaderException(String message){
        super(message);
    }
}
