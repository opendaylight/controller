package org.opendaylight.controller.ping.northbound;

import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;

import org.codehaus.enunciate.jaxrs.ResponseCode;
import org.codehaus.enunciate.jaxrs.StatusCodes;
import org.opendaylight.controller.ping.service.api.PingServiceAPI;
import org.opendaylight.controller.sal.utils.ServiceHelper;

@Path("/")
public class PingNorthbound {
    /**
     * Ping test
     */
    @Path("/ping/{ipAddress}")
    @PUT
    @StatusCodes({
            @ResponseCode(code = 200, condition = "Destination reachable"),
            // @ResponseCode(code = 206, condition = "Ping in progress"),
            @ResponseCode(code = 503, condition = "Internal error"),
            @ResponseCode(code = 503, condition = "Destination unreachable") })
    public Response ping(@PathParam(value = "ipAddress") String ipAddress) {
        return pingCommon(ipAddress, true);
    }

    @Path("/pingasync/{ipAddress}")
    @PUT
    @StatusCodes({
            @ResponseCode(code = 200, condition = "Destination reachable"),
            @ResponseCode(code = 206, condition = "Ping in progress"),
            @ResponseCode(code = 503, condition = "Internal error"),
            @ResponseCode(code = 503, condition = "Destination unreachable") })
    public Response pingasync(@PathParam(value = "ipAddress") String ipAddress) {
        return pingCommon(ipAddress, false);
    }

    private Response pingCommon(String ipAddress, boolean isSync) {
        PingServiceAPI ping = (PingServiceAPI) ServiceHelper.getGlobalInstance(PingServiceAPI.class, this);
        if (ping == null) {
            /* Ping service not found. */
            return Response.ok(new String("No ping service")).status(500).build();
        }
        PingServiceAPI.PingResult pingResult = isSync ?
                ping.pingDestinationSync(ipAddress) : ping.pingDestinationAsync(ipAddress);
        if (pingResult == PingServiceAPI.PingResult.InProgress)
            return Response.ok(new String(ipAddress + " - " + pingResult)).status(206).build();
        if (pingResult == PingServiceAPI.PingResult.GotResponse)
            return Response.ok(new String(ipAddress + " - " + pingResult)).build();
        return Response.ok(new String(ipAddress + " - " + pingResult)).status(503).build();
    }
}
