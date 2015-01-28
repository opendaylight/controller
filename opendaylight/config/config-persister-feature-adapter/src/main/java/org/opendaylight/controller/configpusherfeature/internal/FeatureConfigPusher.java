/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.configpusherfeature.internal;

import com.google.common.collect.LinkedHashMultimap;
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

/*
 * Simple class to push configs to the config subsystem from Feature's configfiles
 */
public class FeatureConfigPusher {
    private static final Logger LOG = LoggerFactory.getLogger(FeatureConfigPusher.class);
    private static final int MAX_RETRIES=100;
    private static final int RETRY_PAUSE_MILLIS=1;
    private FeaturesService featuresService = null;
    private ConfigPusher pusher = null;
    /*
     * A LinkedHashSet (to preserve order and insure uniqueness) of the pushedConfigs
     * This is used to prevent pushing duplicate configs if a Feature is in multiple dependency
     * chains.  Also, preserves the *original* Feature chain for which we pushed the config.
     * (which is handy for logging).
     */
    LinkedHashSet<FeatureConfigSnapshotHolder> pushedConfigs = new LinkedHashSet<FeatureConfigSnapshotHolder>();
    /*
     * LinkedHashMultimap to track which configs we pushed for each Feature installation
     * For future use
     */
    LinkedHashMultimap<Feature,FeatureConfigSnapshotHolder> feature2configs = LinkedHashMultimap.create();

    /*
     * @param p - ConfigPusher to push ConfigSnapshotHolders
     */
    public FeatureConfigPusher(final ConfigPusher p, final FeaturesService f) {
        pusher = p;
        featuresService = f;
    }
    /*
     * Push config files from Features to config subsystem
     * @param features - list of Features to extract config files from recursively and push
     * to the config subsystem
     *
     * @return A LinkedHashMultimap of Features to the FeatureConfigSnapshotHolder actually pushed
     * If a Feature is not in the returned LinkedHashMultimap then we couldn't push its configs
     * (Ususally because it was not yet installed)
     */
    public LinkedHashMultimap<Feature,FeatureConfigSnapshotHolder> pushConfigs(final List<Feature> features) throws Exception, InterruptedException {
        LinkedHashMultimap<Feature,FeatureConfigSnapshotHolder> pushedFeatures = LinkedHashMultimap.create();
        for(Feature feature: features) {
            LinkedHashSet<FeatureConfigSnapshotHolder> configSnapShots = pushConfig(feature);
            if(!configSnapShots.isEmpty()) {
                pushedFeatures.putAll(feature,configSnapShots);
            }
        }
        return pushedFeatures;
    }

    private LinkedHashSet<FeatureConfigSnapshotHolder> pushConfig(final Feature feature) throws Exception, InterruptedException {
        LinkedHashSet<FeatureConfigSnapshotHolder> configs = new LinkedHashSet<FeatureConfigSnapshotHolder>();
        if(feature != null && isInstalled(feature)) {
            ChildAwareFeatureWrapper wrappedFeature = new ChildAwareFeatureWrapper(feature,featuresService);
            configs = wrappedFeature.getFeatureConfigSnapshotHolders();
            if(!configs.isEmpty()) {
                configs = pushConfig(configs);
                feature2configs.putAll(feature, configs);
            }
        }
        return configs;
    }

    private boolean isInstalled(final Feature feature) {
        for(int retries=0;retries<MAX_RETRIES;retries++) {
            try {
                List<Feature> installedFeatures = Arrays.asList(featuresService.listInstalledFeatures());
                if(installedFeatures.contains(feature)) {
                    return true;
                } else {
                    LOG.warn("Karaf featuresService.listInstalledFeatures() has not yet finished installing feature (retry {}) {} {}",retries,feature.getName(),feature.getVersion());
                }
            } catch (Exception e) {
                if(retries < MAX_RETRIES) {
                    LOG.warn("Karaf featuresService.listInstalledFeatures() has thrown an exception, retry {}", retries, e);
                } else {
                    LOG.error("Giving up on Karaf featuresService.listInstalledFeatures() which has thrown an exception, retry {}", retries, e);
                    throw e;
                }
            }
            try {
                Thread.sleep(RETRY_PAUSE_MILLIS);
            } catch (InterruptedException e1) {
                throw new IllegalStateException(e1);
            }
        }
        LOG.error("Giving up (after {} retries) on Karaf featuresService.listInstalledFeatures() which has not yet finished installing feature {} {}",MAX_RETRIES,feature.getName(),feature.getVersion());
        return false;
    }

    private LinkedHashSet<FeatureConfigSnapshotHolder> pushConfig(final LinkedHashSet<FeatureConfigSnapshotHolder> configs) throws InterruptedException {
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
