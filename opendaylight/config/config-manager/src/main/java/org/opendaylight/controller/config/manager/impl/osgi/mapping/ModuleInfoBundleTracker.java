/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.config.manager.impl.osgi.mapping;

import static org.osgi.framework.BundleEvent.RESOLVED;
import static org.osgi.framework.BundleEvent.STARTED;
import static org.osgi.framework.BundleEvent.STARTING;
import static org.osgi.framework.BundleEvent.UNINSTALLED;
import static org.osgi.framework.BundleEvent.UNRESOLVED;
import static org.osgi.framework.BundleEvent.UPDATED;

import com.google.common.base.Verify;
import com.google.common.io.Resources;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
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
 * Tracks bundles and attempts to retrieve YangModuleInfo, which is then fed into ModuleInfoRegistry
 */
public final class ModuleInfoBundleTracker implements AutoCloseable,
        BundleTrackerCustomizer<Collection<ObjectRegistration<YangModuleInfo>>> {

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

    private Collection<ObjectRegistration<YangModuleInfo>> activateBundle(final Bundle bundle, final URL resource) {
        LOG.debug("Got addingBundle({}) with YangModelBindingProvider resource {}", bundle, resource);

        final Collection<String> services;
        try {
            services = Resources.readLines(resource, StandardCharsets.UTF_8);
        } catch (IOException e) {
            LOG.error("Error while reading {} from bundle {}", resource, bundle, e);
            return Collections.emptyList();
        }

        final Collection<ObjectRegistration<YangModuleInfo>> registrations = new ArrayList<>(services.size());
        for (String moduleInfoName : services) {
            LOG.trace("Retrieve ModuleInfo({}, {})", moduleInfoName, bundle);

            final ObjectRegistration<YangModuleInfo> reg;
            try {
                final YangModuleInfo moduleInfo = retrieveModuleInfo(moduleInfoName, bundle);
                reg = moduleInfoRegistry.registerModuleInfo(moduleInfo);
            } catch (final RuntimeException e) {
                LOG.warn("Failed to process {} for bundle {}, attempting to continue", moduleInfoName, bundle, e);
                continue;
            }

            registrations.add(reg);
        }

        LOG.trace("Bundle {} resolved to {}", registrations);
        if (!starting && !registrations.isEmpty()) {
            moduleInfoRegistry.updateService();
        }

        return registrations;
    }

    private void deactivateBundle(final Bundle bundle, final Collection<ObjectRegistration<YangModuleInfo>> regs) {
        LOG.debug("Bundle {} removing registrations", bundle);
        regs.forEach(reg -> {
            try {
                reg.close();
            } catch (final Exception e) {
                LOG.warn("Unable to unregister YangModuleInfo {}, continuing", reg.getInstance(), e);
            }
        });
        regs.clear();
    }

    @Override
    public Collection<ObjectRegistration<YangModuleInfo>> addingBundle(final Bundle bundle, final BundleEvent event) {
        LOG.debug("Adding bundle {} event {}", bundle, event);

        final URL resource = bundle.getEntry(YANG_MODULE_INFO_SERVICE_PATH);
        if (resource == null) {
            LOG.debug("Bundle {} is missing {}, not tracking it", bundle, YANG_MODULE_INFO_SERVICE_PATH);
            return null;
        }
        if (event == null) {
            LOG.debug("Bundle {} has no event, postponing its processing", bundle);
            return new ArrayList<>(0);
        }

        final int type = event.getType();
        if ((type & (RESOLVED | STARTING | STARTED)) == 0) {
            LOG.debug("Bundle {} has event type {}, postponing its processing", bundle, type);
            return new ArrayList<>(0);
        }

        return activateBundle(bundle, resource);
    }

    @Override
    public void modifiedBundle(final Bundle bundle, final BundleEvent event,
            final Collection<ObjectRegistration<YangModuleInfo>> object) {
        if (event == null) {
            LOG.debug("Bundle {} modification has no event", bundle);
            return;
        }

        final int type = event.getType();
        LOG.debug("Bundle {} got event {}", bundle, type);
        if ((type & (RESOLVED | STARTING | STARTED)) != 0) {
            if (!object.isEmpty()) {
                final URL resource = Verify.verifyNotNull(bundle.getEntry(YANG_MODULE_INFO_SERVICE_PATH));
                object.addAll(activateBundle(bundle, resource));
            } else {
                LOG.debug("Bundle {} already has registrations, ignoring event {}", bundle, type);
            }

            return;
        }

        if (object.isEmpty()) {
            LOG.debug("Bundle {} has empty registrations, ignoring event {}", bundle, type);
            return;
        }

        if ((type & (UNRESOLVED | UPDATED | UNINSTALLED)) != 0) {
            deactivateBundle(bundle, object);
        } else {
            LOG.debug("Bundle {} ignoring event {}", bundle, type);
        }
    }

    @Override
    public void removedBundle(final Bundle bundle, final BundleEvent event,
            final Collection<ObjectRegistration<YangModuleInfo>> regs) {
        if (regs != null) {
            deactivateBundle(bundle, regs);
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

        try{
            return instance.getModuleInfo();
        } catch (NoClassDefFoundError | ExceptionInInitializerError e) {
            throw new IllegalStateException("Error while executing getModuleInfo on " + instance, e);
        }
    }

    private static Class<?> loadClass(final String moduleInfoClass, final Bundle bundle) {
        try {
            return bundle.loadClass(moduleInfoClass);
        } catch (final ClassNotFoundException e) {
            String errorMessage = logMessage("Could not find class {} in bundle {}, reason {}", moduleInfoClass,
                bundle, e);
            throw new IllegalStateException(errorMessage);
        }
    }

    public static String logMessage(final String slfMessage, final Object... params) {
        LOG.info(slfMessage, params);
        String formatMessage = slfMessage.replaceAll("\\{\\}", "%s");
        return String.format(formatMessage, params);
    }
}
