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
 * Status Code 404 (Not Found)
 *
 * The server has not found anything matching the Request-URI.
 * No indication is given of whether the condition is temporary or permanent.
 * The 410 (Gone) status code SHOULD be used if the server knows,
 * through some internally configurable mechanism, that an old resource
 * is permanently unavailable and has no forwarding address.
 * This status code is commonly used when the server does not wish to
 * reveal exactly why the request has been refused, or when no other
 * response is applicable.
 *
 *
 *
 */
public class ResourceNotFoundException extends WebApplicationException {
    private static final long serialVersionUID = 1L;

    public ResourceNotFoundException(String string) {
        super(Response.status(Response.Status.NOT_FOUND).entity(string).type(
                MediaType.TEXT_PLAIN).build());
    }
}
