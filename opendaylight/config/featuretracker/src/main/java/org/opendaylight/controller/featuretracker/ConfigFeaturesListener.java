package org.opendaylight.controller.featuretracker;

import java.util.ArrayList;
import java.util.List;

import org.apache.felix.utils.version.VersionRange;
import org.apache.felix.utils.version.VersionTable;
import org.apache.karaf.features.Dependency;
import org.apache.karaf.features.Feature;
import org.apache.karaf.features.FeatureEvent;
import org.apache.karaf.features.FeaturesListener;
import org.apache.karaf.features.FeaturesService;
import org.apache.karaf.features.RepositoryEvent;
import org.osgi.framework.Version;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConfigFeaturesListener implements FeaturesListener {
    private static final Logger logger = LoggerFactory.getLogger(ConfigFeaturesListener.class);
    private FeaturesService featureService;

    public ConfigFeaturesListener(FeaturesService f) {
        featureService = f;
    }
    @Override
    public void featureEvent(FeatureEvent event) {

        logFeature("Feature Type:" + event.getType(),event.getFeature(),event.isReplay());
        List<Feature> dependentFeatures = getDepedentFeatures(event.getFeature());
        for(Feature f: dependentFeatures) {
            logFeature("Feature Type: DEPENDENCY",f,false);
        }
    }

    protected void logFeature(String msg,Feature feature,boolean isReplay) {
        logger.info(msg +
                " Name: " + feature.getName() +
                " Version: " + feature.getVersion() +
                "isReplay: " + isReplay);
    }

    private List<Feature> getDepedentFeatures(Feature feature) {
        List<Dependency> dependencies = feature.getDependencies();
        List<Feature> returnvalue = new ArrayList<Feature>();
        for(Dependency dependency: dependencies) {
            Feature fi = getFeatureFromDependency(dependency);
            if(fi == null) {
                throw new IllegalArgumentException("Feature: " + feature.getName() +
                                                    " Version: " + feature.getVersion() +
                                                    "is missing Dependency: " +dependency.getName() +
                                                    " Version: " + dependency.getVersion());
            } else {
                returnvalue.addAll(getDepedentFeatures(fi));
                returnvalue.add(fi);
            }
        }
        return returnvalue;
    }

    protected Feature getFeatureFromDependency(Dependency dependency) {
        Feature[] features = featureService.listInstalledFeatures();
        VersionRange range = org.apache.karaf.features.internal.model.Feature.DEFAULT_VERSION.equals(dependency.getVersion())
                ? VersionRange.ANY_VERSION : new VersionRange(dependency.getVersion(), true, true);
        Feature fi = null;
        for(Feature f: features) {
            if (f.getName().equals(dependency.getName())) {
                Version v = VersionTable.getVersion(f.getVersion());
                if (range.contains(v)) {
                    if (fi == null || VersionTable.getVersion(fi.getVersion()).compareTo(v) < 0) {
                        fi = f;
                        break;
                    }
                }
            }
        }
        return fi;
    }

    @Override
    public void repositoryEvent(RepositoryEvent event) {
        logger.info("Repository: " + event.getType() + " " + event.getRepository());
    }

}
