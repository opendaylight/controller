/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.rest.api;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
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
import org.opendaylight.controller.sal.restconf.impl.NormalizedNodeContext;
import org.opendaylight.controller.sal.restconf.impl.StructuredData;
import org.opendaylight.yangtools.yang.data.api.CompositeNode;
import org.opendaylight.yangtools.yang.data.api.Node;


/**
 * The URI hierarchy for the RESTCONF resources consists of an entry point container, 4 top-level resources, and 1
 * field.
 * <ul>
 * <li><b>/restconf</b> - {@link #getRoot()}
 * <ul>
 *      <li><b>/config</b> - {@link #readConfigurationData(String)}
 *                              {@link #updateConfigurationData(String, CompositeNode)}
 *                              {@link #createConfigurationData(CompositeNode)}
 *                              {@link #createConfigurationData(String, CompositeNode)}
 * {@link #deleteConfigurationData(String)}
 * <li><b>/operational</b> - {@link #readOperationalData(String)}
 * <li>/modules - {@link #getModules()}
 * <ul>
 * <li>/module
 * </ul>
 *      <li><b>/operations</b> - {@link #invokeRpc(String, CompositeNode)}
 *                               {@link #invokeRpc(String, CompositeNode)}
 * <li>/version (field)
 * </ul>
 * </ul>
 */
@Path("/")
public interface RestconfService {

    public static final String XML = "+xml";
    public static final String JSON = "+json";

    @GET
    public Object getRoot();

    @GET
    @Path("/modules")
    @Produces({ Draft02.MediaTypes.API + JSON, Draft02.MediaTypes.API + XML, MediaType.APPLICATION_JSON,
            MediaType.APPLICATION_XML, MediaType.TEXT_XML })
    public NormalizedNodeContext getModules(@Context UriInfo uriInfo);

    @GET
    @Path("/modules/{identifier:.+}")
    @Produces({ Draft02.MediaTypes.API + JSON, Draft02.MediaTypes.API + XML, MediaType.APPLICATION_JSON,
            MediaType.APPLICATION_XML, MediaType.TEXT_XML })
    public NormalizedNodeContext getModules(@PathParam("identifier") String identifier, @Context UriInfo uriInfo);

    @GET
    @Path("/modules/module/{identifier:.+}")
    @Produces({ Draft02.MediaTypes.API + JSON, Draft02.MediaTypes.API + XML, MediaType.APPLICATION_JSON,
            MediaType.APPLICATION_XML, MediaType.TEXT_XML })
    public NormalizedNodeContext getModule(@PathParam("identifier") String identifier, @Context UriInfo uriInfo);

    @GET
    @Path("/operations")
    @Produces({ Draft02.MediaTypes.API + JSON, Draft02.MediaTypes.API + XML, MediaType.APPLICATION_JSON,
            MediaType.APPLICATION_XML, MediaType.TEXT_XML })
    public NormalizedNodeContext getOperations(@Context UriInfo uriInfo);

    @GET
    @Path("/operations/{identifier:.+}")
    @Produces({ Draft02.MediaTypes.API + JSON, Draft02.MediaTypes.API + XML, MediaType.APPLICATION_JSON,
            MediaType.APPLICATION_XML, MediaType.TEXT_XML })
    public NormalizedNodeContext getOperations(@PathParam("identifier") String identifier, @Context UriInfo uriInfo);

    @POST
    @Path("/operations/{identifier:.+}")
    @Produces({ Draft02.MediaTypes.OPERATION + JSON, Draft02.MediaTypes.OPERATION + XML,
            Draft02.MediaTypes.DATA + JSON, Draft02.MediaTypes.DATA + XML, MediaType.APPLICATION_JSON,
            MediaType.APPLICATION_XML, MediaType.TEXT_XML })
    @Consumes({ Draft02.MediaTypes.OPERATION + JSON, Draft02.MediaTypes.OPERATION + XML,
            Draft02.MediaTypes.DATA + JSON, Draft02.MediaTypes.DATA + XML, MediaType.APPLICATION_JSON,
            MediaType.APPLICATION_XML, MediaType.TEXT_XML })
    public StructuredData invokeRpc(@Encoded @PathParam("identifier") String identifier, CompositeNode payload,
            @Context UriInfo uriInfo);

    @POST
    @Path("/operations/{identifier:.+}")
    @Produces({ Draft02.MediaTypes.OPERATION + JSON, Draft02.MediaTypes.OPERATION + XML,
            Draft02.MediaTypes.DATA + JSON, Draft02.MediaTypes.DATA + XML, MediaType.APPLICATION_JSON,
            MediaType.APPLICATION_XML, MediaType.TEXT_XML })
    public StructuredData invokeRpc(@Encoded @PathParam("identifier") String identifier,
            @DefaultValue("") String noPayload, @Context UriInfo uriInfo);

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
    public Response updateConfigurationData(@Encoded @PathParam("identifier") String identifier, Node<?> payload);

    @POST
    @Path("/config/{identifier:.+}")
    @Consumes({ Draft02.MediaTypes.DATA + JSON, Draft02.MediaTypes.DATA + XML, MediaType.APPLICATION_JSON,
            MediaType.APPLICATION_XML, MediaType.TEXT_XML })
    public Response createConfigurationData(@Encoded @PathParam("identifier") String identifier, Node<?> payload);

    @POST
    @Path("/config")
    @Consumes({ Draft02.MediaTypes.DATA + JSON, Draft02.MediaTypes.DATA + XML, MediaType.APPLICATION_JSON,
            MediaType.APPLICATION_XML, MediaType.TEXT_XML })
    public Response createConfigurationData(Node<?> payload);

    @DELETE
    @Path("/config/{identifier:.+}")
    public Response deleteConfigurationData(@Encoded @PathParam("identifier") String identifier);

    @GET
    @Path("/streams/stream/{identifier:.+}")
    public Response subscribeToStream(@Encoded @PathParam("identifier") String identifier, @Context UriInfo uriInfo);

    @GET
    @Path("/streams")
    @Produces({ Draft02.MediaTypes.API + JSON, Draft02.MediaTypes.API + XML, MediaType.APPLICATION_JSON,
            MediaType.APPLICATION_XML, MediaType.TEXT_XML })
    public NormalizedNodeContext getAvailableStreams(@Context UriInfo uriInfo);
}
