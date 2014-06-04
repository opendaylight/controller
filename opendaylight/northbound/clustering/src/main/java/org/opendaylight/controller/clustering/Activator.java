package org.opendaylight.controller.clustering;

import org.opendaylight.controller.clustering.northbound.ClusteringNorthbound;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

public class Activator implements BundleActivator {
   private ServiceRegistration registration;

   @Override
   public void start(BundleContext context) throws Exception {
      ClusteringNorthbound cnb = new ClusteringNorthbound();
      registration = context.registerService(
      ClusteringNorthbound.class.getName(), cnb, null);
   }

   @Override
   public void stop(BundleContext context) throws Exception {
      registration.unregister();
   }
}
