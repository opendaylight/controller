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
 * Status Code 501 (Not Implemented)
 *
 * The server does not support the functionality required to fulfill the
 * request. This is the appropriate response when the server does not recognize
 * the request method and is not capable of supporting it for any resource.
 */
public class NotImplementedException extends WebApplicationException {
    private static final long serialVersionUID = 1L;

    public NotImplementedException(String string) {
        super(Response.status(new NotImplemented()).entity(string).type(MediaType.TEXT_PLAIN).build());
    }
}
