/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.dom.api;

import javax.annotation.Nonnull;

/**
 * Exception thrown when an attempt to attach a conflicting shard to the global
 * table.
 */
public class DOMDataTreeShardingConflictException extends Exception {
    private static final long serialVersionUID = 1L;

    public DOMDataTreeShardingConflictException(final @Nonnull String message) {
        super(message);
    }

    public DOMDataTreeShardingConflictException(final @Nonnull String message, final @Nonnull Throwable cause) {
        super(message, cause);
    }
}
