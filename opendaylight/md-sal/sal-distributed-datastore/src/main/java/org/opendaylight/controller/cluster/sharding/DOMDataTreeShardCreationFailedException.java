/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.sharding;

import com.google.common.annotations.Beta;
import javax.annotation.Nonnull;

/**
 * Exception thrown when there was a at any point during the creation of a shard via {@link DistributedShardFactory}.
 */
@Beta
public class DOMDataTreeShardCreationFailedException extends Exception {
    private static final long serialVersionUID = 1L;

    public DOMDataTreeShardCreationFailedException(final @Nonnull String message) {
        super(message);
    }

    public DOMDataTreeShardCreationFailedException(final @Nonnull String message, final @Nonnull Throwable cause) {
        super(message, cause);
    }
}
