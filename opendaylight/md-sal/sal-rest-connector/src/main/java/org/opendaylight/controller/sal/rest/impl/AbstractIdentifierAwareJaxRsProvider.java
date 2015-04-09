package org.opendaylight.controller.sal.rest.impl;

import javax.ws.rs.core.Context;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.UriInfo;
import org.opendaylight.controller.sal.rest.api.RestconfConstants;
import org.opendaylight.controller.sal.restconf.impl.ControllerContext;
import org.opendaylight.controller.sal.restconf.impl.InstanceIdentifierContext;

public class AbstractIdentifierAwareJaxRsProvider {

    private static final String POST = "POST";

    @Context
    private UriInfo uriInfo;

    @Context
    private Request request;

    protected final String getIdentifier() {
        return uriInfo.getPathParameters(false).getFirst(RestconfConstants.IDENTIFIER);
    }

    protected InstanceIdentifierContext<?> getInstanceIdentifierContext() {
        return ControllerContext.getInstance().toInstanceIdentifier(getIdentifier());
    }

    protected UriInfo getUriInfo() {
        return uriInfo;
    }

    protected boolean isPost() {
        return POST.equals(request.getMethod());
    }
}
