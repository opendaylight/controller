/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.rest.providers;

import javax.ws.rs.core.Context;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.core.UriInfo;
import org.opendaylight.controller.rest.common.InstanceIdentifierContext;
import org.opendaylight.controller.rest.common.RestconfConstants;
import org.opendaylight.controller.rest.connector.RestSchemaController;

public class AbstractIdentifierAwareJaxRsProvider {

    private static final String POST = "POST";
    private final RestSchemaController restSchemaCx;

    public AbstractIdentifierAwareJaxRsProvider(final RestSchemaController restSchemaCx) {
        this.restSchemaCx = restSchemaCx;
    }

    @Context
    private UriInfo uriInfo;

    @Context
    private Request request;

    @Context
    private SecurityContext securityCx;

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
