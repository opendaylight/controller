/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.config.manager.impl.osgi;

import static java.lang.String.format;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Charsets;
import com.google.common.io.Resources;
import java.io.IOException;
import java.net.URL;
import org.opendaylight.controller.config.spi.ModuleFactory;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.ServiceRegistration;
import org.osgi.util.tracker.BundleTrackerCustomizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * OSGi extender that listens for bundle activation events. Reads file
 * META-INF/services/org.opendaylight.controller.config.spi.ModuleFactory, each
 * line should contain an implementation of ModuleFactory interface. Creates new
 * instance with default constructor and registers it into OSGi service
 * registry. There is no need for listening for implementing removedBundle as
 * the services are unregistered automatically.
 * Code based on http://www.toedter.com/blog/?p=236
 */
public class ModuleFactoryBundleTracker implements BundleTrackerCustomizer<Object> {
    private final BlankTransactionServiceTracker blankTransactionServiceTracker;
    private static final Logger LOG = LoggerFactory.getLogger(ModuleFactoryBundleTracker.class);

    public ModuleFactoryBundleTracker(BlankTransactionServiceTracker blankTransactionServiceTracker) {
        this.blankTransactionServiceTracker = blankTransactionServiceTracker;
    }

    @Override
    public Object addingBundle(Bundle bundle, BundleEvent event) {
        URL resource = bundle.getEntry("META-INF/services/" + ModuleFactory.class.getName());
        LOG.trace("Got addingBundle event of bundle {}, resource {}, event {}",
                bundle, resource, event);
        if (resource != null) {
            try {
                for (String factoryClassName : Resources.readLines(resource, Charsets.UTF_8)) {
                    registerFactory(factoryClassName, bundle);
                }
            } catch (IOException e) {
                LOG.error("Error while reading {}", resource, e);
                throw new RuntimeException(e);
            }
        }
        return bundle;
    }

    @Override
    public void modifiedBundle(Bundle bundle, BundleEvent event, Object object) {
        // NOOP
    }

    @Override
    public void removedBundle(Bundle bundle, BundleEvent event, Object object) {
        // workaround for service tracker not getting removed service event
        blankTransactionServiceTracker.blankTransaction();
    }

    @VisibleForTesting
    protected static ServiceRegistration<?> registerFactory(String factoryClassName, Bundle bundle) {
        String errorMessage;
        Exception ex = null;
        try {
            Class<?> clazz = bundle.loadClass(factoryClassName);
            if (ModuleFactory.class.isAssignableFrom(clazz)) {
                try {
                    LOG.debug("Registering {} in bundle {}",
                            clazz.getName(), bundle);
                    return bundle.getBundleContext().registerService(
                            ModuleFactory.class.getName(), clazz.newInstance(),
                            null);
                } catch (InstantiationException e) {
                    errorMessage = logMessage(
                            "Could not instantiate {} in bundle {}, reason {}",
                            factoryClassName, bundle, e);
                    ex = e;
                } catch (IllegalAccessException e) {
                    errorMessage = logMessage(
                            "Illegal access during instantiation of class {} in bundle {}, reason {}",
                            factoryClassName, bundle, e);
                    ex = e;
                }
            } else {
                errorMessage = logMessage(
                        "Class {} does not implement {} in bundle {}", clazz,
                        ModuleFactory.class, bundle);
            }
        } catch (ClassNotFoundException e) {
            errorMessage = logMessage(
                    "Could not find class {} in bundle {}, reason {}",
                    factoryClassName, bundle, e);
            ex = e;
        }

        throw ex == null ? new IllegalStateException(errorMessage) : new IllegalStateException(errorMessage, ex);
    }

    public static String logMessage(String slfMessage, Object... params) {
        LOG.info(slfMessage, params);
        String formatMessage = slfMessage.replaceAll("\\{\\}", "%s");
        return format(formatMessage, params);
    }
}
