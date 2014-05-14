/**
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.northbound.commons.query;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * Signals that an error happened during the parsing or processing of a query.
 */
public class QueryException extends WebApplicationException {

    private static final long serialVersionUID = 1L;

    public QueryException(String msg) {
        super(Response.status(Response.Status.BAD_REQUEST)
                .entity(msg).type(MediaType.TEXT_PLAIN).build());
    }

    public QueryException(String msg, Throwable cause) {
        super(cause, Response.status(Response.Status.BAD_REQUEST)
                .entity(msg).type(MediaType.TEXT_PLAIN).build());
    }
}
