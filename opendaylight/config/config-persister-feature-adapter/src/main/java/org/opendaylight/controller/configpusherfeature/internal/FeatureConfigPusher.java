/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.configpusherfeature.internal;

import com.google.common.base.Optional;
import com.google.common.collect.LinkedHashMultimap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.ConcurrentModificationException;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.apache.karaf.features.Feature;
import org.apache.karaf.features.FeaturesService;
import org.opendaylight.controller.config.persist.api.ConfigPusher;
import org.opendaylight.controller.config.persist.api.ConfigSnapshotHolder;
import org.opendaylight.controller.config.persist.storage.file.xml.XmlFileStorageAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/*
 * Simple class to push configs to the config subsystem from Feature's configfiles
 */
public class FeatureConfigPusher {

    private static final Logger LOG = LoggerFactory.getLogger(FeatureConfigPusher.class);

    private static final int MAX_RETRIES = 10;
    private static final int RETRY_PAUSE_MILLIS = 100;

    private final FeaturesService featuresService;
    private final ConfigPusher pusher;

    /*
     * A LinkedHashSet (to preserve order and insure uniqueness) of the pushedConfigs
     * This is used to prevent pushing duplicate configs if a Feature is in multiple dependency
     * chains.  Also, preserves the *original* Feature chain for which we pushed the config.
     * (which is handy for logging).
     */
    Set<FeatureConfigSnapshotHolder> pushedConfigs = new LinkedHashSet<>();

    /*
     * LinkedHashMultimap to track which configs we pushed for each Feature installation
     * For future use
     */
    LinkedHashMultimap<Feature,FeatureConfigSnapshotHolder> feature2configs = LinkedHashMultimap.create();

    /*
     * @param p - ConfigPusher to push ConfigSnapshotHolders
     */
    public FeatureConfigPusher(final ConfigPusher pusher, final FeaturesService featuresService) {
        this.pusher = pusher;
        this.featuresService = featuresService;
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
    public LinkedHashMultimap<Feature, FeatureConfigSnapshotHolder> pushConfigs(final List<Feature> features) throws Exception {
        LinkedHashMultimap<Feature, FeatureConfigSnapshotHolder> pushedFeatures = LinkedHashMultimap.create();
        for (Feature feature : features) {


            Set<FeatureConfigSnapshotHolder> configSnapShots = pushConfig(feature);
            if (!configSnapShots.isEmpty()) {
                pushedFeatures.putAll(feature, configSnapShots);
            }
        }
        return pushedFeatures;
    }

    private Set<FeatureConfigSnapshotHolder> pushConfig(final Feature feature) throws Exception {
        Set<FeatureConfigSnapshotHolder> configs = new LinkedHashSet<>();
        if(isInstalled(feature)) {
            // FIXME Workaround for BUG-2836, features service returns null for feature: standard-condition-webconsole_0_0_0, 3.0.1
            if(featuresService.getFeature(feature.getName(), feature.getVersion()) == null) {
                LOG.debug("Feature: {}, {} is missing from features service. Skipping", feature.getName(), feature.getVersion());
            } else {
                ChildAwareFeatureWrapper wrappedFeature = new ChildAwareFeatureWrapper(feature, featuresService);
                configs = wrappedFeature.getFeatureConfigSnapshotHolders();
                if (!configs.isEmpty()) {
                    configs = pushConfig(configs, feature);
                    feature2configs.putAll(feature, configs);
                }
            }
        }
        return configs;
    }

    private boolean isInstalled(final Feature feature) {
        Exception lastException = null;
        for (int retries = 0; retries < MAX_RETRIES; retries++) {
            try {
                List<Feature> installedFeatures = Arrays.asList(featuresService.listInstalledFeatures());
                if (installedFeatures.contains(feature)) {
                    return true;
                } else {
                    LOG.warn("Karaf featuresService.listInstalledFeatures() has not yet finished installing feature (retry {}) {} {}", retries, feature.getName(), feature.getVersion());
                }
            // TODO This catch of ConcurrentModificationException may be able to simply be removed after
            // we're fully on Karaf 4 only, as a comment in BUG-6787 indicates that (in Karaf 4) :
            // "the 'installed' Map of FeaturesServiceImpl .. appears to be correctly synchronized/thread-safe".
            // (Or, if it's still NOK, then it could be fixed properly upstream in Karaf once we're on recent.)
            } catch (ConcurrentModificationException e) {
                // BUG-6787 experience shows that a LOG.warn (or info) here is very confusing to end-users;
                // as we have a retry loop anyway, there is no point informing (and confusing) users of this
                // intermediate state of, so ... NOOP, do not log here.
                lastException = e;
            }
            try {
                Thread.sleep(RETRY_PAUSE_MILLIS);
            } catch (InterruptedException e1) {
                throw new IllegalStateException(e1);
            }
        }
        LOG.error("Giving up (after {} retries) on Karaf featuresService.listInstalledFeatures() "
                        + "which has not yet finished installing feature {} {} (stack trace is last exception caught)",
                MAX_RETRIES, feature.getName(), feature.getVersion(), lastException);
        return false;
    }

    private Set<FeatureConfigSnapshotHolder> pushConfig(final Set<FeatureConfigSnapshotHolder> configs, final Feature feature)
            throws InterruptedException {
        Set<FeatureConfigSnapshotHolder> configsToPush = new LinkedHashSet<>(configs);
        configsToPush.removeAll(pushedConfigs);
        if (!configsToPush.isEmpty()) {

            // Ignore features that are present in persisted current config
            final Optional<XmlFileStorageAdapter> currentCfgPusher = XmlFileStorageAdapter.getInstance();
            if (currentCfgPusher.isPresent() &&
                    currentCfgPusher.get().getPersistedFeatures().contains(feature.getId())) {
                LOG.warn("Ignoring default configuration {} for feature {}, the configuration is present in {}",
                        configsToPush, feature.getId(), currentCfgPusher.get());
            } else {
                pusher.pushConfigs(new ArrayList<ConfigSnapshotHolder>(configsToPush));
            }

            pushedConfigs.addAll(configsToPush);
        }
        Set<FeatureConfigSnapshotHolder> configsPushed = new LinkedHashSet<>(pushedConfigs);
        configsPushed.retainAll(configs);
        return configsPushed;
    }
}
