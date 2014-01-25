/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.dom.broker;

import static com.google.common.base.Preconditions.checkState;

import java.net.URL;
import java.util.Enumeration;

import org.opendaylight.controller.sal.core.api.model.SchemaService;
import org.opendaylight.controller.sal.dom.broker.impl.SchemaContextProvider;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.concepts.Registration;
import org.opendaylight.yangtools.concepts.util.ListenerRegistry;
import org.opendaylight.yangtools.yang.model.api.Module;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.opendaylight.yangtools.yang.model.api.SchemaServiceListener;
import org.opendaylight.yangtools.yang.parser.impl.util.URLSchemaContextResolver;
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

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSet.Builder;

public class GlobalBundleScanningSchemaServiceImpl implements //
        SchemaContextProvider, //
        SchemaService, //
        ServiceTrackerCustomizer<SchemaServiceListener, SchemaServiceListener>, //
        AutoCloseable {
    private static final Logger logger = LoggerFactory.getLogger(GlobalBundleScanningSchemaServiceImpl.class);

    private ListenerRegistry<SchemaServiceListener> listeners;

    private BundleContext context;
    private BundleScanner scanner = new BundleScanner();

    private BundleTracker<ImmutableSet<Registration<URL>>> bundleTracker;

    private final URLSchemaContextResolver contextResolver = new URLSchemaContextResolver();

    private ServiceTracker<SchemaServiceListener, SchemaServiceListener> listenerTracker;

    private boolean starting = true;

    public ListenerRegistry<SchemaServiceListener> getListeners() {
        return listeners;
    }

    public void setListeners(ListenerRegistry<SchemaServiceListener> listeners) {
        this.listeners = listeners;
    }

    public BundleContext getContext() {
        return context;
    }

    public void setContext(BundleContext context) {
        this.context = context;
    }

    public void start() {
        checkState(context != null);
        if (listeners == null) {
            listeners = new ListenerRegistry<>();
        }

        listenerTracker = new ServiceTracker<>(context, SchemaServiceListener.class, GlobalBundleScanningSchemaServiceImpl.this);
        bundleTracker = new BundleTracker<ImmutableSet<Registration<URL>>>(context, BundleEvent.RESOLVED
                        | BundleEvent.UNRESOLVED, scanner);
        bundleTracker.open();
        listenerTracker.open();
        starting = false;
        tryToUpdateSchemaContext();
    }

    @Override
    public SchemaContext getSchemaContext() {
        return getGlobalContext();
    }

    public SchemaContext getGlobalContext() {
        return contextResolver.getSchemaContext().orNull();
    }

    @Override
    public void addModule(Module module) {
        throw new UnsupportedOperationException();
    }

    @Override
    public SchemaContext getSessionContext() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void removeModule(Module module) {
        throw new UnsupportedOperationException();
    }

    @Override
    public ListenerRegistration<SchemaServiceListener> registerSchemaServiceListener(SchemaServiceListener listener) {
        return listeners.register(listener);
    }

    @Override
    public void close() throws Exception {
        if (bundleTracker != null) {
            bundleTracker.close();
        }
        if (listenerTracker != null) {
            listenerTracker.close();
        }
        // FIXME: Add listeners.close();
    }


    private void updateContext(SchemaContext snapshot) {
        Object[] services = listenerTracker.getServices();
        if (services != null) {
            for (Object rawListener : services) {
                SchemaServiceListener listener = (SchemaServiceListener) rawListener;
                try {
                    listener.onGlobalContextUpdated(snapshot);
                } catch (Exception e) {
                    logger.error("Exception occured during invoking listener", e);
                }
            }
        }
        for (ListenerRegistration<SchemaServiceListener> listener : listeners) {
            try {
                listener.getInstance().onGlobalContextUpdated(snapshot);
            } catch (Exception e) {
                logger.error("Exception occured during invoking listener", e);
            }
        }
    }

    private class BundleScanner implements BundleTrackerCustomizer<ImmutableSet<Registration<URL>>> {
        @Override
        public ImmutableSet<Registration<URL>> addingBundle(Bundle bundle, BundleEvent event) {

            if (bundle.getBundleId() == 0) {
                return ImmutableSet.of();
            }

            Enumeration<URL> enumeration = bundle.findEntries("META-INF/yang", "*.yang", false);
            Builder<Registration<URL>> builder = ImmutableSet.<Registration<URL>> builder();
            while (enumeration != null && enumeration.hasMoreElements()) {
                Registration<URL> reg = contextResolver.registerSource(enumeration.nextElement());
                builder.add(reg);
            }
            ImmutableSet<Registration<URL>> urls = builder.build();
            if(urls.isEmpty()) {
                return urls;
            }
            tryToUpdateSchemaContext();
            return urls;
        }

        @Override
        public void modifiedBundle(Bundle bundle, BundleEvent event, ImmutableSet<Registration<URL>> object) {
            logger.debug("Modified bundle {} {} {}", bundle, event, object);
        }

        /**
         * If removing YANG files makes yang store inconsistent, method
         * {@link #getYangStoreSnapshot()} will throw exception. There is no
         * rollback.
         */

        @Override
        public synchronized void removedBundle(Bundle bundle, BundleEvent event, ImmutableSet<Registration<URL>> urls) {
            for (Registration<URL> url : urls) {
                try {
                    url.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            tryToUpdateSchemaContext();
        }
    }

    @Override
    public SchemaServiceListener addingService(ServiceReference<SchemaServiceListener> reference) {

        SchemaServiceListener listener = context.getService(reference);
        SchemaContext _ctxContext = getGlobalContext();
        if (getContext() != null && _ctxContext != null) {
            listener.onGlobalContextUpdated(_ctxContext);
        }
        return listener;
    }

    public synchronized void tryToUpdateSchemaContext() {
        if(starting ) {
            return;
        }
        Optional<SchemaContext> schema = contextResolver.tryToUpdateSchemaContext();
        if(schema.isPresent()) {
            updateContext(schema.get());
        }
    }

    @Override
    public void modifiedService(ServiceReference<SchemaServiceListener> reference, SchemaServiceListener service) {
        // NOOP
    }

    @Override
    public void removedService(ServiceReference<SchemaServiceListener> reference, SchemaServiceListener service) {
        context.ungetService(reference);
    }
}
