package org.opendaylight.controller.sal.rest.impl;

import javax.ws.rs.core.Context;
import javax.ws.rs.core.UriInfo;

import org.opendaylight.controller.sal.rest.api.RestconfConstants;
import org.opendaylight.controller.sal.restconf.impl.InstanceIdWithSchemaNode;
import org.opendaylight.controller.sal.restconf.impl.RestconfIdentifierCodecImpl;

import com.google.common.base.Optional;


public class AbstractIdentifierAwareJaxRsProvider {

    @Context
    private UriInfo uriInfo;

    @Context
    private RestconfIdentifierCodecImpl iiCodec;

    protected final String getIdentifier() {
        return uriInfo.getPathParameters().getFirst(RestconfConstants.IDENTIFIER);
    }


    protected final Optional<InstanceIdWithSchemaNode> getIdentifierWithSchema() {
        String stringIdentifier = getIdentifier();
        if(iiCodec != null && uriInfo != null) {
            return Optional.of(iiCodec.fromString(stringIdentifier));
        }
        return Optional.absent();
    }
}
