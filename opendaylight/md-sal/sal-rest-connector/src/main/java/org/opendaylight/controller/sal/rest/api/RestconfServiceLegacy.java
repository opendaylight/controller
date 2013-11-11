package org.opendaylight.controller.sal.rest.api;

import static org.opendaylight.controller.sal.restconf.impl.MediaTypes.API;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;

import org.opendaylight.controller.md.sal.common.api.TransactionStatus;
import org.opendaylight.controller.sal.restconf.impl.StructuredData;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.data.api.CompositeNode;

public interface RestconfServiceLegacy {

    public static final String XML = "+xml";
    public static final String JSON = "+json";
    
    @Deprecated
    @GET
    @Path("/datastore")
    @Produces({API+JSON,API+XML})
    public StructuredData readAllData();

    @Deprecated
    @GET
    @Path("/datastore/{identifier:.+}")
    @Produces({API+JSON,API+XML})
    public StructuredData readData(@PathParam("identifier") String identifier);

    @Deprecated
    @PUT
    @Path("/datastore/{identifier:.+}")
    @Produces({API+JSON,API+XML})
    public RpcResult<TransactionStatus> createConfigurationDataLegacy(@PathParam("identifier") String identifier, CompositeNode payload);

    @Deprecated
    @POST
    @Path("/datastore/{identifier:.+}")
    @Produces({API+JSON,API+XML})
    public RpcResult<TransactionStatus> updateConfigurationDataLegacy(@PathParam("identifier") String identifier, CompositeNode payload);

}
