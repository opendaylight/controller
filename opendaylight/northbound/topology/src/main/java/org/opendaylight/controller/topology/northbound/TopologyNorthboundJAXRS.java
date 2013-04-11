
/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.topology.northbound;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.xml.bind.JAXBElement;

import org.codehaus.enunciate.jaxrs.ResponseCode;
import org.codehaus.enunciate.jaxrs.StatusCodes;
import org.codehaus.enunciate.jaxrs.TypeHint;
import org.opendaylight.controller.northbound.commons.RestMessages;
import org.opendaylight.controller.northbound.commons.exception.InternalServerErrorException;
import org.opendaylight.controller.northbound.commons.exception.ResourceNotFoundException;
import org.opendaylight.controller.sal.core.Edge;
import org.opendaylight.controller.sal.core.Property;
import org.opendaylight.controller.sal.utils.ServiceHelper;
import org.opendaylight.controller.sal.utils.Status;
import org.opendaylight.controller.topologymanager.ITopologyManager;
import org.opendaylight.controller.topologymanager.TopologyUserLinkConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Topology Northbound REST API
 * 
 * <br><br>
 * Authentication scheme : <b>HTTP Basic</b><br>
 * Authentication realm : <b>opendaylight</b><br>
 * Transport : <b>HTTP and HTTPS</b><br>
 * <br>
 * HTTPS Authentication is disabled by default. Administrator can enable it in tomcat-server.xml after adding 
 * a proper keystore / SSL certificate from a trusted authority.<br>
 * More info : http://tomcat.apache.org/tomcat-7.0-doc/ssl-howto.html#Configuration
 */

@Path("/")
public class TopologyNorthboundJAXRS {
    private static Logger logger = LoggerFactory
            .getLogger(TopologyNorthboundJAXRS.class);

    /**
     *
     * Retrieve the Topology
     *
     * @param containerName The container for which we want to retrieve the topology
     *
     * @return A List of EdgeProps each EdgeProp represent an Edge of
     * the grap with the corresponding properties attached to it.
     */
    @Path("/{containerName}")
    @GET
    @Produces( { MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    @TypeHint(Topology.class)
    @StatusCodes( { @ResponseCode(code = 404, condition = "The Container Name passed was not found") })
    public Topology getTopology(
            @PathParam("containerName") String containerName) {
        ITopologyManager topologyManager = (ITopologyManager) ServiceHelper
                .getInstance(ITopologyManager.class, containerName, this);
        if (topologyManager == null) {
            throw new ResourceNotFoundException(RestMessages.NOCONTAINER
                    .toString());
        }

        Map<Edge, Set<Property>> topo = topologyManager.getEdges();
        if (topo != null) {
            List<EdgeProperties> res = new ArrayList<EdgeProperties>();
            for (Map.Entry<Edge, Set<Property>> entry : topo.entrySet()) {
                EdgeProperties el = new EdgeProperties(entry.getKey(), entry.getValue());
                res.add(el);
            }
            return new Topology(res);
        }

        return null;
    }

    /**
    * Retrieve the user configured links 
    *
    * @param containerName The container for which we want to retrieve the user links
    *
    * @return A List of user configured links
    */
   @Path("/{containerName}/userLink")
   @GET
   @Produces( { MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
   @TypeHint(TopologyUserLinks.class)
   @StatusCodes( { @ResponseCode(code = 404, condition = "The Container Name passed was not found") })
   public TopologyUserLinks getUserLinks(
           @PathParam("containerName") String containerName) {
       ITopologyManager topologyManager = (ITopologyManager) ServiceHelper
               .getInstance(ITopologyManager.class, containerName, this);
       if (topologyManager == null) {
           throw new ResourceNotFoundException(RestMessages.NOCONTAINER
                   .toString());
       }

       ConcurrentMap<String, TopologyUserLinkConfig> userLinks = topologyManager.getUserLinks();
       if ((userLinks != null) && (userLinks.values() != null)) {
    	   List<TopologyUserLinkConfig> res = new ArrayList<TopologyUserLinkConfig>(userLinks.values());
           return new TopologyUserLinks(res);
       }

       return null;
   }
   
   /**
    * Add an User Link
    *
    * @param containerName Name of the Container. The base Container is "default".
    * @param TopologyUserLinkConfig in JSON or XML format
    * @return Response as dictated by the HTTP Response Status code
    */

   @Path("/{containerName}/userLink")
   @POST
   @Consumes( { MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
   @StatusCodes( {
           @ResponseCode(code = 201, condition = "User Link added successfully"),
           @ResponseCode(code = 404, condition = "The Container Name passed was not found"),
           @ResponseCode(code = 409, condition = "Failed to add User Link due to Conflicting Name"),
           @ResponseCode(code = 500, condition = "Failed to add User Link. Failure Reason included in HTTP Error response"),
           @ResponseCode(code = 503, condition = "One or more of Controller services are unavailable") })
   public Response addUserLink(
           @PathParam(value = "containerName") String containerName,
           @TypeHint(TopologyUserLinkConfig.class) JAXBElement<TopologyUserLinkConfig> userLinkConfig) {

		ITopologyManager topologyManager = (ITopologyManager) ServiceHelper
				.getInstance(ITopologyManager.class, containerName, this);
		if (topologyManager == null) {
			throw new ResourceNotFoundException(RestMessages.NOCONTAINER
					.toString());
		}

		Status status = topologyManager.addUserLink(userLinkConfig.getValue());
		if (status.isSuccess()) {
			return Response.status(Response.Status.CREATED).build();
		}
		throw new InternalServerErrorException(status.getDescription());
   }

   /**
    * Delete an User Link
    *
    * @param containerName Name of the Container. The base Container is "default".
    * @param name Name of the Link Configuration
    * @return Response as dictated by the HTTP Response Status code
    */

   @Path("/{containerName}/userLink/{name}")
   @DELETE
   @Consumes( { MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
   @StatusCodes( {
   	       @ResponseCode(code = 200, condition = "Operation successful"),
           @ResponseCode(code = 404, condition = "The Container Name or Link Configuration Name was not found"),
           @ResponseCode(code = 503, condition = "One or more of Controller services are unavailable") })
   public Response deleteUserLink(
		   @PathParam("containerName") String containerName,
           @PathParam("name") String name) {

		ITopologyManager topologyManager = (ITopologyManager) ServiceHelper
				.getInstance(ITopologyManager.class, containerName, this);
	   if (topologyManager == null) {
		   throw new ResourceNotFoundException(RestMessages.NOCONTAINER
				   .toString());
	   }

       Status ret = topologyManager.deleteUserLink(name);
       if (ret.isSuccess()) {
           return Response.ok().build();
       }
       throw new ResourceNotFoundException(ret.getDescription());
   }
}
