package org.opendaylight.controller.rest.connector.impl;

import java.io.IOException;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.core.MultivaluedMap;

public class RestconfCORSFilter implements ContainerResponseFilter {

    @Override
    public void filter(final ContainerRequestContext containerRequestContext,
            final ContainerResponseContext containerResponseContext) throws IOException {
        final MultivaluedMap<String, Object> headers = containerResponseContext.getHeaders();
        headers.add("Access-Control-Allow-Origin", "*");
        headers.add("Access-Control-Allow-Methods", "GET, POST, OPTIONS, DELETE, PUT, HEAD");
        headers.add("Access-Control-Allow-Headers", "origin, content-type, accept, authorization");
        headers.add("Access-Control-Expose-Headers", "location");
    }
}