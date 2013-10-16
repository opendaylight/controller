/**
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.northbound.commons;

import javax.ws.rs.Consumes;
import javax.ws.rs.core.GenericEntity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import org.codehaus.jackson.JsonProcessingException;

/**
 * A custom exception mapper for handling Jackson JsonProcessingException types
 */
@Provider
@Consumes({MediaType.APPLICATION_JSON})
public class JacksonJsonProcessingExceptionMapper
    implements ExceptionMapper<JsonProcessingException>
{

    @Override
    public Response toResponse(JsonProcessingException exception) {
        GenericEntity<String> entity =
                new GenericEntity<String>(exception.getMessage()) {};
        return Response.status(Response.Status.BAD_REQUEST).entity(entity).build();
    }
}

