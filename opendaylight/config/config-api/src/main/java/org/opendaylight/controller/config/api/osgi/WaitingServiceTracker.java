/*
 * Copyright (c) 2016 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.config.api.osgi;

import java.util.concurrent.TimeUnit;
import javax.annotation.Nonnull;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.util.tracker.ServiceTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Tracker that waits for an OSGi service.
 *
 * @author Thomas Pantelis
 */
public final class WaitingServiceTracker<T> implements AutoCloseable {
    private static final Logger LOG = LoggerFactory.getLogger(WaitingServiceTracker.class);
    public static final long FIVE_MINUTES = TimeUnit.MILLISECONDS.convert(5, TimeUnit.MINUTES);

    private final ServiceTracker<T, ?> tracker;
    private final Class<T> serviceInterface;

    private WaitingServiceTracker(Class<T> serviceInterface, ServiceTracker<T, ?> tracker) {
        this.tracker = tracker;
        this.serviceInterface = serviceInterface;
    }

    /**
     * Waits for an OSGi services.
     *
     * @param timeoutInMillis the timeout in millis
     * @return the service instance
     * @throws ServiceNotFoundException if it times out or is interrupted
     */
    @SuppressWarnings("unchecked")
    public T waitForService(long timeoutInMillis) throws ServiceNotFoundException {
        try {
            T service = (T) tracker.waitForService(timeoutInMillis);
            if(service == null) {
                throw new ServiceNotFoundException(String.format("OSGi Service %s was not found after %d ms",
                        serviceInterface, timeoutInMillis));
            }

            return service;
        } catch(InterruptedException e) {
            throw new ServiceNotFoundException(String.format("Wait for OSGi service %s was interrrupted",
                    serviceInterface));
        }
    }

    /**
     * Creates an instance.
     *
     * @param serviceInterface the service interface
     * @param context the BundleContext
     * @return new WaitingServiceTracker instance
     */
    public static <T> WaitingServiceTracker<T> create(@Nonnull Class<T> serviceInterface, @Nonnull BundleContext context) {
        ServiceTracker<T, ?> tracker = new ServiceTracker<>(context, serviceInterface, null);
        tracker.open();
        return new WaitingServiceTracker<>(serviceInterface, tracker);
    }

    /**
     * Creates an instance.
     *
     * @param serviceInterface the service interface
     * @param context the BundleContext
     * @param filter the OSGi service filter
     * @return new WaitingServiceTracker instance
     */
    public static <T> WaitingServiceTracker<T> create(@Nonnull Class<T> serviceInterface, @Nonnull BundleContext context,
            @Nonnull String filter) {
        String newFilter = String.format("(&(%s=%s)%s)", Constants.OBJECTCLASS, serviceInterface.getName(), filter);
        try {
            ServiceTracker<T, ?> tracker = new ServiceTracker<>(context, context.createFilter(newFilter), null);
            tracker.open();
            return new WaitingServiceTracker<>(serviceInterface, tracker);
        } catch(InvalidSyntaxException e) {
            throw new IllegalArgumentException(String.format("Invalid OSGi filter %s", newFilter), e);
        }
    }

    @Override
    public void close() {
        try {
            tracker.close();
        } catch(RuntimeException e) {
            // The ServiceTracker could throw IllegalStateException if the BundleContext is already closed.
            // This is benign so ignore it.
            LOG.debug("Error closing ServiceTracker", e);
        }
    }
}
