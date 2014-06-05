
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

import org.apache.karaf.features.Feature;
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

    public FeaturesServiceNorthbound() {
        super();
    }
    public FeaturesServiceNorthbound(FeaturesService featuresService) {
        super();
        this.featuresService = featuresService;
    }

    @GET
    @Path("/list")
    @Produces(MediaType.APPLICATION_JSON)
    public List<FeatureJson> listFeatures() throws Exception {
        List<FeatureJson> list = new ArrayList<FeatureJson>();
        List<FeatureJson> fInstalled = getInstalledFeatures();
        for(org.apache.karaf.features.Feature f : featuresService.listFeatures()) {
            FeatureJson tempF = toFeatureJson(f, false);
            tempF.setIsInstall(isFeatureInstalled(tempF, fInstalled));
            list.add(tempF);
        }
        return list;
    }

    @GET
    @Path("/installed")
    @Produces(MediaType.APPLICATION_JSON)
    public List<FeatureJson> installedFeatures() throws Exception{
        return getInstalledFeatures();
    }

    @POST
    @Path("/install")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response installFeature(FeatureName feature) {
        String result = "Feature Installed : " + feature;
        try {
            featuresService.installFeature(feature.getName());
        } catch(Exception e) {
            System.out.println(e);
            return Response.status(500).entity("Feature " + feature.getName() + " could not be installed").build();
        }
        return Response.status(201).entity(result).build();
    }

    @DELETE
    @Path("/uninstall")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response uninstallFeature(FeatureName feature) {
        String result = "Feature Uninstalled : " + feature;
        try {
            featuresService.uninstallFeature(feature.getName());
        } catch(Exception e) {
            System.out.println(e);
            return Response.status(500).entity("Feature " + feature.getName() + " could not be installed").build();
        }
        return Response.status(200).entity(result).build();
    }

    public void setFeaturesService(FeaturesService featuresService) {
        this.featuresService = featuresService;
    }

    public FeaturesService getFeaturesService() {
        return this.featuresService;
    }

    private List<FeatureJson> getInstalledFeatures() {
        Feature[] fInstalled = featuresService.listInstalledFeatures();
        ArrayList<FeatureJson> installedFeatures = new ArrayList<FeatureJson>();
        for(Feature f : fInstalled) {
            installedFeatures.add(toFeatureJson(f, true));
        }
        return installedFeatures;
    }

    private Boolean isFeatureInstalled(FeatureJson f, List<FeatureJson> iFeatures) {
        for(FeatureJson f2 : iFeatures) {
            if(f.name == f2.name) {
                return true;
            }
        }
        return false;
    }

    private FeatureJson toFeatureJson(Feature feature, Boolean installed) {
        return new FeatureJson(feature.getId(),
                feature.getName(),
                feature.getDescription(),
                feature.getDetails(),
                feature.getVersion(),
                feature.hasVersion(),
                feature.getResolver(),
                installed);
    }
}