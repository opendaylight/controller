/*
 * Copyright (c) 2013, 2017 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.config.manager.impl.osgi.mapping;

import com.google.common.io.Resources;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import org.opendaylight.yangtools.concepts.ObjectRegistration;
import org.opendaylight.yangtools.yang.binding.YangModelBindingProvider;
import org.opendaylight.yangtools.yang.binding.YangModuleInfo;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleEvent;
import org.osgi.util.tracker.BundleTracker;
import org.osgi.util.tracker.BundleTrackerCustomizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Tracks bundles and attempts to retrieve YangModuleInfo, which is then fed
 * into ModuleInfoRegistry.
 */
public final class ModuleInfoBundleTracker
        implements AutoCloseable, BundleTrackerCustomizer<Collection<ObjectRegistration<YangModuleInfo>>> {

    private static final Logger LOG = LoggerFactory.getLogger(ModuleInfoBundleTracker.class);

    public static final String MODULE_INFO_PROVIDER_PATH_PREFIX = "META-INF/services/";
    private static final String YANG_MODULE_INFO_SERVICE_PATH = MODULE_INFO_PROVIDER_PATH_PREFIX
            + YangModelBindingProvider.class.getName();

    private final RefreshingSCPModuleInfoRegistry moduleInfoRegistry;

    private BundleTracker<Collection<ObjectRegistration<YangModuleInfo>>> bundleTracker;
    private boolean starting;

    public ModuleInfoBundleTracker(final RefreshingSCPModuleInfoRegistry moduleInfoRegistry) {
        this.moduleInfoRegistry = moduleInfoRegistry;
    }

    public void open(final BundleTracker<Collection<ObjectRegistration<YangModuleInfo>>> bundleTracker) {
        LOG.debug("ModuleInfoBundleTracker open starting with bundleTracker {}", bundleTracker);

        if (bundleTracker != null) {
            this.bundleTracker = bundleTracker;
            starting = true;
            bundleTracker.open();

            starting = false;
            moduleInfoRegistry.updateService();
        } else {
            starting = false;
        }

        LOG.debug("ModuleInfoBundleTracker open complete");
    }

    @Override
    public void close() {
        if (bundleTracker != null) {
            bundleTracker.close();
            bundleTracker = null;
        }
    }

    @Override
    @SuppressWarnings("IllegalCatch")
    public Collection<ObjectRegistration<YangModuleInfo>> addingBundle(final Bundle bundle, final BundleEvent event) {
        URL resource = bundle.getEntry(YANG_MODULE_INFO_SERVICE_PATH);
        LOG.debug("Got addingBundle({}) with YangModelBindingProvider resource {}", bundle, resource);
        if (resource == null) {
            return Collections.emptyList();
        }
        List<ObjectRegistration<YangModuleInfo>> registrations = new LinkedList<>();

        try {
            for (String moduleInfoName : Resources.readLines(resource, StandardCharsets.UTF_8)) {
                LOG.trace("Retrieve ModuleInfo({}, {})", moduleInfoName, bundle);
                YangModuleInfo moduleInfo = retrieveModuleInfo(moduleInfoName, bundle);
                registrations.add(moduleInfoRegistry.registerModuleInfo(moduleInfo));
            }

            if (!starting) {
                moduleInfoRegistry.updateService();
            }
        } catch (final IOException e) {
            LOG.error("Error while reading {} from bundle {}", resource, bundle, e);
        } catch (final RuntimeException e) {
            LOG.error("Failed to process {} for bundle {}", resource, bundle, e);
        }

        LOG.trace("Got following registrations {}", registrations);
        return registrations;
    }

    @Override
    public void modifiedBundle(final Bundle bundle, final BundleEvent event,
            final Collection<ObjectRegistration<YangModuleInfo>> object) {
    }

    @Override
    @SuppressWarnings("IllegalCatch")
    public void removedBundle(final Bundle bundle, final BundleEvent event,
            final Collection<ObjectRegistration<YangModuleInfo>> regs) {
        if (regs == null) {
            return;
        }

        for (ObjectRegistration<YangModuleInfo> reg : regs) {
            try {
                reg.close();
            } catch (final Exception e) {
                LOG.error("Unable to unregister YangModuleInfo {}", reg.getInstance(), e);
            }
        }
    }

    private static YangModuleInfo retrieveModuleInfo(final String moduleInfoClass, final Bundle bundle) {
        String errorMessage;
        Class<?> clazz = loadClass(moduleInfoClass, bundle);

        if (!YangModelBindingProvider.class.isAssignableFrom(clazz)) {
            errorMessage = logMessage("Class {} does not implement {} in bundle {}", clazz,
                    YangModelBindingProvider.class, bundle);
            throw new IllegalStateException(errorMessage);
        }
        final YangModelBindingProvider instance;
        try {
            Object instanceObj = clazz.newInstance();
            instance = YangModelBindingProvider.class.cast(instanceObj);
        } catch (final InstantiationException e) {
            errorMessage = logMessage("Could not instantiate {} in bundle {}, reason {}", moduleInfoClass, bundle, e);
            throw new IllegalStateException(errorMessage, e);
        } catch (final IllegalAccessException e) {
            errorMessage = logMessage("Illegal access during instantiation of class {} in bundle {}, reason {}",
                    moduleInfoClass, bundle, e);
            throw new IllegalStateException(errorMessage, e);
        }

        try {
            return instance.getModuleInfo();
        } catch (NoClassDefFoundError | ExceptionInInitializerError e) {
            throw new IllegalStateException("Error while executing getModuleInfo on " + instance, e);
        }
    }

    private static Class<?> loadClass(final String moduleInfoClass, final Bundle bundle) {
        try {
            return bundle.loadClass(moduleInfoClass);
        } catch (final ClassNotFoundException e) {
            String errorMessage = logMessage("Could not find class {} in bundle {}, reason {}", moduleInfoClass, bundle,
                    e);
            throw new IllegalStateException(errorMessage);
        }
    }

    public static String logMessage(final String slfMessage, final Object... params) {
        LOG.info(slfMessage, params);
        String formatMessage = slfMessage.replaceAll("\\{\\}", "%s");
        return String.format(formatMessage, params);
    }
}
