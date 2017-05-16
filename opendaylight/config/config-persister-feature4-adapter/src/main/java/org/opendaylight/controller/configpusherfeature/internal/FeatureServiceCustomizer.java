/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.configpusherfeature.internal;

import com.google.common.base.Optional;
import com.google.common.collect.Sets;

import java.util.Set;

import org.apache.karaf.features.Feature;
import org.apache.karaf.features.FeaturesListener;
import org.apache.karaf.features.FeaturesService;
import org.opendaylight.controller.config.persist.api.ConfigPusher;
import org.opendaylight.controller.config.persist.storage.file.xml.XmlFileStorageAdapter;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.util.tracker.ServiceTrackerCustomizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FeatureServiceCustomizer implements ServiceTrackerCustomizer<FeaturesService, FeaturesService>, AutoCloseable {
    private static final Logger LOG = LoggerFactory.getLogger(FeatureServiceCustomizer.class);
    private ConfigPusher configPusher = null;
    private ServiceRegistration<?> registration;

    FeatureServiceCustomizer(final ConfigPusher c) {
        configPusher = c;
    }

    @Override
    public FeaturesService addingService(final ServiceReference<FeaturesService> reference) {
        BundleContext bc = reference.getBundle().getBundleContext();
        final FeaturesService featureService = bc.getService(reference);
        final Optional<XmlFileStorageAdapter> currentPersister = XmlFileStorageAdapter.getInstance();

        if (XmlFileStorageAdapter.getInstance().isPresent()) {
            final Set<String> installedFeatureIds = Sets.newHashSet();
            try {
                for (final Feature installedFeature : featureService.listInstalledFeatures()) {
                    installedFeatureIds.add(installedFeature.getId());
                }
            } catch (final Exception e) {
                LOG.error("Error listing installed features", e);
            }

            currentPersister.get().setFeaturesService(() -> installedFeatureIds);
        }
        ConfigFeaturesListener configFeaturesListener = new ConfigFeaturesListener(configPusher, featureService);
        registration = bc.registerService(FeaturesListener.class.getCanonicalName(), configFeaturesListener, null);
        return featureService;
    }

    @Override
    public void modifiedService(final ServiceReference<FeaturesService> reference,
                                final FeaturesService service) {
        // we don't care if the properties change
    }

    @Override
    public void removedService(final ServiceReference<FeaturesService> reference,
                               final FeaturesService service) {
        close();
    }

    @Override
    public void close() {
        if (registration != null) {
            registration.unregister();
            registration = null;
        }
    }
}
