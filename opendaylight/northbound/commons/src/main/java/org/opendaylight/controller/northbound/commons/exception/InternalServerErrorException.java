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
 * Status Code 500 (Internal Server Error)
 *
 * The server encountered an unexpected condition which prevented
 * it from fulfilling the request.
 *
 *
 *
 */
public class InternalServerErrorException extends WebApplicationException {
    private static final long serialVersionUID = 1L;

    /**
     * Constructor for the INTERNAL_SERVER_ERROR custom handler
     *
     * @param string Error message to specify further the
     * INTERNAL_SERVER_ERROR response
     *
     */
    public InternalServerErrorException(String string) {
        super(Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(
                string).type(MediaType.TEXT_PLAIN).build());
    }
}
