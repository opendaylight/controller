/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.schema.service.impl;

import static com.google.common.base.Preconditions.checkState;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.util.concurrent.CheckedFuture;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import javax.annotation.concurrent.GuardedBy;
import org.opendaylight.controller.sal.core.api.model.SchemaService;
import org.opendaylight.controller.sal.core.api.model.YangTextSourceProvider;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.concepts.Registration;
import org.opendaylight.yangtools.util.ListenerRegistry;
import org.opendaylight.yangtools.yang.model.api.Module;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.opendaylight.yangtools.yang.model.api.SchemaContextListener;
import org.opendaylight.yangtools.yang.model.api.SchemaContextProvider;
import org.opendaylight.yangtools.yang.model.repo.api.SchemaSourceException;
import org.opendaylight.yangtools.yang.model.repo.api.SourceIdentifier;
import org.opendaylight.yangtools.yang.model.repo.api.YangTextSchemaSource;
import org.opendaylight.yangtools.yang.parser.repo.YangTextSchemaContextResolver;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.BundleTracker;
import org.osgi.util.tracker.BundleTrackerCustomizer;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GlobalBundleScanningSchemaServiceImpl implements SchemaContextProvider, SchemaService, ServiceTrackerCustomizer<SchemaContextListener, SchemaContextListener>, YangTextSourceProvider, AutoCloseable {
    private static final Logger LOG = LoggerFactory.getLogger(GlobalBundleScanningSchemaServiceImpl.class);

    @GuardedBy(value = "lock")
    private final ListenerRegistry<SchemaContextListener> listeners = new ListenerRegistry<>();
    private final YangTextSchemaContextResolver contextResolver = YangTextSchemaContextResolver.create("global-bundle");
    private final BundleScanner scanner = new BundleScanner();
    private final BundleContext context;

    private ServiceTracker<SchemaContextListener, SchemaContextListener> listenerTracker;
    private BundleTracker<Iterable<Registration>> bundleTracker;
    private boolean starting = true;
    private volatile boolean stopping;
    private final Object lock = new Object();

    private GlobalBundleScanningSchemaServiceImpl(final BundleContext context) {
        this.context = Preconditions.checkNotNull(context);
    }

    public static GlobalBundleScanningSchemaServiceImpl createInstance(final BundleContext ctx) {
        GlobalBundleScanningSchemaServiceImpl instance = new GlobalBundleScanningSchemaServiceImpl(ctx);
        instance.start();
        return instance;
    }

    public BundleContext getContext() {
        return context;
    }

    private void start() {
        checkState(context != null);
        LOG.debug("start() starting");

        listenerTracker = new ServiceTracker<>(context, SchemaContextListener.class, GlobalBundleScanningSchemaServiceImpl.this);
        bundleTracker = new BundleTracker<>(context, Bundle.RESOLVED | Bundle.STARTING |
                Bundle.STOPPING | Bundle.ACTIVE, scanner);

        synchronized(lock) {
            bundleTracker.open();

            LOG.debug("BundleTracker.open() complete");

            boolean hasExistingListeners = Iterables.size(listeners.getListeners()) > 0;
            if(hasExistingListeners) {
                tryToUpdateSchemaContext();
            }
        }

        listenerTracker.open();
        starting = false;

        LOG.debug("start() complete");
    }

    @Override
    public SchemaContext getSchemaContext() {
        return getGlobalContext();
    }

    @Override
    public SchemaContext getGlobalContext() {
        return contextResolver.getSchemaContext().orNull();
    }

    @Override
    public void addModule(final Module module) {
        throw new UnsupportedOperationException();
    }

    @Override
    public SchemaContext getSessionContext() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void removeModule(final Module module) {
        throw new UnsupportedOperationException();
    }

    @Override
    public ListenerRegistration<SchemaContextListener> registerSchemaContextListener(final SchemaContextListener listener) {
        synchronized(lock) {
            Optional<SchemaContext> potentialCtx = contextResolver.getSchemaContext();
            if(potentialCtx.isPresent()) {
                listener.onGlobalContextUpdated(potentialCtx.get());
            }
            return listeners.register(listener);
        }
    }

    @Override
    public void close() {
        stopping = true;
        if (bundleTracker != null) {
            bundleTracker.close();
        }
        if (listenerTracker != null) {
            listenerTracker.close();
        }

        for (ListenerRegistration<SchemaContextListener> l : listeners.getListeners()) {
            l.close();
        }
    }

    @GuardedBy(value = "lock")
    private void notifyListeners(final SchemaContext snapshot) {
        Object[] services = listenerTracker.getServices();
        for (ListenerRegistration<SchemaContextListener> listener : listeners) {
            try {
                listener.getInstance().onGlobalContextUpdated(snapshot);
            } catch (Exception e) {
                LOG.error("Exception occured during invoking listener", e);
            }
        }
        if (services != null) {
            for (Object rawListener : services) {
                final SchemaContextListener listener = (SchemaContextListener) rawListener;
                try {
                    listener.onGlobalContextUpdated(snapshot);
                } catch (Exception e) {
                    LOG.error("Exception occured during invoking listener {}", listener, e);
                }
            }
        }
    }

    @Override
    public CheckedFuture<YangTextSchemaSource, SchemaSourceException> getSource(final SourceIdentifier sourceIdentifier) {
        return contextResolver.getSource(sourceIdentifier);

    }

    private class BundleScanner implements BundleTrackerCustomizer<Iterable<Registration>> {
        @Override
        public Iterable<Registration> addingBundle(final Bundle bundle, final BundleEvent event) {

            if (bundle.getBundleId() == 0) {
                return Collections.emptyList();
            }

            final Enumeration<URL> enumeration = bundle.findEntries("META-INF/yang", "*.yang", false);
            if (enumeration == null) {
                return Collections.emptyList();
            }

            final List<Registration> urls = new ArrayList<>();
            while (enumeration.hasMoreElements()) {
                final URL u = enumeration.nextElement();
                try {
                    urls.add(contextResolver.registerSource(u));
                    LOG.debug("Registered {}", u);
                } catch (Exception e) {
                    LOG.warn("Failed to register {}, ignoring it", e);
                }
            }

            if (!urls.isEmpty()) {
                LOG.debug("Loaded {} new URLs from bundle {}, attempting to rebuild schema context",
                        urls.size(), bundle.getSymbolicName());
                tryToUpdateSchemaContext();
            }

            return ImmutableList.copyOf(urls);
        }

        @Override
        public void modifiedBundle(final Bundle bundle, final BundleEvent event, final Iterable<Registration> object) {
        }

        /**
         * If removing YANG files makes yang store inconsistent, method
         * {@link #getYangStoreSnapshot()} will throw exception. There is no
         * rollback.
         */

        @Override
        public void removedBundle(final Bundle bundle, final BundleEvent event, final Iterable<Registration> urls) {
            for (Registration url : urls) {
                try {
                    url.close();
                } catch (Exception e) {
                    LOG.warn("Failed do unregister URL {}, proceeding", url, e);
                }
            }

            int numUrls = Iterables.size(urls);
            if(numUrls > 0 ) {
                if(LOG.isDebugEnabled()) {
                    LOG.debug("removedBundle: {}, state: {}, # urls: {}", bundle.getSymbolicName(), bundle.getState(), numUrls);
                }

                tryToUpdateSchemaContext();
            }
        }
    }

    @Override
    public SchemaContextListener addingService(final ServiceReference<SchemaContextListener> reference) {

        SchemaContextListener listener = context.getService(reference);
        SchemaContext _ctxContext = getGlobalContext();
        if (getContext() != null && _ctxContext != null) {
            listener.onGlobalContextUpdated(_ctxContext);
        }
        return listener;
    }

    public void tryToUpdateSchemaContext() {
        if (starting || stopping) {
            return;
        }

        synchronized(lock) {
            Optional<SchemaContext> schema = contextResolver.getSchemaContext();
            if(schema.isPresent()) {
                if(LOG.isDebugEnabled()) {
                    LOG.debug("Got new SchemaContext: # of modules {}", schema.get().getAllModuleIdentifiers().size());
                }

                notifyListeners(schema.get());
            }
        }
    }

    @Override
    public void modifiedService(final ServiceReference<SchemaContextListener> reference, final SchemaContextListener service) {
        // NOOP
    }

    @Override
    public void removedService(final ServiceReference<SchemaContextListener> reference, final SchemaContextListener service) {
        context.ungetService(reference);
    }
}
