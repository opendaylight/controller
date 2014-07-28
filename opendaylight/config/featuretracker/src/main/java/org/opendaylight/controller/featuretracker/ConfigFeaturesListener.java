package org.opendaylight.controller.featuretracker;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.JAXBException;

import org.apache.karaf.features.ConfigFileInfo;
import org.apache.karaf.features.Dependency;
import org.apache.karaf.features.Feature;
import org.apache.karaf.features.FeatureEvent;
import org.apache.karaf.features.FeaturesListener;
import org.apache.karaf.features.FeaturesService;
import org.apache.karaf.features.RepositoryEvent;
import org.opendaylight.controller.config.persist.api.ConfigSnapshotHolder;
import org.osgi.framework.Version;
import org.osgi.framework.VersionRange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConfigFeaturesListener implements FeaturesListener {
    private static final Logger logger = LoggerFactory.getLogger(ConfigFeaturesListener.class);
    private FeaturesService featureService;
    /*
     * This will need to be extracted to a service from ConfigPusher in config-persister.
     * This ConfigPusher will need to keep a threadsafe queue for the configs its pushing.
     */

    private ConfigPusher pusher;

    public ConfigFeaturesListener(FeaturesService f, ConfigPusher p) {
        featureService = f;
        pusher = p;
    }
    @Override
    public void featureEvent(FeatureEvent event) {

        logFeature("Feature Type:" + event.getType(),event.getFeature(),event.isReplay());
        // Look into what isReplay means
        pusher.pushConfigs(featureToConfigSnapShots(event.getFeature()));
    }

    protected List<ConfigSnapshotHolder> featureToConfigSnapsShots( Feature f) {
        List<Feature> dependentFeatures = getDepedentFeatures(event.getFeature());
        // Also do the feature itself
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

    protected List<ConfigFileInfo> extractConfigFilesFromFeatures(List<Features> features) {
        // Extracts all config files from Features taking care to preverse order in which
        // config files appear in the Features (ie, if FeatureA has C1,C2, C3 in that order
        // then C1 should always proceed C2 in the list.
    }

    protected List<ConfigFileInfo> orderRespectingDedupe(List<ConfigFileInfo> configFiles) {
        // Removed duplicates from the list such that the order of the list is respected
        // And the first instance of a Feature in that list is retained *unle INCOMPLETE
    }

    protected List<ConfigSnapshotHolder> configFilesToConfigSnapShotHolders(List<ConfigFileInfo>configFiles) {
        // Convert the List<ConfigFileInfo> to List<ConfigSnapShotHolder> preserving the list
        // order.
        // See: XmlDirectoryPersister.loadLastConfig(File file) on line 104 of XMLDirectoryPersister
    }

    protected List<Feature> orderRespectingDedupe(List<Feature> features) {
        // Remove duplicates from the list such that the order of the list is respected
        // And the first instance of a Feature in the list is the on retained.
        return null.
    }

    @Override
    public void repositoryEvent(RepositoryEvent event) {
        logger.info("Repository: " + event.getType() + " " + event.getRepository());
    }

}
