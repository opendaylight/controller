/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.config.manager.impl.util;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.util.tracker.BundleTracker;
import org.osgi.util.tracker.ServiceTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OsgiRegistrationUtil {
    private static final Logger LOG = LoggerFactory.getLogger(OsgiRegistrationUtil.class);

    private OsgiRegistrationUtil() {
    }

    @SafeVarargs
    public static <T> AutoCloseable registerService(final BundleContext bundleContext, final T service, final Class<? super T> ... interfaces) {
        checkNotNull(service);
        checkNotNull(interfaces);
        List<AutoCloseable> autoCloseableList = new ArrayList<>();
        for (Class<? super T> ifc : interfaces) {
            ServiceRegistration<? super T> serviceRegistration = bundleContext.registerService(ifc, service, null);
            autoCloseableList.add(wrap(serviceRegistration));
        }
        return aggregate(autoCloseableList);
    }

    public static AutoCloseable wrap(final ServiceRegistration<?> reg) {
        checkNotNull(reg);
        return new AutoCloseable() {
            @Override
            public void close() throws Exception {
                reg.unregister();
            }
        };
    }

    public static AutoCloseable wrap(final BundleTracker<?> bundleTracker) {
        checkNotNull(bundleTracker);
        return new AutoCloseable() {
            @Override
            public void close() throws Exception {
                bundleTracker.close();
            }
        };
    }

    public static AutoCloseable wrap(final ServiceTracker<?, ?> serviceTracker) {
        checkNotNull(serviceTracker);
        return new AutoCloseable() {
            @Override
            public void close() throws Exception {
                serviceTracker.close();
            }
        };
    }

    /**
     * Close list of auto closeables in reverse order
     */
    public static AutoCloseable aggregate(final List<? extends AutoCloseable> list) {
        checkNotNull(list);

        return new AutoCloseable() {
            @Override
            public void close() throws Exception {
                Exception firstException = null;
                for (ListIterator<? extends AutoCloseable> it = list.listIterator(list.size()); it.hasPrevious();) {
                    AutoCloseable ac = it.previous();
                    try {
                        ac.close();
                    } catch (Exception e) {
                        LOG.warn("Exception while closing {}", ac, e);
                        if (firstException == null) {
                            firstException = e;
                        } else {
                            firstException.addSuppressed(e);
                        }
                    }
                }
                if (firstException != null) {
                    throw firstException;
                }
            }
        };
    }
}
