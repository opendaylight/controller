package org.opendaylight.controller.featuresservice;

import org.apache.karaf.features.FeaturesService;
import org.opendaylight.controller.featuresservice.northbound.FeaturesServiceNorthbound;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;

public class Activator implements BundleActivator {
   private ServiceRegistration registration;

   @Override
   public void start(BundleContext context) throws Exception {
      ServiceReference[] references =
                context.getServiceReferences(FeaturesService.class.getName(), null);
      FeaturesService service = (FeaturesService)context.getService(references[0]);
      FeaturesServiceNorthbound cnb = new FeaturesServiceNorthbound(service);
      registration = context.registerService(
      FeaturesServiceNorthbound.class.getName(), cnb, null);
   }

   @Override
   public void stop(BundleContext context) throws Exception {
      registration.unregister();
   }
}
