package org.opendaylight.controller.networkconfig.neutron.northbound;

import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NeutronUtil {
    private static final Logger logger = LoggerFactory
            .getLogger(NeutronUtil.class);
    public static Object[] getInstances(Class<?> clazz,Object bundle) {
        Object instances[] = null;
        try {
            BundleContext bCtx = FrameworkUtil.getBundle(bundle.getClass())
                    .getBundleContext();

            ServiceReference<?>[] services = null;
                services = bCtx.getServiceReferences(clazz.getName(),
                        null);
            if (services != null) {
                instances = new Object[services.length];
                for (int i = 0; i < services.length; i++) {
                    instances[i] = bCtx.getService(services[i]);
                }
            }
        } catch (Exception e) {
            logger.error("Instance reference is NULL");
        }
        return instances;
    }
}
