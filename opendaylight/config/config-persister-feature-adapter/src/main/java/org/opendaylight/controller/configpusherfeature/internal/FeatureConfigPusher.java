package org.opendaylight.controller.configpusherfeature.internal;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;

import org.apache.karaf.features.Feature;
import org.apache.karaf.features.FeaturesService;
import org.opendaylight.controller.config.persist.api.ConfigPusher;
import org.opendaylight.controller.config.persist.api.ConfigSnapshotHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.LinkedHashMultimap;

public class FeatureConfigPusher {
    private static final Logger logger = LoggerFactory.getLogger(FeatureConfigPusher.class);
    private FeaturesService featuresService = null;
    private ConfigPusher pusher = null;
    LinkedHashSet<FeatureConfigSnapshotHolder> pushedConfigs = new LinkedHashSet<FeatureConfigSnapshotHolder>();
    LinkedHashMultimap<Feature,FeatureConfigSnapshotHolder> feature2configs = LinkedHashMultimap.create();

    public FeatureConfigPusher(ConfigPusher p, FeaturesService f) {
        pusher = p;
        featuresService = f;
    }

    public LinkedHashMultimap<Feature,FeatureConfigSnapshotHolder> pushConfigs(List<Feature> features) throws Exception, InterruptedException {
        LinkedHashMultimap<Feature,FeatureConfigSnapshotHolder> pushedFeatures = LinkedHashMultimap.create();
        for(Feature feature: features) {
            LinkedHashSet<FeatureConfigSnapshotHolder> configSnapShots = pushConfig(feature);
            if(!configSnapShots.isEmpty()) {
                pushedFeatures.putAll(feature,configSnapShots);
            }
        }
        return pushedFeatures;
    }

    private LinkedHashSet<FeatureConfigSnapshotHolder> pushConfig(Feature feature) throws Exception, InterruptedException {
        LinkedHashSet<FeatureConfigSnapshotHolder> configs = new LinkedHashSet<FeatureConfigSnapshotHolder>();
        if(isInstalled(feature)) {
            ChildAwareFeatureWrapper wrappedFeature = new ChildAwareFeatureWrapper(feature,featuresService);
            configs = wrappedFeature.getFeatureConfigSnapshotHolders();
            if(!configs.isEmpty()) {
                configs = pushConfig(configs);
                feature2configs.putAll(feature, configs);
            }
        }
        return configs;
    }

    private boolean isInstalled(Feature feature) {
        List<Feature> installedFeatures = Arrays.asList(featuresService.listInstalledFeatures());
        return installedFeatures.contains(feature);
    }

    private LinkedHashSet<FeatureConfigSnapshotHolder> pushConfig(LinkedHashSet<FeatureConfigSnapshotHolder> configs) throws InterruptedException {
        LinkedHashSet<FeatureConfigSnapshotHolder> configsToPush = new LinkedHashSet<FeatureConfigSnapshotHolder>(configs);
        configsToPush.removeAll(pushedConfigs);
        if(!configsToPush.isEmpty()) {
            pusher.pushConfigs(new ArrayList<ConfigSnapshotHolder>(configsToPush));
            pushedConfigs.addAll(configsToPush);
        }
        LinkedHashSet<FeatureConfigSnapshotHolder> configsPushed = new LinkedHashSet<FeatureConfigSnapshotHolder>(pushedConfigs);
        configsPushed.retainAll(configs);
        return configsPushed;
    }
}
