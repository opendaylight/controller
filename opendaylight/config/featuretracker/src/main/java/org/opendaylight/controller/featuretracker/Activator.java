package org.opendaylight.controller.featuretracker;

import org.apache.karaf.features.FeaturesListener;
import org.apache.karaf.features.FeaturesService;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;

public class Activator implements BundleActivator {
    ServiceRegistration<?> registration;

    public void start(BundleContext context) throws Exception {
        ServiceReference<FeaturesService> featureref = context.getServiceReference(FeaturesService.class);
        ServiceReference<FeaturesService> configref = context.getServiceReference(ConfigPusher.class);
        FeaturesListener configFeaturesListener = new ConfigFeaturesListener(context.getService(featureref),context.getService(configref));
        registration = context.registerService(FeaturesListener.class.getCanonicalName(), configFeaturesListener, null);
    }

    public void stop(BundleContext context) throws Exception {
        if(registration != null) {
            registration.unregister();
        }
    }
}
