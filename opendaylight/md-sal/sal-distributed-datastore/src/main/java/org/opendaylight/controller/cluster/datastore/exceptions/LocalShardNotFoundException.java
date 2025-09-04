/*
 * Copyright (c) 2014 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.exceptions;

/**
 * Exception thrown when attempting to find a local shard but it doesn't exist.
 *
 * @author Thomas Pantelis
 */
public class LocalShardNotFoundException extends RuntimeException {
    @java.io.Serial
    private static final long serialVersionUID = 1L;

    public LocalShardNotFoundException(final String message) {
        super(message);
    }
}
