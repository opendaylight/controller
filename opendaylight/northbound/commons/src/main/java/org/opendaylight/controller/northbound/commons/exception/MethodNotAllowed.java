/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.northbound.commons.exception;

import javax.ws.rs.core.Response;

/**
 * Implementation of StatusType for error 405 as in:
 * http://www.w3.org/Protocols/rfc2616/rfc2616-sec10.html#sec10.4.6
 *
 *
 */
public class MethodNotAllowed implements Response.StatusType {
    @Override
    public int getStatusCode() {
        return 405;
    }

    @Override
    public String getReasonPhrase() {
        return "Method Not Allowed";
    }

    @Override
    public Response.Status.Family getFamily() {
        return Response.Status.Family.CLIENT_ERROR;
    }
}
