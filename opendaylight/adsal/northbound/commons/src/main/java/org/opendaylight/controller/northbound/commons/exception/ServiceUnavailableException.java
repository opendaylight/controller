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
 * Status Code 503 (Service Unavailable Error)
 *
 * The server is currently unable to handle the request due to a temporary
 * overloading or maintenance of the server.
 * The implication is that this is a temporary condition which will be alleviated
 * after some delay.
 *
 *
 */
public class ServiceUnavailableException extends WebApplicationException {
    private static final long serialVersionUID = 1L;

    /**
     * Constructor for the SERVICE_UNAVAILABLE custom handler
     *
     * @param string Error message to specify further the
     * SERVICE_UNAVAILABLE response
     *
     */
    public ServiceUnavailableException(String string) {
        super(Response.status(Response.Status.SERVICE_UNAVAILABLE).entity(
                string).type(MediaType.TEXT_PLAIN).build());
    }
}
