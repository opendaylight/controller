/*
 * Copyright (c) 2025 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package io.atomix.storage.journal;

import java.io.IOException;

/**
 * Exception thrown when an entry being stored is too large.
 */
public final class EntryTooLargeException extends IOException {
    @java.io.Serial
    private static final long serialVersionUID = 1L;

    public EntryTooLargeException(final String message, final Throwable cause) {
        super(message, cause);
    }
}