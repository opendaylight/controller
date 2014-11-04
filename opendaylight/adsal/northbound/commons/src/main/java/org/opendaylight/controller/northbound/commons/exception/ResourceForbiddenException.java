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
 * Status Code 403 (Forbidden)
 *
 * The server understood the request, but is refusing to fulfill it.
 * Authorization will not help and the request SHOULD NOT be repeated.
 * If the request method was not HEAD and the server wishes to make public
 * why the request has not been fulfilled, it SHOULD describe the reason
 * for the refusal in the entity.
 * If the server does not wish to make this information available to
 * the client, the status code 404 (Not Found) can be used instead.
 *
 *
 *
 */
public class ResourceForbiddenException extends WebApplicationException {
    private static final long serialVersionUID = 1L;

    public ResourceForbiddenException(String string) {
        super(Response.status(Response.Status.FORBIDDEN).entity(string).type(
                MediaType.TEXT_PLAIN).build());
    }
}
