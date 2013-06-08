/*
 * Copyright IBM Corporation, 2013.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.samples.loadbalancer.northbound;

import java.util.List;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.xml.bind.JAXBElement;

import org.codehaus.enunciate.jaxrs.ResponseCode;
import org.codehaus.enunciate.jaxrs.StatusCodes;
import org.codehaus.enunciate.jaxrs.TypeHint;
import org.opendaylight.controller.containermanager.IContainerManager;
import org.opendaylight.controller.samples.loadbalancer.entities.Pool;
import org.opendaylight.controller.samples.loadbalancer.entities.PoolMember;
import org.opendaylight.controller.samples.loadbalancer.entities.VIP;
import org.opendaylight.controller.northbound.commons.RestMessages;
import org.opendaylight.controller.northbound.commons.exception.InternalServerErrorException;
import org.opendaylight.controller.northbound.commons.exception.MethodNotAllowedException;
import org.opendaylight.controller.northbound.commons.exception.ResourceConflictException;
import org.opendaylight.controller.northbound.commons.exception.ResourceNotFoundException;
import org.opendaylight.controller.northbound.commons.exception.ServiceUnavailableException;
import org.opendaylight.controller.northbound.commons.exception.UnsupportedMediaTypeException;
import org.opendaylight.controller.sal.utils.ServiceHelper;
import org.opendaylight.controller.samples.loadbalancer.IConfigManager;

/**
 * This class exposes North bound REST APIs for the Load Balancer Service.
 * Following APIs are exposed by the Load Balancer Service:
 * 
 * Data retrieval REST APIs::
 * 	1. Get details of all existing pools
 * 		Type : GET  
 * 		URI : /one/nb/v2/lb/{container-name}/
 * 	NOTE: Current implementation of the opendaylight usage 'default' as a container-name
 * 	e.g : http://localhost:8080/one/nb/v2/lb/default will give you list of all the pools
 * 	
 * 	2. Get details of all the existing VIPs
 * 		Type : GET
 * 		URI:  /one/nb/v2/lb/{container-name}/vips
 * 
 * Pool related REST APIs::
 * 	1. Create Pool : 
 * 		Type : POST
 * 		URI : /one/nb/v2/lb/{container-name}/create/pool
 * 		Request body :
 *                      {
 *                              "name":"",
 *                              "lbmethod":""
 *                      }
 * 		Currently, two load balancing policies are allowed {"roundrobin" and "random" }
 * 
 * 	2. Delete Pool : 
 * 		Type : DELETE
 * 		URI : /one/nb/v2/lb/{container-name}/delete/pool/{pool-name}
 * 
 * VIP related REST APIs::
 * 	1. Create VIP: 
 * 		Type : POST
 * 		URI : /one/nb/v2/lb/{container-name}/create/vip
 * 		Request body :
 *                      {
 *                              "name":"",
 *                              "ip":"ip in (xxx.xxx.xxx.xxx) format",
 *                              "protocol":"TCP/UDP",
 *                              "port":"any valid port number",
 *                              "poolname":"" (optional)
 *                       }
 * 		The pool name is optional and can be set up at a later stage (using the REST API given below).
 * 
 * 	2. Update VIP: Update pool name of the VIP
 * 		Type : PUT
 * 		URI : /one/nb/v2/lb/{container-name}/update/vip
 * 		Request body :
 *                      {
 *                              "name":"",
 *                              "poolname":""
 *                       }
 *              Currently, we only allow update of the VIP pool name (if a VIP does not have an attached pool)
 *              and not of the VIP name itself.
 *              The specified pool name must already exist. If the specified VIP is already attached to a pool, the update
 *              will fail.
 * 
 * 	3. Delete VIP : 
 * 		Type : DELETE
 * 		URI : /one/nb/v2/lb/{container-name}/delete/vip/{vip-name} 
 * 
 * Pool member related REST APIs::
 * 	1. Create pool member:
 * 		Type : POST
 * 		URI : /one/nb/v2/lb/default/create/poolmember
 * 		Request body :
 *                      {
 *                              "name":"",
 *                              "ip":"ip in (xxx.xxx.xxx.xxx) format",
 *                              "poolname":"existing pool name"
 *                       }
 * 
 * 	2. Delete pool member:
 * 		Type : DELETE
 * 		URI	: /one/nb/v2/lb/{container-name}/delete/poolmember/{pool-member-name}/{pool-name}
 *	
 *	NOTE: Property "name" of each individual entity must be unique. 
 *	All the above REST APIs throw appropriate response codes in case of error/success. 
 *	Please consult the respective methods to get details of various response codes.
 */

@Path("/")
public class LoadBalancerNorthbound {
    
    /*
     * Method returns the Load balancer service instance running within
     * 'default' container.
     */
    private IConfigManager getConfigManagerService(String containerName) {
        IContainerManager containerManager = (IContainerManager) ServiceHelper
                .getGlobalInstance(IContainerManager.class, this);
        if (containerManager == null) {
            throw new ServiceUnavailableException("Container "
                    + RestMessages.SERVICEUNAVAILABLE.toString());
        }

        boolean found = false;
        List<String> containerNames = containerManager.getContainerNames();
        for (String cName : containerNames) {
            if (cName.trim().equalsIgnoreCase(containerName.trim())) {
                found = true;
            }
        }

        if (found == false) {
            throw new ResourceNotFoundException(containerName + " "
                    + RestMessages.NOCONTAINER.toString());
        }

        IConfigManager configManager = (IConfigManager) ServiceHelper.getInstance(
        		IConfigManager.class, containerName, this);

        if (configManager == null) {
            throw new ServiceUnavailableException("Load Balancer"
                    + RestMessages.SERVICEUNAVAILABLE.toString());
        }

        return configManager;
    }

    @Path("/{containerName}")
    @GET
    @Produces( { MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    @TypeHint(Pools.class)
    @StatusCodes( {
        @ResponseCode(code = 200, condition = "Operation successful"),
        @ResponseCode(code = 404, condition = "The containerName is not found"),
        @ResponseCode(code = 503, condition = "Load balancer service is unavailable") })
    public Pools getAllPools(
            @PathParam("containerName") String containerName) {
        
        IConfigManager configManager = getConfigManagerService(containerName);
        if (configManager == null) {
            throw new ServiceUnavailableException("Load Balancer "
                                                    + RestMessages.SERVICEUNAVAILABLE.toString());
        }
        
        return new Pools(configManager.getAllPools());
    }

    @Path("/{containerName}/vips")
    @GET
    @Produces( { MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    @TypeHint(VIPs.class)
    @StatusCodes( {
        @ResponseCode(code = 200, condition = "Operation successful"),
        @ResponseCode(code = 404, condition = "The containerName is not found"),
        @ResponseCode(code = 503, condition = "Load balancer service is unavailable") })
    public VIPs getAllVIPs(
            @PathParam("containerName") String containerName) {
        
        IConfigManager configManager = getConfigManagerService(containerName);
        if (configManager == null) {
            throw new ServiceUnavailableException("Load Balancer "
                    + RestMessages.SERVICEUNAVAILABLE.toString());
        }

        return new VIPs(configManager.getAllVIPs());
    }

    @Path("/{containerName}/create/vip")
    @POST
    @Consumes( { MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    @StatusCodes( {
        @ResponseCode(code = 201, condition = "VIP created successfully"),
        @ResponseCode(code = 404, condition = "The Container Name not found"),
        @ResponseCode(code = 503, condition = "Load balancer service is unavailable"),
        @ResponseCode(code = 409, condition = "VIP already exist"),
        @ResponseCode(code = 415, condition = "Invalid input data")})
    public Response addVIP(@PathParam("containerName") String containerName,
            @TypeHint(VIP.class) JAXBElement<VIP> inVIP){
        
        VIP vipInput = inVIP.getValue();
        String name = vipInput.getName();
        String ip = vipInput.getIp();
        String protocol = vipInput.getProtocol();
        short protocolPort = vipInput.getPort();
        String poolName = vipInput.getPoolName();
        if(name.isEmpty() ||
                ip.isEmpty()||
                protocol.isEmpty()||
                protocolPort < 0 ){
            throw new UnsupportedMediaTypeException(RestMessages.INVALIDDATA.toString());
        }
        
        IConfigManager configManager = getConfigManagerService(containerName);
        
        if (configManager == null) {
            throw new ServiceUnavailableException("Load Balancer "
                    + RestMessages.SERVICEUNAVAILABLE.toString());
        }
        
        if(!configManager.vipExists(name, ip, protocol, protocolPort, poolName)){
            
            VIP vip = configManager.createVIP(name, ip, protocol, protocolPort, poolName);
            if ( vip != null){
                return Response.status(Response.Status.CREATED).build();
            }
        }else{
            throw new ResourceConflictException(NBConst.RES_VIP_ALREADY_EXIST);
        }
        throw new InternalServerErrorException(NBConst.RES_VIP_CREATION_FAILED);
    }

    @Path("/{containerName}/update/vip")
    @PUT
    @Consumes( { MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    @StatusCodes( {
        @ResponseCode(code = 201, condition = "VIP updated successfully"),
        @ResponseCode(code = 404, condition = "The containerName not found"),
        @ResponseCode(code = 503, condition = "VIP not found"),
        @ResponseCode(code = 404, condition = "Pool not found"),
        @ResponseCode(code = 405, condition = "Pool already attached to the VIP"),
        @ResponseCode(code = 415, condition = "Invalid input name")})
    public Response updateVIP(@PathParam("containerName") String containerName,
            @TypeHint(VIP.class) JAXBElement<VIP> inVIP) {
        
        VIP vipInput = inVIP.getValue();
        String name = vipInput.getName();
        String poolName = vipInput.getPoolName();
        if(name.isEmpty() ||
                poolName.isEmpty()){
            throw new UnsupportedMediaTypeException(RestMessages.INVALIDDATA.toString());
        }
        
        IConfigManager configManager = getConfigManagerService(containerName);
        if (configManager == null) {
            throw new ServiceUnavailableException("Load Balancer "
                                                + RestMessages.SERVICEUNAVAILABLE.toString());
        }
        
        if(!configManager.poolExists(poolName))
            throw new ResourceNotFoundException(NBConst.RES_POOL_NOT_FOUND);
        
        if(configManager.getVIPAttachedPool(name)!=null)
            throw new MethodNotAllowedException(NBConst.RES_VIP_POOL_EXIST);
        
        if(configManager.updateVIP(name, poolName)!= null)
            return Response.status(Response.Status.ACCEPTED).build();
        
        throw new InternalServerErrorException(NBConst.RES_VIP_UPDATE_FAILED);
    }
    
    @Path("/{containerName}/delete/vip/{vipName}")
    @DELETE
    @Consumes( { MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    @StatusCodes( {
        @ResponseCode(code = 200, condition = "VIP deleted successfully"),
        @ResponseCode(code = 404, condition = "The containerName not found"),
        @ResponseCode(code = 503, condition = "Load balancer service is unavailable"),
        @ResponseCode(code = 404, condition = "VIP not found"),
        @ResponseCode(code = 500, condition = "Failed to delete VIP")})
    public Response deleteVIP(
            @PathParam(value = "containerName") String containerName,
            @PathParam(value = "vipName") String vipName) {
        
        if(vipName.isEmpty())
            throw new UnsupportedMediaTypeException(RestMessages.INVALIDDATA.toString());
        
        IConfigManager configManager = getConfigManagerService(containerName);
        if (configManager == null) {
            throw new ServiceUnavailableException("Load Balancer"
                                            + RestMessages.SERVICEUNAVAILABLE.toString());
        }
        
        if(!configManager.vipExists(vipName))
            throw new ResourceNotFoundException(NBConst.RES_VIP_NOT_FOUND);
        
        for(VIP vip : configManager.getAllVIPs()){
            if(vip.getName().equals(vipName)){
                configManager.deleteVIP(vipName);
                return Response.ok().build();
            }
        }
        throw new InternalServerErrorException(NBConst.RES_VIP_DELETION_FAILED);
    }

    @Path("/{containerName}/create/pool")
    @POST
    @Consumes( { MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    @StatusCodes( {
        @ResponseCode(code = 201, condition = "Pool created successfully"),
        @ResponseCode(code = 404, condition = "The containerName not found"),
        @ResponseCode(code = 503, condition = "Load balancer service is unavailable"),
        @ResponseCode(code = 409, condition = "Pool already exist"),
        @ResponseCode(code = 415, condition = "Invalid input data")})
    public Response addPool(@PathParam("containerName") String containerName,
            @TypeHint(Pool.class) JAXBElement<Pool> inPool) {
        
        Pool poolInput = inPool.getValue();
        String name = poolInput.getName();
        String lbMethod =poolInput.getLbMethod();
        if(name.isEmpty() ||
                lbMethod.isEmpty()){
            throw new UnsupportedMediaTypeException(RestMessages.INVALIDDATA.toString());
        }
        
        IConfigManager configManager = getConfigManagerService(containerName);
        if (configManager == null) {
            throw new ServiceUnavailableException("Load Balancer "
                                            + RestMessages.SERVICEUNAVAILABLE.toString());
        }
        
        if(!configManager.poolExists(name)){
            
            Pool pool = configManager.createPool(name, lbMethod);
            if ( pool != null){
                return Response.status(Response.Status.CREATED).build();
            }
        }else{
            throw new ResourceConflictException(NBConst.RES_POOL_ALREADY_EXIST);
        }
        throw new InternalServerErrorException(NBConst.RES_POOL_CREATION_FAILED);
    }

    @Path("/{containerName}/delete/pool/{poolName}")
    @DELETE
    @Consumes( { MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    @StatusCodes( {
        @ResponseCode(code = 200, condition = "Pool deleted successfully"),
        @ResponseCode(code = 404, condition = "The containerName not found"),
        @ResponseCode(code = 503, condition = "Load balancer service is unavailable"),
        @ResponseCode(code = 404, condition = "Pool not found"),
        @ResponseCode(code = 500, condition = "Failed to delete Pool")})
    public Response deletePool(
            @PathParam(value = "containerName") String containerName,
            @PathParam(value = "poolName") String poolName) {
        
        if(poolName.isEmpty())
            throw new UnsupportedMediaTypeException(RestMessages.INVALIDDATA.toString());
        
        IConfigManager configManager = getConfigManagerService(containerName);
        if (configManager == null) {
            throw new ServiceUnavailableException("Load Balancer"
                                        + RestMessages.SERVICEUNAVAILABLE.toString());
        }
        
        if(!configManager.poolExists(poolName))
            throw new ResourceNotFoundException(NBConst.RES_POOL_NOT_FOUND);
        
        for(Pool pool:configManager.getAllPools()){
            if(pool.getName().equals(poolName)){
                configManager.deletePool(poolName);
                return Response.ok().build();
            }
        }
        throw new InternalServerErrorException(NBConst.RES_POOL_DELETION_FAILED);
    }

    @Path("/{containerName}/create/poolmember")
    @POST
    @Consumes( { MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    @StatusCodes( {
        @ResponseCode(code = 201, condition = "Pool member created successfully"),
        @ResponseCode(code = 404, condition = "The containerName not found"),
        @ResponseCode(code = 503, condition = "Load balancer service is unavailable"),
        @ResponseCode(code = 404, condition = "Pool not found"),
        @ResponseCode(code = 409, condition = "Pool member already exist"),
        @ResponseCode(code = 415, condition = "Invalid input data")})
    public Response addPoolMember(@PathParam("containerName") String containerName,
            @TypeHint(PoolMember.class) JAXBElement<PoolMember> inPoolMember){
        
        PoolMember pmInput = inPoolMember.getValue();
    	String name = pmInput.getName();
    	String memberIP = pmInput.getIp();
    	String poolName = pmInput.getPoolName();
    	
    	if(name.isEmpty() ||
    	        memberIP.isEmpty()||
    	        poolName.isEmpty()){
    	    throw new UnsupportedMediaTypeException(RestMessages.INVALIDDATA.toString());
    	}
    	
    	IConfigManager configManager = getConfigManagerService(containerName);
    	if (configManager == null) {
    	    throw new ServiceUnavailableException("Load Balancer "
    	                                + RestMessages.SERVICEUNAVAILABLE.toString());
    	}
    	
    	if(!configManager.poolExists(poolName))
    	    throw new ResourceNotFoundException(NBConst.RES_POOL_NOT_FOUND);
    	
    	if(!configManager.memberExists(name, memberIP, poolName)){
    	    
    	    PoolMember poolMember = configManager.addPoolMember(name, memberIP, poolName);
    	    if ( poolMember != null){
    	        return Response.status(Response.Status.CREATED).build();
    	    }
    	}else{
    	    throw new ResourceConflictException(NBConst.RES_POOLMEMBER_ALREADY_EXIST);
    	}
    	throw new InternalServerErrorException(NBConst.RES_POOLMEMBER_CREATION_FAILED);
    }

    @Path("/{containerName}/delete/poolmember/{poolMemberName}/{poolName}")
    @DELETE
    @Consumes( { MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    @StatusCodes( {
        @ResponseCode(code = 200, condition = "Pool member deleted successfully"),
        @ResponseCode(code = 404, condition = "The containerName not found"),
        @ResponseCode(code = 503, condition = "Load balancer service is unavailable"),
        @ResponseCode(code = 404, condition = "Pool member not found"),
        @ResponseCode(code = 404, condition = "Pool not found")})
    public Response deletePoolMember(
            @PathParam(value = "containerName") String containerName,
            @PathParam(value = "poolMemberName") String poolMemberName,
            @PathParam(value = "poolName") String poolName) {
        
        if(poolMemberName.isEmpty()||
                poolName.isEmpty())
            throw new UnsupportedMediaTypeException(RestMessages.INVALIDDATA.toString());
        
        IConfigManager configManager = getConfigManagerService(containerName);
        
        if (configManager == null) {
            throw new ServiceUnavailableException("Load Balancer"
                                        + RestMessages.SERVICEUNAVAILABLE.toString());
        }
        
        if(!configManager.poolExists(poolName))
            throw new ResourceNotFoundException(NBConst.RES_POOL_NOT_FOUND);
        
        if(configManager.memberExists(poolMemberName, poolName)){
            
            configManager.removePoolMember(poolMemberName, poolName);
            
            return Response.ok().build();
        }
        throw new ResourceNotFoundException(NBConst.RES_POOLMEMBER_NOT_FOUND);
    }
}
