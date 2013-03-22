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
 * Status Code 410 (Gone)
 *
 * The requested resource is no longer available at the server and no
 * forwarding address is known. This condition is expected to be
 * considered permanent. Clients with link editing capabilities SHOULD
 * delete references to the Request-URI after user approval. If the
 * server does not know, or has no facility to determine, whether or
 * not the condition is permanent, the status code 404 (Not Found)
 * SHOULD be used instead. This response is cacheable unless indicated
 * otherwise.
 *
 *
 *
 */
public class ResourceGoneException extends WebApplicationException {
    private static final long serialVersionUID = 1L;

    public ResourceGoneException(String string) {
        super(Response.status(Response.Status.GONE).entity(string).type(
                MediaType.TEXT_PLAIN).build());
    }
}
