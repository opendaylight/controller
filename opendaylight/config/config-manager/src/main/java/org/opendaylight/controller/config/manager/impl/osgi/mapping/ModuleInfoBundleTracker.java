/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.config.manager.impl.osgi.mapping;

import static java.util.Objects.requireNonNull;
import static org.osgi.framework.BundleEvent.RESOLVED;
import static org.osgi.framework.BundleEvent.STARTED;
import static org.osgi.framework.BundleEvent.STARTING;
import static org.osgi.framework.BundleEvent.UNINSTALLED;
import static org.osgi.framework.BundleEvent.UNRESOLVED;
import static org.osgi.framework.BundleEvent.UPDATED;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;
import com.google.common.io.Resources;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.regex.Pattern;
import javax.annotation.RegEx;
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
        BundleTrackerCustomizer<ModuleInfoBundleTracker.ModuleBundleState> {

    final class ModuleBundleState {
        private final Bundle bundle;
        private final URL resource;

        private List<ObjectRegistration<YangModuleInfo>> registrations = ImmutableList.of();

        ModuleBundleState(final Bundle bundle, final URL resource) {
            this.bundle = requireNonNull(bundle);
            this.resource = requireNonNull(resource);
        }

        void ensureStarted() {
            if (!registrations.isEmpty()) {
                LOG.debug("Bundle {} is already registered", bundle);
                return;
            }

            LOG.debug("Bundle {} scanning services in {}", bundle, resource);
            final List<String> services;
            try {
                services = Resources.readLines(resource, StandardCharsets.UTF_8);
            } catch (IOException e) {
                LOG.error("Error while reading {} from bundle {}", resource, bundle, e);
                return;
            }

            LOG.trace("Bundle {} has services {}", bundle, services);
            final Builder<ObjectRegistration<YangModuleInfo>> builder = ImmutableList.builder();
            for (String service : services) {
                LOG.trace("Retrieve ModuleInfo({}, {})", service, bundle);

                final ObjectRegistration<YangModuleInfo> reg;
                try {
                    final YangModuleInfo moduleInfo = retrieveModuleInfo(service, bundle);
                    reg = moduleInfoRegistry.registerModuleInfo(moduleInfo);
                } catch (final RuntimeException e) {
                    LOG.warn("Failed to process {} for bundle {}, attempting to continue", service, bundle, e);
                    continue;
                }

                builder.add(reg);
            }

            registrations = builder.build();
            LOG.trace("Bundle {} resolved to {}", bundle, registrations);
            if (!starting && !registrations.isEmpty()) {
                moduleInfoRegistry.updateService();
            }
        }

        void ensureStopped() {
            if (registrations.isEmpty()) {
                LOG.debug("Bundle {} already unregistered", bundle);
                return;
            }

            LOG.debug("Bundle {} removing registrations", bundle);
            registrations.forEach(reg -> {
                try {
                    reg.close();
                } catch (final Exception e) {
                    LOG.warn("Unable to unregister YangModuleInfo {}, continuing", reg.getInstance(), e);
                }
            });
            registrations = ImmutableList.of();
            moduleInfoRegistry.updateService();
        }

        void processEvent(final int type) {
            LOG.debug("Bundle {} got event {} starting {}", bundle, type, starting);

            if ((type & (RESOLVED | STARTING | STARTED)) != 0) {
                ensureStarted();
            } else if ((type & (UNRESOLVED | UPDATED | UNINSTALLED)) != 0) {
                ensureStopped();
            } else {
                LOG.debug("Bundle {} ignoring event {}", bundle, type);
            }
        }
    }

    private static final Logger LOG = LoggerFactory.getLogger(ModuleInfoBundleTracker.class);

    @RegEx
    private static final String LOG_REGEX_STR = "\\{\\}";
    private static final Pattern LOG_REGEX = Pattern.compile(LOG_REGEX_STR);

    public static final String MODULE_INFO_PROVIDER_PATH_PREFIX = "META-INF/services/";
    private static final String YANG_MODULE_INFO_SERVICE_PATH = MODULE_INFO_PROVIDER_PATH_PREFIX
            + YangModelBindingProvider.class.getName();

    private final RefreshingSCPModuleInfoRegistry moduleInfoRegistry;

    private BundleTracker<?> bundleTracker;
    private volatile boolean starting;

    public ModuleInfoBundleTracker(final RefreshingSCPModuleInfoRegistry moduleInfoRegistry) {
        this.moduleInfoRegistry = moduleInfoRegistry;
    }

    public void open(final BundleTracker<?> bundleTracker) {
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
    public ModuleBundleState addingBundle(final Bundle bundle, final BundleEvent event) {
        final URL resource = bundle.getEntry(YANG_MODULE_INFO_SERVICE_PATH);
        if (resource == null) {
            LOG.debug("Bundle {} is missing {}, not tracking it", bundle, YANG_MODULE_INFO_SERVICE_PATH);
            return null;
        }

        LOG.debug("Tracking bundle {} resource {} starting {}", bundle, resource, starting);
        final ModuleBundleState ret = new ModuleBundleState(bundle, resource);

        if (event != null) {
            ret.processEvent(event.getType());
        }

        return ret;
    }

    @Override
    public void modifiedBundle(final Bundle bundle, final BundleEvent event, final ModuleBundleState object) {
        if (event != null) {
            object.processEvent(event.getType());
        } else {
            LOG.debug("Bundle {} modification has no event, ignoring it", bundle);
        }
    }

    @Override
    public void removedBundle(final Bundle bundle, final BundleEvent event, final ModuleBundleState object) {
        LOG.debug("Stopping tracking bundle {}", bundle);
        object.ensureStopped();
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

    private static String logMessage(final String slfMessage, final Object... params) {
        LOG.info(slfMessage, params);
        return String.format(LOG_REGEX.matcher(slfMessage).replaceAll("%s"), params);
    }
}
