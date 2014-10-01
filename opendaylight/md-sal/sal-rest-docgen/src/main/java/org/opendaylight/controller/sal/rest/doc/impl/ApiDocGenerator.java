/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.rest.doc.impl;

import com.google.common.base.Preconditions;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.ws.rs.core.UriInfo;
import org.opendaylight.controller.sal.core.api.model.SchemaService;
import org.opendaylight.controller.sal.rest.doc.swagger.ApiDeclaration;
import org.opendaylight.controller.sal.rest.doc.swagger.ResourceList;
import org.opendaylight.yangtools.yang.model.api.ContainerSchemaNode;
import org.opendaylight.yangtools.yang.model.api.DataSchemaNode;
import org.opendaylight.yangtools.yang.model.api.ListSchemaNode;
import org.opendaylight.yangtools.yang.model.api.Module;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class gathers all yang defined {@link Module}s and generates Swagger compliant documentation.
 */
public class ApiDocGenerator extends BaseYangSwaggerGenerator {

    private static Logger _logger = LoggerFactory.getLogger(ApiDocGenerator.class);

    private static final ApiDocGenerator INSTANCE = new ApiDocGenerator();
    private SchemaService schemaService;

    /**
     * Returns resources for swagger display for all modules which has at least one rpc
     * or list|container as top element.
     */
    public ResourceList getResourceListing(UriInfo uriInfo) {
        final SchemaContext schemaContext = resolveSchemaContext();
        return super.getResourceListing(uriInfo, schemaContext, "", getSortedModules(schemaContext));
    }

    public ApiDeclaration getApiDeclaration(String module, String revision, UriInfo uriInfo) {
        SchemaContext schemaContext = schemaService.getGlobalContext();
        Preconditions.checkState(schemaContext != null);
        return super.getApiDeclaration(module, revision, uriInfo, schemaContext, "");
    }

    /**
     * Returns resource for swagger display for one specified module.
     */
    public ResourceList getResourceListing(UriInfo uriInfo, String moduleName, String revision) {
        final SchemaContext schemaContext = resolveSchemaContext();
        final Module module = findModuleByNameAndRevision(moduleName, revision, schemaContext);
        return super.getResourceListing(uriInfo, schemaContext, "", Collections.singleton(module));
    }

    private SchemaContext resolveSchemaContext() {
        Preconditions.checkState(schemaService != null);
        SchemaContext schemaContext = schemaService.getGlobalContext();
        Preconditions.checkState(schemaContext != null);
        return schemaContext;
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

    /**
     * Returns just list of modules which have rpc or list|container as top level.
     *
     * No other operation is done to be as fast as possible.
     * @return
     */
    public List<Module> getListOfModulesWithRestLinks() {
        final SchemaContext schemaContext = resolveSchemaContext();
        List<Module> modules = new ArrayList<>();
        for (Module module : getSortedModules(schemaContext)) {
            if (!module.getRpcs().isEmpty()) {
                modules.add(module);
                continue;
            }
            for (DataSchemaNode child : module.getChildNodes()) {
                if (child instanceof ListSchemaNode || child instanceof ContainerSchemaNode) {
                    modules.add(module);
                    break;
                }
            }
        }
        return modules;
    }



}
