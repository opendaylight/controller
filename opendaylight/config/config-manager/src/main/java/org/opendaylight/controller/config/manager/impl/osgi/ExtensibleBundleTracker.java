/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.config.manager.impl.osgi;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.util.tracker.BundleTracker;
import org.osgi.util.tracker.BundleTrackerCustomizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * Extensible bundle tracker. Takes several BundleTrackerCustomizers and propagates bundle events to all of them.
 * Primary customizer
 *
 * @param <T>
 */
public final class ExtensibleBundleTracker<T> extends BundleTracker<T> {

    private final BundleTrackerCustomizer<T> primaryTracker;
    private final BundleTrackerCustomizer<?>[] additionalTrackers;

    private static final Logger logger = LoggerFactory.getLogger(ExtensibleBundleTracker.class);

    public ExtensibleBundleTracker(BundleContext context, BundleTrackerCustomizer<T> primaryBundleTrackerCustomizer,
                                   BundleTrackerCustomizer<?>... additionalBundleTrackerCustomizers) {
        this(context, Bundle.ACTIVE, primaryBundleTrackerCustomizer, additionalBundleTrackerCustomizers);
    }

    public ExtensibleBundleTracker(BundleContext context, int bundleState,
                                   BundleTrackerCustomizer<T> primaryBundleTrackerCustomizer,
                                   BundleTrackerCustomizer<?>... additionalBundleTrackerCustomizers) {
        super(context, bundleState, null);
        this.primaryTracker = primaryBundleTrackerCustomizer;
        this.additionalTrackers = additionalBundleTrackerCustomizers;
        logger.trace("Registered as extender with context {} and bundle state {}", context, bundleState);
    }

    @Override
    public T addingBundle(final Bundle bundle, final BundleEvent event) {
        T primaryTrackerRetVal = primaryTracker.addingBundle(bundle, event);

        forEachAdditionalBundle(new BundleStrategy() {
            @Override
            public void execute(BundleTrackerCustomizer<?> tracker) {
                tracker.addingBundle(bundle, event);
            }
        });

        return primaryTrackerRetVal;
    }

    @Override
    public void modifiedBundle(final Bundle bundle, final BundleEvent event, final T object) {
        primaryTracker.modifiedBundle(bundle, event, object);

        forEachAdditionalBundle(new BundleStrategy() {
            @Override
            public void execute(BundleTrackerCustomizer<?> tracker) {
                tracker.modifiedBundle(bundle, event, null);
            }
        });

    }

    @Override
    public void removedBundle(final Bundle bundle, final BundleEvent event, final T object) {
        primaryTracker.removedBundle(bundle, event, object);

        forEachAdditionalBundle(new BundleStrategy() {
            @Override
            public void execute(BundleTrackerCustomizer<?> tracker) {
                tracker.removedBundle(bundle, event, null);
            }
        });
    }

    private void forEachAdditionalBundle(BundleStrategy lambda) {
        for (BundleTrackerCustomizer<?> trac : additionalTrackers) {
            lambda.execute(trac);
        }
    }

    private static interface BundleStrategy {
        void execute(BundleTrackerCustomizer<?> tracker);
    }

}
