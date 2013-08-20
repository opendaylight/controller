/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.northbound.commons.exception;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * Status Code 400 (Bad Request)
 *
 * The request could not be understood by the server due to malformed syntax.
 * The client SHOULD NOT repeat the request without modifications.
 */
public class BadRequestException extends WebApplicationException {
    private static final long serialVersionUID = 1L;

    public BadRequestException(String string) {
        super(Response.status(Response.Status.BAD_REQUEST).entity(string).type(MediaType.TEXT_PLAIN).build());
    }
}
