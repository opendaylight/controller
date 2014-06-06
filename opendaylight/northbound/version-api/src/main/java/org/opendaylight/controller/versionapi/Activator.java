package org.opendaylight.controller.versionapi;

import java.util.Hashtable;
import org.opendaylight.controller.versionapi.northbound.ConfigUpdater;
import org.opendaylight.controller.versionapi.northbound.VersionapiNorthbound;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.framework.Constants;
import org.osgi.service.cm.ManagedService;

public class Activator implements BundleActivator {
   private ServiceRegistration registration;
   private static final String CONFIG_PID = "version.release";

   @Override
   public void start(BundleContext context) throws Exception {
      // Open distribution.properties
      Hashtable<String, Object> properties = new Hashtable<String, Object>();
      properties.put(Constants.SERVICE_PID, CONFIG_PID);

      ConfigUpdater cu = new ConfigUpdater();
      registration = context.registerService(
      ManagedService.class.getName(), cu, properties);

      VersionapiNorthbound cnb = new VersionapiNorthbound();
      cnb.setConfigUpdater(cu);
      registration = context.registerService(
      VersionapiNorthbound.class.getName(), cnb, null);
   }

   @Override
   public void stop(BundleContext context) throws Exception {
      registration.unregister();
   }
}
