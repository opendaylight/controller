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
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import org.apache.karaf.features.Feature;
import org.apache.karaf.features.FeaturesService;
import org.opendaylight.controller.config.persist.api.ConfigPusher;
import org.opendaylight.controller.config.persist.storage.file.xml.XmlFileStorageAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/*
 * Simple class to push configs to the config subsystem from Feature's configfiles
 */
public class FeatureConfigPusher {
    private static final Logger LOG = LoggerFactory.getLogger(FeatureConfigPusher.class);
    private static final int MAX_RETRIES = 100;
    private static final int RETRY_PAUSE_MILLIS = 1;

    private FeaturesService featuresService = null;
    private ConfigPusher pusher = null;

    /*
     * A LinkedHashSet (to preserve order and insure uniqueness) of the pushedConfigs
     * This is used to prevent pushing duplicate configs if a Feature is in multiple dependency
     * chains.  Also, preserves the *original* Feature chain for which we pushed the config.
     * (which is handy for logging).
     */
    private final Set<FeatureConfigSnapshotHolder> pushedConfigs = new LinkedHashSet<>();

    /*
     * LinkedHashMultimap to track which configs we pushed for each Feature installation
     * For future use
     */
    private final LinkedHashMultimap<Feature, FeatureConfigSnapshotHolder> feature2configs = LinkedHashMultimap
            .create();

    public FeatureConfigPusher(final ConfigPusher configPusher, final FeaturesService featuresService) {
        pusher = configPusher;
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
    public LinkedHashMultimap<Feature, FeatureConfigSnapshotHolder> pushConfigs(
            final List<Feature> features) throws Exception {
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
        // Ignore feature conditions — these encode conditions on other features and shouldn't be processed here
        if (feature.getName().contains("-condition-")) {
            LOG.debug("Ignoring conditional feature {}", feature);
            return Collections.emptySet();
        }
        // pax-exam's Karaf container generates a wrapper feature holding the test dependencies. Ignore it.
        if ("test-dependencies".equals(feature.getName())) {
            LOG.debug("Ignoring pax-exam wrapper feature {}", feature);
            return Collections.emptySet();
        }

        if (!isInstalled(feature)) {
            return Collections.emptySet();
        }
        // FIXME Workaround for BUG-2836, features service returns null for feature:
        // standard-condition-webconsole_0_0_0, 3.0.1
        if (featuresService.getFeature(feature.getName(), feature.getVersion()) == null) {
            LOG.debug("Feature: {}, {} is missing from features service. Skipping", feature.getName(), feature
                    .getVersion());
            return Collections.emptySet();
        }

        ChildAwareFeatureWrapper wrappedFeature = new ChildAwareFeatureWrapper(feature, featuresService);
        Set<FeatureConfigSnapshotHolder> configs = wrappedFeature.getFeatureConfigSnapshotHolders();
        if (!configs.isEmpty()) {
            configs = pushConfig(configs, feature);
            feature2configs.putAll(feature, configs);
        }
        return configs;
    }

    private Set<FeatureConfigSnapshotHolder> pushConfig(final Set<FeatureConfigSnapshotHolder> configs,
                                                        final Feature feature) throws InterruptedException {
        Set<FeatureConfigSnapshotHolder> configsToPush = new LinkedHashSet<>(configs);
        configsToPush.removeAll(pushedConfigs);
        if (!configsToPush.isEmpty()) {

            // Ignore features that are present in persisted current config
            final Optional<XmlFileStorageAdapter> currentCfgPusher = XmlFileStorageAdapter.getInstance();
            if (currentCfgPusher.isPresent() && currentCfgPusher.get().getPersistedFeatures()
                    .contains(feature.getId())) {
                LOG.warn("Ignoring default configuration {} for feature {}, the configuration is present in {}",
                        configsToPush, feature.getId(), currentCfgPusher.get());
            } else {
                pusher.pushConfigs(new ArrayList<>(configsToPush));
            }
            pushedConfigs.addAll(configsToPush);
        }
        Set<FeatureConfigSnapshotHolder> configsPushed = new LinkedHashSet<>(pushedConfigs);
        configsPushed.retainAll(configs);
        return configsPushed;
    }

    @SuppressWarnings("IllegalCatch")
    private boolean isInstalled(final Feature feature) throws InterruptedException {
        for (int retries = 0; retries < MAX_RETRIES; retries++) {
            try {
                List<Feature> installedFeatures = Arrays.asList(featuresService.listInstalledFeatures());
                if (installedFeatures.contains(feature)) {
                    return true;
                }

                LOG.debug("Karaf Feature Service has not yet finished installing feature {}/{} (retry {})", feature
                        .getName(), feature.getVersion(), retries);
            } catch (final Exception e) {
                LOG.warn("Karaf featuresService.listInstalledFeatures() has thrown an exception, retry {}", retries, e);
            }

            TimeUnit.MILLISECONDS.sleep(RETRY_PAUSE_MILLIS);
        }

        // In karaf  4.1.3 we see this error logged for 2 system features, "jaas-boot" and "wrap", many times. It's
        // unclear why listInstalledFeatures doesn't return them but it doesn't really matter since we're only
        // interested in ODL features that have CSS files. Maybe the common denominator is that they don't have a
        // version (ie it's 0.0.0) hence the following check to avoid logging the error. This check would not
        // exclude any ODL feature since all ODL features are versioned (should be anyway).
        if (!"0.0.0".equals(feature.getVersion())) {
            LOG.error("Giving up (after {} retries) on Karaf featuresService.listInstalledFeatures() which has not yet "
                    + "finished installing feature {} {}", MAX_RETRIES, feature.getName(), feature.getVersion());
        }
        return false;
    }
}
