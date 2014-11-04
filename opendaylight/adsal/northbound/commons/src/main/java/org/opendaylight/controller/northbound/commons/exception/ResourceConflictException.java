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
 * Status Code 409 (Conflict)
 *
 * The request could not be completed due to a conflict with the
 * current state of the resource. This code is only allowed in
 * situations where it is expected that the user might be able to
 * resolve the conflict and resubmit the request. The response body
 * SHOULD include enough information for the user to recognize the
 * source of the conflict. Ideally, the response entity would include
 * enough information for the user or user agent to fix the problem;
 * however, that might not be possible and is not required.
 *
 *
 *
 */
public class ResourceConflictException extends WebApplicationException {
    private static final long serialVersionUID = 1L;

    public ResourceConflictException(String string) {
        super(Response.status(Response.Status.CONFLICT).entity(string).type(
                MediaType.TEXT_PLAIN).build());
    }
}
