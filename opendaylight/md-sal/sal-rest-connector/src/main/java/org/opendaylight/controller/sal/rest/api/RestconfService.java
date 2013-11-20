/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.rest.api;

import static org.opendaylight.controller.sal.restconf.impl.MediaTypes.API;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

import org.opendaylight.controller.sal.restconf.impl.StructuredData;
import org.opendaylight.yangtools.yang.data.api.CompositeNode;

/**
 *   The URI hierarchy for the RESTCONF resources consists of an entry
 *   point container, 3 top-level resources, and 1 field.  Refer to
 *  Section 5 for details on each URI.
 *    <ul>
 *    <li><b>/restconf</b> - {@link #getRoot()}
 *     <ul><li><b>/config</b> 
 *         <li><b>/operational</b> - {@link #readAllData()} - Added in Draft02
 *         <li><b>/datastore</b> - {@link #readAllData()}
 *         <ul>
 *            <li>/(top-level-data-nodes) (config=true or false)
 *         </ul>
 *         <li>/modules
 *          <ul><li>/module
 *              <li>/name
 *              <li>/revision
 *              <li>/namespace
 *              <li>/feature
 *             <li>/deviation
 *          </ul>
 *          <li>/operations
 *          <ul>
 *             <li>/(custom protocol operations)
 *          </ul>
 *         <li>/version (field)
 *     </ul>
 */
@Path("/")
public interface RestconfService extends RestconfServiceLegacy {

    public static final String XML = "+xml";
    public static final String JSON = "+json";

    @GET
    public Object getRoot();

    @GET
    @Path("/modules")
    @Produces({API+JSON,API+XML})
    public StructuredData getModules();

    @POST
    @Path("/operations/{identifier}")
    @Produces({Draft02.MediaTypes.API+JSON,Draft02.MediaTypes.API+XML,API+JSON,API+XML})
    public StructuredData invokeRpc(@PathParam("identifier") String identifier, CompositeNode payload);
    
    @GET
    @Path("/config/{identifier:.+}")
    @Produces({Draft02.MediaTypes.DATA+JSON,Draft02.MediaTypes.DATA+XML})
    public StructuredData readConfigurationData(@PathParam("identifier") String identifier);
    
    @PUT
    @Path("/config/{identifier:.+}")
    @Produces({API+JSON,API+XML})
    public Response createConfigurationData(@PathParam("identifier") String identifier, CompositeNode payload);

    @POST
    @Path("/config/{identifier:.+}")
    @Produces({API+JSON,API+XML})
    public Response updateConfigurationData(@PathParam("identifier") String identifier, CompositeNode payload);

    @GET
    @Path("/operational/{identifier:.+}")
    @Produces({Draft02.MediaTypes.DATA+JSON,Draft02.MediaTypes.DATA+XML})
    public StructuredData readOperationalData(@PathParam("identifier") String identifier);

    @PUT
    @Path("/operational/{identifier:.+}")
    @Produces({API+JSON,API+XML})
    public Response createOperationalData(@PathParam("identifier") String identifier, CompositeNode payload);

    @POST
    @Path("/operational/{identifier:.+}")
    @Produces({API+JSON,API+XML})
    public Response updateOperationalData(@PathParam("identifier") String identifier, CompositeNode payload);

}
