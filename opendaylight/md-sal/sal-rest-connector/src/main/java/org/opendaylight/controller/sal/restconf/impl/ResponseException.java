/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.restconf.impl;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

public class ResponseException extends WebApplicationException {

    private static final long serialVersionUID = -5320114450593021655L;

    public ResponseException(Status status, String msg) {
        super(Response.status(status).type(MediaType.TEXT_PLAIN_TYPE).entity(msg).build());
    }
}
