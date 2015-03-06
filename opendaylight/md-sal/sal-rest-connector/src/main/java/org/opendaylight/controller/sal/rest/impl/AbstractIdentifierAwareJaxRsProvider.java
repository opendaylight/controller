package org.opendaylight.controller.sal.rest.impl;

import com.google.common.base.Optional;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.UriInfo;
import org.opendaylight.controller.sal.rest.api.RestconfConstants;
import org.opendaylight.controller.sal.restconf.impl.ControllerContext;
import org.opendaylight.controller.sal.restconf.impl.InstanceIdentifierContext;
import org.opendaylight.yangtools.yang.model.api.RpcDefinition;

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
        final String ident = getIdentifier();
        // FIXME make InstanceIdentifierFactory in better way for 3 kind of ident parsing strategy
        // 1) base path 2) RPC path 3) mountpoint (base / rpc) - try to reuse same code
        if (ident.contains(ControllerContext.MOUNT)) {
            final InstanceIdentifierContext mountPointContext =
                    ControllerContext.getInstance().toMountPointIdentifier(ident);
            if (mountPointContext.getSchemaNode() instanceof RpcDefinition) {
                return mountPointContext;
            }
        }
        return ControllerContext.getInstance().toInstanceIdentifier(ident);
    }

    protected UriInfo getUriInfo() {
        return uriInfo;
    }
}
