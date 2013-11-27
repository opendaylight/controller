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
