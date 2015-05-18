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
import org.opendaylight.controller.config.persist.storage.file.xml.FeatureListProvider;
import org.opendaylight.controller.config.persist.storage.file.xml.XmlFileStorageAdapter;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

public class FeatureServiceCustomizer implements ServiceTrackerCustomizer<FeaturesService, FeaturesService>, AutoCloseable {
    private ConfigPusher configPusher = null;
    private ConfigFeaturesListener configFeaturesListener = null;
    private ServiceRegistration<?> registration;

    FeatureServiceCustomizer(ConfigPusher c) {
        configPusher = c;
    }


    @Override
    public FeaturesService addingService(ServiceReference<FeaturesService> reference) {
        BundleContext bc = reference.getBundle().getBundleContext();
        final FeaturesService featureService = bc.getService(reference);
        final Optional<XmlFileStorageAdapter> currentPersister = XmlFileStorageAdapter.getInstance();

        if(XmlFileStorageAdapter.getInstance().isPresent()) {
            final Set<String> installedFeatureIds = Sets.newHashSet();
            for (final Feature installedFeature : featureService.listInstalledFeatures()) {
                installedFeatureIds.add(installedFeature.getId());
            }

            currentPersister.get().setFeaturesService(new FeatureListProvider() {
                @Override
                public Set<String> listFeatures() {
                    return installedFeatureIds;
                }
            });
        }
        configFeaturesListener = new ConfigFeaturesListener(configPusher,featureService);
        registration = bc.registerService(FeaturesListener.class.getCanonicalName(), configFeaturesListener, null);
        return featureService;
    }

    @Override
    public void modifiedService(ServiceReference<FeaturesService> reference,
            FeaturesService service) {
        // we don't care if the properties change

    }

    @Override
    public void removedService(ServiceReference<FeaturesService> reference,
            FeaturesService service) {
        close();
    }

    @Override
    public void close() {
        if(registration != null) {
            registration.unregister();
            registration = null;
        }
    }

}
