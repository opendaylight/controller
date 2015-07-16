package org.opendaylight.controller.rest.providers;

import javax.ws.rs.core.Context;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.UriInfo;
import org.opendaylight.controller.rest.common.InstanceIdentifierContext;
import org.opendaylight.controller.rest.common.RestconfConstants;
import org.opendaylight.controller.rest.connector.RestSchemaContext;

public class AbstractIdentifierAwareJaxRsProvider {

    private static final String POST = "POST";
    private final RestSchemaContext restSchemaCx;

    public AbstractIdentifierAwareJaxRsProvider(final RestSchemaContext restSchemaCx) {
        this.restSchemaCx = restSchemaCx;
    }

    @Context
    private UriInfo uriInfo;

    @Context
    private Request request;

    protected final String getIdentifier() {
        return uriInfo.getPathParameters(false).getFirst(RestconfConstants.IDENTIFIER);
    }

    protected InstanceIdentifierContext<?> getInstanceIdentifierContext() {
        return restSchemaCx.toInstanceIdentifier(getIdentifier());
    }

    protected UriInfo getUriInfo() {
        return uriInfo;
    }

    protected boolean isPost() {
        return POST.equals(request.getMethod());
    }
}
