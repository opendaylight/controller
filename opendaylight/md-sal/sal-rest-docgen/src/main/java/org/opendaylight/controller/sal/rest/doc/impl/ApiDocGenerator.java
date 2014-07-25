/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.rest.doc.impl;

import javax.ws.rs.core.UriInfo;

import org.opendaylight.controller.sal.core.api.model.SchemaService;
import org.opendaylight.controller.sal.rest.doc.swagger.ApiDeclaration;
import org.opendaylight.controller.sal.rest.doc.swagger.ResourceList;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;

/**
 * This class gathers all yang defined {@link Module}s and generates Swagger
 * compliant documentation.
 */
public class ApiDocGenerator extends BaseYangSwaggerGenerator {

    private static Logger _logger = LoggerFactory.getLogger(ApiDocGenerator.class);

    private static final ApiDocGenerator INSTANCE = new ApiDocGenerator();
    private SchemaService schemaService;

    public ResourceList getResourceListing(UriInfo uriInfo) {
        Preconditions.checkState(schemaService != null);
        SchemaContext schemaContext = schemaService.getGlobalContext();
        Preconditions.checkState(schemaContext != null);
        return super.getResourceListing(uriInfo, schemaContext, "");
    }

    public ApiDeclaration getApiDeclaration(String module, String revision, UriInfo uriInfo) {
        SchemaContext schemaContext = schemaService.getGlobalContext();
        Preconditions.checkState(schemaContext != null);
        return super.getApiDeclaration(module, revision, uriInfo, schemaContext, "");
    }

    /**
     * Returns singleton instance
     *
     * @return
     */
    public static ApiDocGenerator getInstance() {
        return INSTANCE;
    }

    /**
     *
     * @param schemaService
     */
    public void setSchemaService(SchemaService schemaService) {
        this.schemaService = schemaService;
    }
}
