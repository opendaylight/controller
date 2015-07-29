/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.rest.services;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.Encoded;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.opendaylight.controller.rest.common.NormalizedNodeContext;
import org.opendaylight.controller.rest.common.RestconfConstants;
import org.opendaylight.controller.sal.rest.api.Draft02;

/**
 * <b>Data resources</b>
 *
 * This interface represents the datastore resource type which are split to the
 * configuration and operation datastore.
 *
 * @see <a
 *      href="https://tools.ietf.org/html/draft-ietf-netconf-restconf-07#section-3.4"></a>
 *      Specification defines URL path as "{+restconf}/data"
 *
 *      TODO finish documentation
 *
 *      The URI hierarchy for the RESTCONF resources consists of an entry point
 *      container, 4 top-level resources, and 1 field.
 *      <ul>
 *      <li><b>/restconf</b> - {@link #getRoot()}
 *      <ul>
 *      <li><b>/config</b> - {@link #readConfigurationData(String)}
 *      {@link #updateConfigurationData(String, NormalizedNodeContext)}
 *      {@link #createConfigurationData(NormalizedNodeContext)}
 *      {@link #createConfigurationData(String, NormalizedNodeContext)}
 *      {@link #deleteConfigurationData(String)}
 *      <li><b>/operational</b> - {@link #readOperationalData(String)}
 *      </ul>
 *      </ul>
 */
@Path("/")
public interface RestconfServiceData {

    public static final String XML = RestconfConstants.XML;
    public static final String JSON = RestconfConstants.JSON;

    @GET
    @Path("/config/{identifier:.+}")
    @Produces({ Draft02.MediaTypes.DATA + JSON, Draft02.MediaTypes.DATA + XML, MediaType.APPLICATION_JSON,
            MediaType.APPLICATION_XML, MediaType.TEXT_XML })
    public NormalizedNodeContext readConfigurationData(@Encoded @PathParam("identifier") String identifier,
            @Context UriInfo uriInfo);

    @GET
    @Path("/operational/{identifier:.+}")
    @Produces({ Draft02.MediaTypes.DATA + JSON, Draft02.MediaTypes.DATA + XML, MediaType.APPLICATION_JSON,
            MediaType.APPLICATION_XML, MediaType.TEXT_XML })
    public NormalizedNodeContext readOperationalData(@Encoded @PathParam("identifier") String identifier,
            @Context UriInfo uriInfo);

    @PUT
    @Path("/config/{identifier:.+}")
    @Consumes({ Draft02.MediaTypes.DATA + JSON, Draft02.MediaTypes.DATA + XML, MediaType.APPLICATION_JSON,
            MediaType.APPLICATION_XML, MediaType.TEXT_XML })
    public Response updateConfigurationData(@Encoded @PathParam("identifier") String identifier,
            NormalizedNodeContext payload);

    /**
     * @param identifier
     * @param payload
     * @param uriInfo
     * @return
     */
    @POST
    @Path("/config/{identifier:.+}")
    @Consumes({ Draft02.MediaTypes.DATA + JSON, Draft02.MediaTypes.DATA + XML, MediaType.APPLICATION_JSON,
            MediaType.APPLICATION_XML, MediaType.TEXT_XML })
    public Response createConfigurationData(@Encoded @PathParam("identifier") String identifier,
            NormalizedNodeContext payload, @Context UriInfo uriInfo);

    @POST
    @Path("/config")
    @Consumes({ Draft02.MediaTypes.DATA + JSON, Draft02.MediaTypes.DATA + XML, MediaType.APPLICATION_JSON,
            MediaType.APPLICATION_XML, MediaType.TEXT_XML })
    public Response createConfigurationData(NormalizedNodeContext payload, @Context UriInfo uriInfo);

    @DELETE
    @Path("/config/{identifier:.+}")
    public Response deleteConfigurationData(@Encoded @PathParam("identifier") String identifier);
}
