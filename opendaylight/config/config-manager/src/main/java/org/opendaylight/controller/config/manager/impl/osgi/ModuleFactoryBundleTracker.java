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
import com.google.common.base.Stopwatch;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.io.Resources;
import com.google.common.util.concurrent.Uninterruptibles;
import java.io.IOException;
import java.net.URL;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;
import javax.annotation.concurrent.GuardedBy;
import org.opendaylight.controller.config.spi.ModuleFactory;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
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
    private static final Logger LOG = LoggerFactory.getLogger(ModuleFactoryBundleTracker.class);
    private static final long BUNDLE_CONTEXT_TIMEOUT = TimeUnit.MILLISECONDS.convert(60, TimeUnit.SECONDS);

    private final BlankTransactionServiceTracker blankTransactionServiceTracker;

    @GuardedBy(value = "bundleModuleFactoryMap")
    private final Multimap<BundleKey, ModuleFactory> bundleModuleFactoryMap = HashMultimap.create();

    public ModuleFactoryBundleTracker(BlankTransactionServiceTracker blankTransactionServiceTracker) {
        this.blankTransactionServiceTracker = blankTransactionServiceTracker;
    }

    @Override
    public Object addingBundle(Bundle bundle, BundleEvent event) {
        if(event != null && event.getType() == BundleEvent.STOPPED) {
            // We're tracking RESOLVED, STARTING, and ACTIVE states but not STOPPING. So when a bundle transitions
            // to STOPPING, removedBundle gets called and we remove the ModuleFactory entries. However the
            // bundle will then transition to RESOLVED which will cause the tracker to notify addingBundle.
            // In this case we don't want to re-add the ModuleFactory entries so we check if the BundleEvent
            // is STOPPED.
            return null;
        }

        URL resource = bundle.getEntry("META-INF/services/" + ModuleFactory.class.getName());
        LOG.trace("Got addingBundle event of bundle {}, resource {}, event {}",
                bundle, resource, event);
        if (resource != null) {
            try {
                for (String factoryClassName : Resources.readLines(resource, Charsets.UTF_8)) {
                    Entry<ModuleFactory, Bundle> moduleFactoryEntry = registerFactory(factoryClassName, bundle);
                    synchronized (bundleModuleFactoryMap) {
                        bundleModuleFactoryMap.put(new BundleKey(moduleFactoryEntry.getValue()),
                                moduleFactoryEntry.getKey());
                    }
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
        synchronized (bundleModuleFactoryMap) {
            bundleModuleFactoryMap.removeAll(new BundleKey(bundle));
        }

        // workaround for service tracker not getting removed service event
        blankTransactionServiceTracker.blankTransaction();
    }

    public Collection<Map.Entry<ModuleFactory, BundleContext>> getModuleFactoryEntries() {
        Collection<Entry<BundleKey, ModuleFactory>> entries;
        synchronized (bundleModuleFactoryMap) {
            entries = new ArrayList<>(bundleModuleFactoryMap.entries());
        }

        Collection<Map.Entry<ModuleFactory, BundleContext>> result = new ArrayList<>(entries.size());
        for(Entry<BundleKey, ModuleFactory> entry: entries) {
            BundleContext context = entry.getKey().getBundleContext();
            if(context == null) {
                LOG.warn("Bundle context for {} ModuleFactory not found", entry.getValue());
            } else {
                result.add(new AbstractMap.SimpleImmutableEntry<>(entry.getValue(), context));
            }
        }

        return result;
    }

    @VisibleForTesting
    protected static Map.Entry<ModuleFactory, Bundle> registerFactory(String factoryClassName, Bundle bundle) {
        String errorMessage;
        Exception ex = null;
        try {
            Class<?> clazz = bundle.loadClass(factoryClassName);
            if (ModuleFactory.class.isAssignableFrom(clazz)) {
                try {
                    LOG.debug("Registering {} in bundle {}", clazz.getName(), bundle);

                    return new AbstractMap.SimpleImmutableEntry<>((ModuleFactory)clazz.newInstance(), bundle);
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
                } catch (RuntimeException e) {
                    errorMessage = logMessage(
                            "Unexpected exception during instantiation of class {} in bundle {}, reason {}",
                            clazz, bundle.getBundleContext(), e);
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

    private static class BundleKey {
        Bundle bundle;
        BundleContext bundleContext;

        public BundleKey(Bundle bundle) {
            this.bundle = bundle;
        }

        BundleContext getBundleContext() {
            if(bundleContext != null) {
                return bundleContext;
            }

            // If the bundle isn't activated yet, it may not have a BundleContext yet so busy wait for it.
            Stopwatch timer = Stopwatch.createStarted();
            while(timer.elapsed(TimeUnit.MILLISECONDS) <= BUNDLE_CONTEXT_TIMEOUT) {
                bundleContext = bundle.getBundleContext();
                if(bundleContext != null) {
                    return bundleContext;
                }

                Uninterruptibles.sleepUninterruptibly(10, TimeUnit.MILLISECONDS);
            }

            return null;
        }

        @Override
        public int hashCode() {
            return (int) bundle.getBundleId();
        }

        @Override
        public boolean equals(Object obj) {
            if (getClass() != obj.getClass()) {
                return false;
            }
            BundleKey other = (BundleKey) obj;
            return bundle.getBundleId() == other.bundle.getBundleId();
        }
    }
}
