/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.rest.doc.impl;

import org.opendaylight.controller.sal.rest.doc.api.ApiDocService;
import org.opendaylight.controller.sal.rest.doc.swagger.ApiDeclaration;
import org.opendaylight.controller.sal.rest.doc.swagger.ResourceList;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

/**
 * This service generates swagger
 * (See <a href="https://helloreverb.com/developers/swagger">https://helloreverb.com/developers/swagger</a>)
 * compliant documentation for RESTCONF APIs. The output of this is used by embedded Swagger UI.
 */
public class ApiDocServiceImpl implements ApiDocService {

  private static final ApiDocService INSTANCE = new ApiDocServiceImpl();

  public static ApiDocService getInstance(){
    return INSTANCE;
  }

  /**
   * Generates index document for Swagger UI. This document lists out all modules with link to get APIs for
   * each module. The API for each module is served by <code> getDocByModule()</code> method.
   *
   * @param uriInfo
   * @return
   */
  @Override
  public Response getRootDoc(UriInfo uriInfo) {

    ApiDocGenerator generator = ApiDocGenerator.getInstance();
    ResourceList rootDoc = generator.getResourceListing(uriInfo);

    return Response.ok(rootDoc).build();
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
  public Response getDocByModule(String module, String revision, UriInfo uriInfo) {
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
  public Response getApiExplorer(UriInfo uriInfo) {
    return Response.seeOther(uriInfo.getBaseUriBuilder().path("../explorer/index.html").build()).build();
  }
}
