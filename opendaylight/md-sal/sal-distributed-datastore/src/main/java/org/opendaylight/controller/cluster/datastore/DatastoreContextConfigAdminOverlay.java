/*
 * Copyright (c) 2015 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore;

import java.io.IOException;
import java.util.Dictionary;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class that overlays DatastoreContext settings with settings obtained from an OSGi Config Admin
 * service.
 *
 * @author Thomas Pantelis
 */
public class DatastoreContextConfigAdminOverlay implements AutoCloseable {
    public static final String CONFIG_ID = "org.opendaylight.controller.cluster.datastore";

    private static final Logger LOG = LoggerFactory.getLogger(DatastoreContextConfigAdminOverlay.class);

    private final DatastoreContextIntrospector introspector;
    private final BundleContext bundleContext;

    public DatastoreContextConfigAdminOverlay(DatastoreContextIntrospector introspector, BundleContext bundleContext) {
        this.introspector = introspector;
        this.bundleContext = bundleContext;

        ServiceReference<ConfigurationAdmin> configAdminServiceReference =
                bundleContext.getServiceReference(ConfigurationAdmin.class);
        if(configAdminServiceReference == null) {
            LOG.warn("No ConfigurationAdmin service found");
        } else {
            overlaySettings(configAdminServiceReference);
        }
    }

    private void overlaySettings(ServiceReference<ConfigurationAdmin> configAdminServiceReference) {
        try {
            ConfigurationAdmin configAdmin = bundleContext.getService(configAdminServiceReference);

            Configuration config = configAdmin.getConfiguration(CONFIG_ID);
            if(config != null) {
                Dictionary<String, Object> properties = config.getProperties();

                LOG.debug("Overlaying settings: {}", properties);

                introspector.update(properties);
            } else {
                LOG.debug("No Configuration found for {}", CONFIG_ID);
            }
        } catch (IOException e) {
            LOG.error("Error obtaining Configuration for pid {}", CONFIG_ID, e);
        } catch(IllegalStateException e) {
            // Ignore - indicates the bundleContext has been closed.
        } finally {
            try {
                bundleContext.ungetService(configAdminServiceReference);
            } catch (Exception e) {
                LOG.debug("Error from ungetService", e);
            }
        }
    }

    @Override
    public void close() {
    }
}
