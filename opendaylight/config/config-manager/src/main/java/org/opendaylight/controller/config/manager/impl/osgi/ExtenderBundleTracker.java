/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.config.manager.impl.osgi;

import static java.lang.String.format;

import java.io.InputStream;
import java.net.URL;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.opendaylight.controller.config.spi.ModuleFactory;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.ServiceRegistration;
import org.osgi.util.tracker.BundleTracker;
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
public class ExtenderBundleTracker extends BundleTracker<Object> {

    private static final Logger logger = LoggerFactory.getLogger(ExtenderBundleTracker.class);

    public ExtenderBundleTracker(BundleContext context) {
        super(context, Bundle.ACTIVE, null);
        logger.trace("Registered as extender with context {}", context);
    }

    @Override
    public Object addingBundle(Bundle bundle, BundleEvent event) {
        URL resource = bundle.getEntry("META-INF/services/" + ModuleFactory.class.getName());
        logger.trace("Got addingBundle event of bundle {}, resource {}, event {}",
                bundle, resource, event);
        if (resource != null) {
            try (InputStream inputStream = resource.openStream()) {
                List<String> lines = IOUtils.readLines(inputStream);
                for (String factoryClassName : lines) {
                    registerFactory(factoryClassName, bundle);
                }
            } catch (Exception e) {
                logger.error("Error while reading {}", resource, e);
                throw new RuntimeException(e);
            }
        }
        return bundle;
    }

    @Override
    public void removedBundle(Bundle bundle, BundleEvent event, Object object) {
        super.removedBundle(bundle,event,object);
    }

    // TODO:test
    private static ServiceRegistration<?> registerFactory(String factoryClassName, Bundle bundle) {
        String errorMessage;
        try {
            Class<?> clazz = bundle.loadClass(factoryClassName);
            if (ModuleFactory.class.isAssignableFrom(clazz)) {
                try {
                    logger.debug("Registering {} in bundle {}",
                            clazz.getName(), bundle);
                    return bundle.getBundleContext().registerService(
                            ModuleFactory.class.getName(), clazz.newInstance(),
                            null);
                } catch (InstantiationException e) {
                    errorMessage = logMessage(
                            "Could not instantiate {} in bundle {}, reason {}",
                            factoryClassName, bundle, e);
                } catch (IllegalAccessException e) {
                    errorMessage = logMessage(
                            "Illegal access during instatiation of class {} in bundle {}, reason {}",
                            factoryClassName, bundle, e);
                }
            } else {
                errorMessage = logMessage(
                        "Class {} does not implement {} in bundle {}", clazz,
                        ModuleFactory.class, bundle);
            }
        } catch (ClassNotFoundException e) {
            errorMessage = logMessage(
                    "Could not find class {} in bunde {}, reason {}",
                    factoryClassName, bundle, e);
        }
        throw new IllegalStateException(errorMessage);
    }

    public static String logMessage(String slfMessage, Object... params) {
        logger.info(slfMessage, params);
        String formatMessage = slfMessage.replaceAll("\\{\\}", "%s");
        return format(formatMessage, params);
    }
}
