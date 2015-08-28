/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.configpusherfeature.internal;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import org.apache.karaf.features.FeatureEvent;
import org.apache.karaf.features.FeaturesListener;
import org.apache.karaf.features.FeaturesService;
import org.apache.karaf.features.RepositoryEvent;
import org.opendaylight.controller.config.persist.api.ConfigPusher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConfigFeaturesListener implements  FeaturesListener,  AutoCloseable {
    private static final Logger LOG = LoggerFactory.getLogger(ConfigFeaturesListener.class);
    private static final int QUEUE_SIZE = 1000;
    private BlockingQueue<FeatureEvent> queue = new LinkedBlockingQueue<>(QUEUE_SIZE);
    Thread pushingThread = null;

    public ConfigFeaturesListener(ConfigPusher p, FeaturesService f) {
        pushingThread = new Thread(new ConfigPushingRunnable(p, f, queue), "ConfigFeatureListener - ConfigPusher");
        pushingThread.start();
    }

    @Override
    public void featureEvent(FeatureEvent event) {
        queue.offer(event);
    }

    @Override
    public void repositoryEvent(RepositoryEvent event) {
        LOG.debug("Repository: {} {}", event.getType(), event.getRepository());
    }

    @Override
    public void close() {
        if(pushingThread != null) {
            pushingThread.interrupt();
            pushingThread = null;
        }
    }
}
