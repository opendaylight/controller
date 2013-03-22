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
 * Status Code 405 (Method Not Allowed)
 *
 * The method specified in the Request-Line is not allowed for the
 * resource identified by the Request-URI. The response MUST include
 * an Allow header containing a list of valid methods for the
 * requested resource.
 *
 *
 *
 */
public class MethodNotAllowedException extends WebApplicationException {
    private static final long serialVersionUID = 1L;

    public MethodNotAllowedException(String string) {
        super(Response.status(new MethodNotAllowed()).entity(string).type(
                MediaType.TEXT_PLAIN).build());
    }
}
