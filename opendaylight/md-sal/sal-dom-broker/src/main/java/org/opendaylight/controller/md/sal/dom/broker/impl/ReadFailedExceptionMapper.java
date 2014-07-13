/*
 * Copyright (c) 2014 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.md.sal.dom.broker.impl;

import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.controller.sal.common.util.ExceptionMapper;

/**
 * Utility exception mapper which translates Exception to {@link ReadFailedException}.
 *
 * @see ExceptionMapper
 *
 * @author Thomas Pantelis
 */
public class ReadFailedExceptionMapper extends ExceptionMapper<ReadFailedException> {

    public static final ReadFailedExceptionMapper MAPPER = new ReadFailedExceptionMapper();

    private ReadFailedExceptionMapper() {
        super( "read", ReadFailedException.class );
    }

    @Override
    protected ReadFailedException newWithCause( String message, Throwable cause ) {
        return new ReadFailedException( message, cause );
    }
}
