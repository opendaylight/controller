package org.opendaylight.controller.sal.rest.impl;

import com.google.common.base.Optional;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.UriInfo;
import org.opendaylight.controller.sal.rest.api.RestconfConstants;
import org.opendaylight.controller.sal.restconf.impl.ControllerContext;
import org.opendaylight.controller.sal.restconf.impl.InstanceIdentifierContext;

public class AbstractIdentifierAwareJaxRsProvider {

    @Context
    private UriInfo uriInfo;

    protected final String getIdentifier() {
        return uriInfo.getPathParameters().getFirst(RestconfConstants.IDENTIFIER);
    }

    protected final Optional<InstanceIdentifierContext> getIdentifierWithSchema() {
        return Optional.of(getInstanceIdentifierContext());
    }

    protected InstanceIdentifierContext getInstanceIdentifierContext() {
        return ControllerContext.getInstance().buildIdentifier(getIdentifier());
    }

    protected UriInfo getUriInfo() {
        return uriInfo;
    }
}
