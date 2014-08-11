/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.configpusherfeature.internal;

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

import com.google.common.collect.LinkedHashMultimap;

public class ConfigPushingRunnable implements Runnable {
    private static final Logger logger = LoggerFactory.getLogger(ConfigPushingRunnable.class);
    private static final int POLL_TIME = 1;
    private BlockingQueue<FeatureEvent> queue;
    private FeatureConfigPusher configPusher;
    public ConfigPushingRunnable(ConfigPusher p, FeaturesService f,BlockingQueue<FeatureEvent> q) {
        queue = q;
        configPusher = new FeatureConfigPusher(p, f);
    }

    @Override
    public void run() {
        List<Feature> toInstall = new ArrayList<Feature>();
        FeatureEvent event;
        boolean interuppted = false;
        while(true) {
            try {
                if(!interuppted) {
                        if(toInstall.isEmpty()) {
                            event = queue.take();
                        } else {
                            event = queue.poll(POLL_TIME, TimeUnit.MILLISECONDS);
                        }
                        if(event != null && event.getFeature() !=null) {
                            processFeatureEvent(event,toInstall);
                        }
                } else if(toInstall.isEmpty()) {
                    logger.error("ConfigPushingRunnable - exiting");
                    return;
                }
            } catch (InterruptedException e) {
                logger.error("ConfigPushingRunnable - interupted");
                interuppted = true;
            } catch (Exception e) {
                logger.error("Exception while processing features {}", e);
            }
        }
    }

    protected void processFeatureEvent(FeatureEvent event, List<Feature> toInstall) throws InterruptedException, Exception {
        if(event.getType() == EventType.FeatureInstalled) {
            toInstall.add(event.getFeature());
            LinkedHashMultimap<Feature,FeatureConfigSnapshotHolder> result = configPusher.pushConfigs(toInstall);
            toInstall.removeAll(result.keySet());
        } else if(event.getType() == EventType.FeatureUninstalled) {
            toInstall.remove(event.getFeature());
        }
    }

    protected void logPushResult(LinkedHashMultimap<Feature,FeatureConfigSnapshotHolder> results) {
        for(Feature f:results.keySet()) {
            logger.info("Pushed configs for feature {} {}",f,results.get(f));
        }
    }
}
