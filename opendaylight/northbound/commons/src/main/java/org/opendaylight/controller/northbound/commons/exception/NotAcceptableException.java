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
 * Status Code 406 (Not Acceptable)
 *
 * The resource identified by the request is only capable of
 * generating response entities which have content characteristics not
 * acceptable according to the accept headers sent in the request.
 *
 *
 *
 */
public class NotAcceptableException extends WebApplicationException {
    private static final long serialVersionUID = 1L;

    public NotAcceptableException(String string) {
        super(Response.status(Response.Status.NOT_ACCEPTABLE).entity(string)
                .type(MediaType.TEXT_PLAIN).build());
    }
}
