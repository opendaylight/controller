package org.opendaylight.controller.versionapi.northbound;

import java.util.ArrayList;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

/**
 * Northbound APIs that returns various Statistics exposed by the Southbound
 * protocol plugins such as Openflow.
 *
 * <br>
 * <br>
 * Authentication scheme : <b>HTTP Basic</b><br>
 * Authentication realm : <b>opendaylight</b><br>
 * Transport : <b>HTTP and HTTPS</b><br>
 * <br>
 * HTTPS Authentication is disabled by default.
 *
 */
@Path("/")
public class VersionapiNorthbound{

    private ConfigUpdater cu;

    @GET
    @Path("/getversion")
    @Produces({ MediaType.APPLICATION_JSON })
    public ArrayList<Version> getVersion() {
        return cu.getVersions();
    }

    public void setConfigUpdater(ConfigUpdater cu){
        this.cu = cu;
    }
}