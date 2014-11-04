package org.opendaylight.controller.northbound.commons.exception;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

@Provider
public class GenericExceptionMapper implements ExceptionMapper<Exception> {

    @Override
    public Response toResponse(Exception exception) {
        //check if WebApplicationException and reuse status code
        if (exception instanceof WebApplicationException) {
            WebApplicationException ex = (WebApplicationException) exception;
            return Response.status(ex.getResponse().getStatus()).
                    entity(ex.getResponse().getEntity()).build();
        }
        // throw 500 for all other errors
        return Response.status(Response.Status.INTERNAL_SERVER_ERROR).
                entity(exception.getMessage()).build();
    }

}
