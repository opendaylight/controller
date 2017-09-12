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
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

import org.apache.karaf.features.Feature;
import org.apache.karaf.features.FeatureEvent;
import org.apache.karaf.features.FeatureEvent.EventType;
import org.apache.karaf.features.FeaturesService;
import org.opendaylight.controller.config.persist.api.ConfigPusher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConfigPushingRunnable implements Runnable {
    private static final Logger LOG = LoggerFactory.getLogger(ConfigPushingRunnable.class);
    private static final int POLL_TIME = 1;
    private BlockingQueue<FeatureEvent> queue;
    private FeatureConfigPusher configPusher;

    public ConfigPushingRunnable(final ConfigPusher configPusher, final FeaturesService featuresService,
                                 final BlockingQueue<FeatureEvent> featureEvents) {
        queue = featureEvents;
        this.configPusher = new FeatureConfigPusher(configPusher, featuresService);
    }

    @Override
    @SuppressWarnings("IllegalCatch")
    public void run() {
        List<Feature> toInstall = new ArrayList<>();
        FeatureEvent event = null;
        boolean interrupted = false;
        while (true) {
            try {
                if (!interrupted) {
                    if (toInstall.isEmpty()) {
                        event = queue.take();
                    } else {
                        event = queue.poll(POLL_TIME, TimeUnit.MILLISECONDS);
                    }
                    if (event != null && event.getFeature() != null) {
                        processFeatureEvent(event, toInstall);
                    }
                } else if (toInstall.isEmpty()) {
                    LOG.error("ConfigPushingRunnable - exiting");
                    return;
                }
            } catch (final InterruptedException e) {
                LOG.error("ConfigPushingRunnable - interrupted");
                interrupted = true;
            } catch (final Exception e) {
                LOG.error("Exception while processing features {} event {}", toInstall, event, e);
            }
        }
    }

    protected void processFeatureEvent(final FeatureEvent event, final List<Feature> toInstall) throws Exception {
        if (event.getType() == EventType.FeatureInstalled) {
            toInstall.add(event.getFeature());
            LinkedHashMultimap<Feature, FeatureConfigSnapshotHolder> result = configPusher.pushConfigs(toInstall);
            toInstall.removeAll(result.keySet());
        } else if (event.getType() == EventType.FeatureUninstalled) {
            toInstall.remove(event.getFeature());
        }
    }

    protected void logPushResult(final LinkedHashMultimap<Feature, FeatureConfigSnapshotHolder> results) {
        for (Feature f : results.keySet()) {
            LOG.info("Pushed configs for feature {} {}", f, results.get(f));
        }
    }
}
