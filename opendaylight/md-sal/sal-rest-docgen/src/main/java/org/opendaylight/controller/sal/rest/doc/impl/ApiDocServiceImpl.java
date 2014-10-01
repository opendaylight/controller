/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.rest.doc.impl;

import java.io.ByteArrayOutputStream;
import java.io.OutputStreamWriter;
import java.util.Map.Entry;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import org.json.JSONWriter;
import org.opendaylight.controller.sal.rest.doc.api.ApiDocService;
import org.opendaylight.controller.sal.rest.doc.mountpoints.MountPointSwagger;
import org.opendaylight.controller.sal.rest.doc.swagger.ApiDeclaration;
import org.opendaylight.controller.sal.rest.doc.swagger.ResourceList;

/**
 * This service generates swagger (See <a
 * href="https://helloreverb.com/developers/swagger"
 * >https://helloreverb.com/developers/swagger</a>) compliant documentation for
 * RESTCONF APIs. The output of this is used by embedded Swagger UI.
 *
 * NOTE: These API's need to be synchronized due to bug 1198. Thread access to
 * the SchemaContext is not synchronized properly and thus you can end up with
 * missing definitions without this synchronization. There are likely otherways
 * to work around this limitation, but given that this API is a dev only tool
 * and not dependent UI, this was the fastest work around.
 *
 */
public class ApiDocServiceImpl implements ApiDocService {

    private static final ApiDocService INSTANCE = new ApiDocServiceImpl();

    public static ApiDocService getInstance() {
        return INSTANCE;
    }

    /**
     * Generates index document for Swagger UI. This document lists out all
     * modules with link to get APIs for each module. The API for each module is
     * served by <code> getDocByModule()</code> method.
     *
     * @param uriInfo
     * @return
     */
    @Override
    public synchronized Response getRootDoc(UriInfo uriInfo) {
        ApiDocGenerator generator = ApiDocGenerator.getInstance();
        ResourceList rootDoc = generator.getResourceListing(uriInfo);
        return Response.ok(rootDoc).build();
    }

    @Override
    public synchronized Response getRootDocWithOneModule(UriInfo uriInfo, String moduleName, String revision) {
        ApiDocGenerator generator = ApiDocGenerator.getInstance();
        ResourceList rootDoc = generator.getResourceListing(uriInfo, moduleName, revision);
        return Response.ok(rootDoc).build();
    }

    @Override
    public Response getListOfRestAwareModules(UriInfo uriInfo) {
        ApiDocGenerator generator = ApiDocGenerator.getInstance();
        final String html = HTMLGeneratorForRestModules.generate(uriInfo.getBaseUriBuilder(),
                generator.getListOfModulesWithRestLinks());
        return Response.ok(html, MediaType.TEXT_HTML_TYPE).build();
    }

    /**
     * Generates Swagger compliant document listing APIs for module.
     *
     * @param module
     * @param revision
     * @param uriInfo
     * @return
     */
    @Override
    public synchronized Response getDocByModule(String module, String revision, UriInfo uriInfo) {
        ApiDocGenerator generator = ApiDocGenerator.getInstance();

        ApiDeclaration doc = generator.getApiDeclaration(module, revision, uriInfo);
        return Response.ok(doc).build();
    }

    /**
     * Redirects to embedded swagger ui.
     *
     * @param uriInfo
     * @return
     */
    @Override
    public synchronized Response getApiExplorer(UriInfo uriInfo) {
        return Response
                .seeOther(uriInfo.getBaseUriBuilder().path("../explorer/index.html").build())
                .build();
    }

    @Override
    public synchronized Response getListOfMounts(UriInfo uriInfo) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (OutputStreamWriter streamWriter = new OutputStreamWriter(baos)) {
            JSONWriter writer = new JSONWriter(streamWriter);
            writer.array();
            for (Entry<String, Long> entry : MountPointSwagger.getInstance()
                    .getInstanceIdentifiers().entrySet()) {
                writer.object();
                writer.key("instance").value(entry.getKey());
                writer.key("id").value(entry.getValue());
                writer.endObject();
            }
            writer.endArray();
        } catch (Exception e) {
            return Response.status(500).entity(e.getMessage()).build();
        }
        return Response.status(200).entity(baos.toString()).build();
    }

    @Override
    public synchronized Response getMountRootDoc(String instanceNum, UriInfo uriInfo) {
        ResourceList resourceList = MountPointSwagger.getInstance().getResourceList(uriInfo,
                Long.parseLong(instanceNum));
        return Response.ok(resourceList).build();
    }

    @Override
    public synchronized Response getMountDocByModule(String instanceNum, String module,
            String revision, UriInfo uriInfo) {
        ApiDeclaration api = MountPointSwagger.getInstance().getMountPointApi(uriInfo,
                Long.parseLong(instanceNum), module, revision);
        return Response.ok(api).build();
    }

}
