package org.opendaylight.controller.configpusherfeature.internal;

import org.apache.karaf.features.FeaturesListener;
import org.apache.karaf.features.FeaturesService;
import org.opendaylight.controller.config.persist.api.ConfigPusher;
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
        FeaturesService featureService = bc.getService(reference);
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
