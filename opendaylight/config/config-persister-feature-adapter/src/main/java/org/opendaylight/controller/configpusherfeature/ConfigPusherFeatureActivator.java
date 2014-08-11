package org.opendaylight.controller.configpusherfeature;

import org.opendaylight.controller.config.persist.api.ConfigPusher;
import org.opendaylight.controller.configpusherfeature.internal.ConfigPusherCustomizer;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.util.tracker.ServiceTracker;

public class ConfigPusherFeatureActivator implements BundleActivator {

    BundleContext bc = null;
    ConfigPusherCustomizer cpc = null;
    ServiceTracker<ConfigPusher,ConfigPusher> cpst = null;

    public void start(BundleContext context) throws Exception {
        bc = context;
        cpc = new ConfigPusherCustomizer();
        cpst = new ServiceTracker<ConfigPusher, ConfigPusher>(bc, ConfigPusher.class.getName(), cpc);
        cpst.open();
    }

    public void stop(BundleContext context) throws Exception {
        if(cpst != null) {
            cpst.close();
            cpst = null;
        }
        if(cpc != null) {
            cpc.close();
            cpc = null;
        }
        bc = null;
    }
}
