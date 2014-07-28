package org.opendaylight.controller.featuretracker;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.SortedSet;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

import org.apache.felix.utils.version.VersionRange;
import org.apache.felix.utils.version.VersionTable;
import org.apache.karaf.features.ConfigFileInfo;
import org.apache.karaf.features.Dependency;
import org.apache.karaf.features.Feature;
import org.apache.karaf.features.FeatureEvent;
import org.apache.karaf.features.FeaturesListener;
import org.apache.karaf.features.FeaturesService;
import org.apache.karaf.features.RepositoryEvent;
import org.apache.karaf.features.internal.model.Features;
import org.opendaylight.controller.config.persist.api.ConfigSnapshotHolder;
import org.opendaylight.controller.config.persist.storage.file.xml.model.ConfigSnapshot;
import org.opendaylight.controller.netconf.api.NetconfDocumentedException;
import org.opendaylight.controller.netconf.persist.impl.ConfigPusher;
import org.osgi.framework.Version;
//import org.osgi.framework.VersionRange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConfigFeaturesListener implements FeaturesListener {
    private static final Logger logger = LoggerFactory.getLogger(ConfigFeaturesListener.class);
    private FeaturesService featureService;
    /*
     * This will need to be extracted to a service from ConfigPusher in config-persister.
     * This ConfigPusher will need to keep a threadsafe queue for the configs its pushing.
     *
     *
     * See the comments where I said we'd need to both expose ConfigPusher as a service,
     * and have it stuck a thread safe queue in front for it to take from
     */

    private ConfigPusher pusher;

    public ConfigFeaturesListener(FeaturesService f, ConfigPusher p) {
        featureService = f;
        pusher = p;
    }
    @Override
    public void featureEvent(FeatureEvent event) {

        logFeature("Feature Type:" + event.getType(),event.getFeature(),event.isReplay());
        //todo Look into what isReplay means
        try {
            pusher.pushConfigs(featureToConfigSnapsShots(event));
        } catch (NetconfDocumentedException nde) {
            logger.error(nde.getMessage());
        }
    }

    protected List<ConfigSnapshotHolder> featureToConfigSnapsShots(FeatureEvent event) {
        List<ConfigSnapshotHolder> configs = new ArrayList<ConfigSnapshotHolder>();
        List<Feature> dependentFeatures = getDepedentFeatures(event.getFeature());
        for(Feature f: dependentFeatures) {
            logFeature("Feature Type: DEPENDENCY",f,false);
        }
        logger.debug("Dedupe features.");
        dependentFeatures = orderRespectingDedupe(dependentFeatures);
        logger.debug("Extract config files.");
        List<ConfigFileInfo> inputFiles = extractConfigFilesFromFeatures(dependentFeatures);
        logger.debug("Dedupe config files.");
        inputFiles = orderConfigRespectingDedupe(inputFiles);
        logger.debug("Convert config files to xml config pusher can use.");
        configs = configFilesToConfigSnapShotHolders(inputFiles);
        return configs;
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
        returnvalue.add(feature); //Add back the parent
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

    protected List<Feature> orderRespectingDedupe(List<Feature> features) {
        // Remove duplicates from the list such that the order of the list is respected
        // And the first instance of a Feature in the list is the on retained.
        ArrayList<Feature> deduped = new ArrayList<Feature>();
        for(Feature f: features) {
            if (!deduped.contains(f)) {
                deduped.add(f);
            }
        }
        return deduped;
    }

    protected List<ConfigFileInfo> extractConfigFilesFromFeatures(List<Feature> features) {
        // Extracts all config files from Features taking care to preserve order in which
        // config files appear in the Features (ie, if FeatureA has C1,C2, C3 in that order
        // then C1 should always proceed C2 in the list.
        List<ConfigFileInfo> cfi = new ArrayList<ConfigFileInfo>();
        for(Feature f: features) {
            List<ConfigFileInfo> t = f.getConfigurationFiles();
            for(ConfigFileInfo i: t) {
                cfi.add(i);
            }
        }
        return cfi;
    }

    protected List<ConfigFileInfo> orderConfigRespectingDedupe(List<ConfigFileInfo> configFiles) {
        // Removed duplicates from the list such that the order of the list is respected
        // And the first instance of a Feature in that list is retained *unle INCOMPLETE
        ArrayList<ConfigFileInfo> deduped = new ArrayList<ConfigFileInfo>();
        for(ConfigFileInfo c: configFiles) {
            if (!deduped.contains(c)) {
                deduped.add(c);
            }
        }
        return deduped;
    }

    protected List<ConfigSnapshotHolder> configFilesToConfigSnapShotHolders(List<ConfigFileInfo>configFiles) {
        // Convert the List<ConfigFileInfo> to List<ConfigSnapShotHolder> preserving the list
        // order.
        // See: XmlDirectoryPersister.loadLastConfig(File file) on line 104 of XMLDirectoryPersister

        ArrayList<ConfigSnapshotHolder> converted = new ArrayList<ConfigSnapshotHolder>();
        for (ConfigFileInfo c : configFiles) {
            // really bad way of converting only xml files
            if (c.getFinalname().endsWith(".xml")) {
                ConfigSnapshotHolder mod = configFileInfoToConfigSnapshotHolder(c);
                converted.add(mod);
            } else {
                logger.debug("Not processing non-xml input: " + c.getFinalname());
            }
        }
        return converted;
    }

    private ConfigSnapshotHolder configFileInfoToConfigSnapshotHolder(ConfigFileInfo fileInfo) {
        Unmarshaller um = null;
        try {
            JAXBContext jaxbContext = JAXBContext.newInstance(ConfigSnapshot.class);
            um = jaxbContext.createUnmarshaller();
            File file = new File(fileInfo.getFinalname());
            return asHolder((ConfigSnapshot) um.unmarshal(file));
        } catch (JAXBException jbe) {
            logger.error(jbe.getMessage());
        }
        return null; // fail
    }

    private static ConfigSnapshotHolder asHolder(final ConfigSnapshot unmarshalled) {
        return new ConfigSnapshotHolder() {
            @Override
            public String getConfigSnapshot() {
                return unmarshalled.getConfigSnapshot();
            }

            @Override
            public SortedSet<String> getCapabilities() {
                return unmarshalled.getCapabilities();
            }

            @Override
            public String toString() {
                return unmarshalled.toString();
            }
        };
    }

    @Override
    public void repositoryEvent(RepositoryEvent event) {
        logger.info("Repository: " + event.getType() + " " + event.getRepository());
    }

}
