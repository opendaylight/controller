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
 * Status Code 415 (Unsupported Media Type)
 *
 * The server is refusing to service the request because the entity of
 * the request is in a format not supported by the requested resource
 * for the requested method.
 *
 *
 *
 */
public class UnsupportedMediaTypeException extends WebApplicationException {
    private static final long serialVersionUID = 1L;

    public UnsupportedMediaTypeException(String string) {
        super(Response.status(Response.Status.UNSUPPORTED_MEDIA_TYPE).entity(
                string).type(MediaType.TEXT_PLAIN).build());
    }
}
