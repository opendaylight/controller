package org.opendaylight.controller.featuretracker;

import org.apache.karaf.features.FeaturesListener;
import org.apache.karaf.features.FeaturesService;
import org.opendaylight.controller.netconf.persist.impl.ConfigPusher;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Activator implements BundleActivator {

    private static final Logger logger = LoggerFactory.getLogger(Activator.class);
    ServiceRegistration<?> registration;
    BundleContext bc = null;
    ServiceTracker fsst = null;
    ServiceTracker cpst = null;

    public void start(BundleContext context) throws Exception {
        bc = context;
        // Using ServiceTrackers to handle ServiceEvents, avoiding boiler plate code.
        fsst = new ServiceTracker(bc, FeaturesService.class.getName(), null);
        fsst.open();

        ConfigPusherCustomizer cpc = new ConfigPusherCustomizer();
        cpst = new ServiceTracker(bc, ConfigPusher.class.getName(), cpc);
        cpst.open();

        ServiceReference<FeaturesService> featureref = (ServiceReference<FeaturesService>) fsst.getServiceReference();
        ServiceReference<ConfigPusher> configref = (ServiceReference<ConfigPusher>) cpst.getServiceReference();

        if (featureref == null) {
            System.out.println("FeatureTracker Could not get feature service reference.");
            logger.warn("Could not get feature service reference!");
        }
        if (configref == null) {
            System.out.println("FeatureTracker Could not get config pusher service reference.");
            logger.warn("Could not get config pusher service reference!");
        }

        FeaturesListener configFeaturesListener = new ConfigFeaturesListener(context.getService(featureref),context.getService(configref));
        registration = context.registerService(FeaturesListener.class.getCanonicalName(), configFeaturesListener, null);
    }

    public void stop(BundleContext context) throws Exception {
        if(registration != null) {
            registration.unregister();
        }
        if(fsst != null) {
            fsst.close();
        }
        if(cpst != null) {
            cpst.close();
        }
        bc = null;
    }

    class ConfigPusherCustomizer implements ServiceTrackerCustomizer<ConfigPusher, ConfigPusher> {

        ConfigPusherCustomizer() {
        }

        @Override
        public ConfigPusher addingService(ServiceReference<ConfigPusher> configPusherServiceReference) {
            logger.trace("Got ConfigPusherCustomizer.addingService {}", configPusherServiceReference);
            ConfigPusher cpService = configPusherServiceReference.getBundle().getBundleContext().getService(configPusherServiceReference);
            return cpService;
        }

        @Override
        public void modifiedService(ServiceReference<ConfigPusher> configPusherServiceReference, ConfigPusher configPusher) {
        }

        @Override
        public void removedService(ServiceReference<ConfigPusher> configPusherServiceReference, ConfigPusher configPusher) {
        }
    }
}
