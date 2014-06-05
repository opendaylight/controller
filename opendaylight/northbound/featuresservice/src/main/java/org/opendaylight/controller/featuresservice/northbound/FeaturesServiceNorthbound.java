
package org.opendaylight.controller.featuresservice.northbound;


import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.apache.karaf.features.FeaturesService;

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
 *
 *
 */

@Path("/features")
public class FeaturesServiceNorthbound {
    private FeaturesService featuresService = null;

    public FeaturesServiceNorthbound(FeaturesService featuresService) {
        super();
        this.featuresService = featuresService;
    }

    @GET
    @Path("/list")
    @Produces(MediaType.APPLICATION_JSON)
    public List<Feature> listFeatures() throws Exception {
        Feature feature = getFeature();
        List<Feature> list = new ArrayList<Feature>();
        list.add(feature);
        list.add(getFeature());
        return list;
    }

    @GET
    @Path("/installed")
    @Produces(MediaType.APPLICATION_JSON)
    public List<Feature> installedFeatures() {
        Feature feature = getFeature();
        List<Feature> list = new ArrayList<Feature>();
        feature.setName("odl-managers");
        list.add(feature);
        list.add(getFeature());
        list.add(getFeature());
        return list;
    }

    @POST
    @Path("/install")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response installFeature(FeatureName feature) {
        String result = "Feature Installed : " + feature;
        return Response.status(201).entity(result).build();
    }

    @DELETE
    @Path("/uninstall")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response uninstallFeature(FeatureName feature) {
        String result = "Feature Uninstalled : " + feature;
        return Response.status(200).entity(result).build();
    }

    private Feature getFeature() {
        Feature feature = new Feature("1234",
                "odl-managers",
                "managers",
                "detail managers",
                "1.11-SNAPSHOT",
                true, "maven.org");
        return feature;
    }

    public void setFeaturesService(FeaturesService featuresService) {
        this.featuresService = featuresService;
    }
}