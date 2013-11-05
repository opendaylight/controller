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

import org.opendaylight.controller.sal.restconf.impl.StructuredData;
import org.opendaylight.yangtools.yang.data.api.CompositeNode;

/**
 *   The URI hierarchy for the RESTCONF resources consists of an entry
 *   point container, 3 top-level resources, and 1 field.  Refer to
 *  Section 5 for details on each URI.
 *    <ul>
 *    <li><b>/restconf</b> - {@link #getRoot()}
 *     <ul><li><b>/datastore</b> - {@link #readAllData()}
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
@Path("restconf")
public interface RestconfService {

    public static final String XML = "+xml";
    public static final String JSON = "+json";

    @GET
    public Object getRoot();

    @GET
    @Path("/datastore")
    @Produces({API+JSON,API+XML})
    public Object readAllData();

    @GET
    @Path("/datastore/{identifier}")
    @Produces({API+JSON,API+XML})
    public StructuredData readData(@PathParam("identifier") String identifier);

    @PUT
    @Path("/datastore/{identifier}")
    @Produces({API+JSON,API+XML})
    public Object createConfigurationData(@PathParam("identifier") String identifier, CompositeNode payload);

    @POST
    @Path("/datastore/{identifier}")
    @Produces({API+JSON,API+XML})
    public Object updateConfigurationData(@PathParam("identifier") String identifier, CompositeNode payload);

    @GET
    @Path("/modules")
    @Produces({API+JSON,API+XML})
    public Object getModules();

    @POST
    @Path("/operations/{identifier}")
    @Produces({API+JSON,API+XML})
    public StructuredData invokeRpc(@PathParam("identifier") String identifier, CompositeNode payload);
}
