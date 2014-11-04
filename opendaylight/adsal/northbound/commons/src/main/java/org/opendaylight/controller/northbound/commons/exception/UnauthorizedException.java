/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.northbound.commons.exception;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.MediaType;

/**
 * Status Code 401 (Unauthorized)
 *
 * The request requires user authentication. The response MUST include
 * a WWW-Authenticate header field (section 14.47) containing a
 * challenge applicable to the requested resource.
 *
 *
 *
 */
public class UnauthorizedException extends WebApplicationException {
    private static final long serialVersionUID = 1L;

    public UnauthorizedException(String string) {
        super(Response.status(Response.Status.UNAUTHORIZED).entity(string)
                .type(MediaType.TEXT_PLAIN).build());
    }
}
