/*
 * Copyright (c) 2018 Inocybe Technologies and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.core.compat;

import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.yangtools.util.concurrent.ExceptionMapper;

/**
 * Adapter that maps the controller API-based ReadFailedException to the mdsal API-based ReadFailedException.
 *
 * @author Thomas Pantelis
 */
public final class ReadFailedExceptionAdapter extends ExceptionMapper<ReadFailedException> {
    public static final ReadFailedExceptionAdapter INSTANCE = new ReadFailedExceptionAdapter();

    private ReadFailedExceptionAdapter() {
        super("read", ReadFailedException.class);
    }

    @Override
    protected ReadFailedException newWithCause(String message, Throwable cause) {
        if (cause instanceof org.opendaylight.mdsal.common.api.ReadFailedException) {
            return new ReadFailedException(cause.getMessage(), cause.getCause());
        }

        return new ReadFailedException(message, cause);
    }
}
