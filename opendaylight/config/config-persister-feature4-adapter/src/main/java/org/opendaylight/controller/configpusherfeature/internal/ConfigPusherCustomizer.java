/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.configpusherfeature.internal;

import org.apache.karaf.features.FeaturesService;
import org.opendaylight.controller.config.persist.api.ConfigPusher;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConfigPusherCustomizer implements ServiceTrackerCustomizer<ConfigPusher, ConfigPusher>, AutoCloseable {
    private static final Logger LOG = LoggerFactory.getLogger(ConfigPusherCustomizer.class);
    private ConfigFeaturesListener configFeaturesListener = null;
    private FeatureServiceCustomizer featureServiceCustomizer = null;
    private ServiceTracker<FeaturesService,FeaturesService> fsst = null;

    @Override
    public ConfigPusher addingService(ServiceReference<ConfigPusher> configPusherServiceReference) {
        LOG.trace("Got ConfigPusherCustomizer.addingService {}", configPusherServiceReference);
        BundleContext bc = configPusherServiceReference.getBundle().getBundleContext();
        ConfigPusher cpService = bc.getService(configPusherServiceReference);
        featureServiceCustomizer = new FeatureServiceCustomizer(cpService);
        fsst = new ServiceTracker<>(bc, FeaturesService.class.getName(), featureServiceCustomizer);
        fsst.open();
        return cpService;
    }

    @Override
    public void modifiedService(ServiceReference<ConfigPusher> configPusherServiceReference, ConfigPusher configPusher) {
        // we don't care if the properties change
    }

    @Override
    public void removedService(ServiceReference<ConfigPusher> configPusherServiceReference, ConfigPusher configPusher) {
        this.close();
    }

    @Override
    public void close() {
        if(fsst != null) {
            fsst.close();
            fsst = null;
        }
        if(configFeaturesListener != null) {
            configFeaturesListener.close();
            configFeaturesListener = null;
        }
        if(featureServiceCustomizer != null) {
            featureServiceCustomizer.close();
            featureServiceCustomizer = null;
        }
    }
}