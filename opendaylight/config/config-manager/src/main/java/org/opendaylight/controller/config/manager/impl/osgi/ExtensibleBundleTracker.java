/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.config.manager.impl.osgi;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.util.tracker.BundleTracker;
import org.osgi.util.tracker.BundleTrackerCustomizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 *
 * Extensible bundle tracker. Takes several BundleTrackerCustomizers and
 * propagates bundle events to all of them.
 *
 * Primary customizer may return tracking object,
 * which will be passed to it during invocation of
 * {@link BundleTrackerCustomizer#removedBundle(Bundle, BundleEvent, Future)}
 *
 *
 * This extender modifies behaviour to not leak platform thread
 * in {@link BundleTrackerCustomizer#addingBundle(Bundle, BundleEvent)}
 * but deliver this event from its own single threaded executor.
 *
 * If bundle is removed before event for adding bundle was executed,
 * that event is cancelled. If addingBundle event is currently in progress
 * or was already executed, platform thread is block untill addingBundle
 * finishes so bundle could be removed correctly in platform thread.
 *
 *
 * Method {@link BundleTrackerCustomizer#removedBundle(Bundle, BundleEvent, Object)}
 * is never invoked on registered trackers.
 *
 * @param <T>
 */
public final class ExtensibleBundleTracker<T> extends BundleTracker<Future<T>> {
    private static final ThreadFactory THREAD_FACTORY = new ThreadFactoryBuilder()
        .setNameFormat("config-bundle-tracker-%d").build();
    private final ExecutorService eventExecutor;
    private final BundleTrackerCustomizer<T> primaryTracker;
    private final BundleTrackerCustomizer<?>[] additionalTrackers;

    private static final Logger LOG = LoggerFactory.getLogger(ExtensibleBundleTracker.class);

    public ExtensibleBundleTracker(final BundleContext context, final BundleTrackerCustomizer<T> primaryBundleTrackerCustomizer,
            final BundleTrackerCustomizer<?>... additionalBundleTrackerCustomizers) {
        this(context, Bundle.ACTIVE, primaryBundleTrackerCustomizer, additionalBundleTrackerCustomizers);
    }

    public ExtensibleBundleTracker(final BundleContext context, final int bundleState,
            final BundleTrackerCustomizer<T> primaryBundleTrackerCustomizer,
            final BundleTrackerCustomizer<?>... additionalBundleTrackerCustomizers) {
        super(context, bundleState, null);
        this.primaryTracker = primaryBundleTrackerCustomizer;
        this.additionalTrackers = additionalBundleTrackerCustomizers;
        eventExecutor = Executors.newSingleThreadExecutor(THREAD_FACTORY);
        LOG.trace("Registered as extender with context {} and bundle state {}", context, bundleState);
    }

    @Override
    public Future<T> addingBundle(final Bundle bundle, final BundleEvent event) {
        LOG.trace("Submiting AddingBundle for bundle {} and event {} to be processed asynchronously",bundle,event);
        Future<T> future = eventExecutor.submit(new Callable<T>() {
            @Override
            public T call() throws Exception {
                try {
                    T primaryTrackerRetVal = primaryTracker.addingBundle(bundle, event);

                    forEachAdditionalBundle(new BundleStrategy() {
                        @Override
                        public void execute(final BundleTrackerCustomizer<?> tracker) {
                            tracker.addingBundle(bundle, event);
                        }
                    });
                    LOG.trace("AddingBundle for {} and event {} finished successfully",bundle,event);
                    return primaryTrackerRetVal;
                } catch (Exception e) {
                    LOG.error("Failed to add bundle {}", bundle, e);
                    throw e;
                }
            }
        });
        return future;
    }

    @Override
    public void modifiedBundle(final Bundle bundle, final BundleEvent event, final Future<T> object) {
        // Intentionally NOOP

    }

    @Override
    public void removedBundle(final Bundle bundle, final BundleEvent event, final Future<T> object) {
        if(!object.isDone() && object.cancel(false)) {
            // We canceled adding event before it was processed
            // so it is safe to return
            LOG.trace("Adding Bundle event for {} was cancelled. No additional work required.",bundle);
            return;
        }
        try {
            LOG.trace("Invoking removedBundle event for {}",bundle);
            primaryTracker.removedBundle(bundle, event, object.get());
            forEachAdditionalBundle(new BundleStrategy() {
                @Override
                public void execute(final BundleTrackerCustomizer<?> tracker) {
                    tracker.removedBundle(bundle, event, null);
                }
            });
            LOG.trace("Removed bundle event for {} finished successfully.",bundle);
        } catch (Exception e) {
            LOG.error("Failed to remove bundle {}", bundle, e);
        }
    }

    private void forEachAdditionalBundle(final BundleStrategy lambda) {
        for (BundleTrackerCustomizer<?> trac : additionalTrackers) {
            lambda.execute(trac);
        }
    }

    private interface BundleStrategy {
        void execute(BundleTrackerCustomizer<?> tracker);
    }

}
