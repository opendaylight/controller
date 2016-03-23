/*
 * Copyright (c) 2016 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.common.util.osgi;

import java.util.concurrent.TimeUnit;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.util.tracker.ServiceTracker;

/**
 * Utilities for accessing OSGi services.
 *
 * @author Thomas Pantelis
 */
public final class OsgiServiceUtils {
    public static final long FIVE_MINUTES = TimeUnit.MILLISECONDS.convert(5, TimeUnit.MINUTES);

    private OsgiServiceUtils() {
    }

    /**
     * Waits for an OSGi services.
     *
     * @param serviceInterface the service interface
     * @param context the BundleContext
     * @param timeoutInMillis the timeout in millis
     * @param filter the optional OSGi service filter
     * @return the service instance
     * @throws ServiceNotFoundException if it times out or is interrupted
     */
    @SuppressWarnings("unchecked")
    public static <T> T waitForService(@Nonnull Class<T> serviceInterface, @Nonnull BundleContext context,
            long timeoutInMillis, @Nullable String filter) throws ServiceNotFoundException {
        ServiceTracker<?, ?> tracker = null;
        try {
            if(filter == null) {
                tracker = new ServiceTracker<>(context, serviceInterface, null);
            } else {
                String newFilter = String.format("&(%s=%s)%s", Constants.OBJECTCLASS, serviceInterface.getName(),
                        filter);
                tracker = new ServiceTracker<>(context, context.createFilter(newFilter), null);
            }

            T service = (T) tracker.waitForService(timeoutInMillis);
            if(service == null) {
                throw new ServiceNotFoundException(String.format("OSGi Service %s was not found after %d ms",
                        serviceInterface, timeoutInMillis));
            }

            return service;
        } catch(InterruptedException e) {
            throw new ServiceNotFoundException(String.format("Wait for OSGi service %s was interrrupted",
                    serviceInterface));
        } catch(InvalidSyntaxException e) {
            throw new ServiceNotFoundException(String.format("Invalid OSGi filter %", filter), e);
        } finally {
            if(tracker != null) {
                tracker.close();
            }
        }
    }
}
