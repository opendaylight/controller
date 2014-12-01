/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.netconf.auth.usermanager;

import com.google.common.annotations.VisibleForTesting;
import org.opendaylight.controller.netconf.auth.AuthProvider;
import org.opendaylight.controller.sal.authorization.AuthResultEnum;
import org.opendaylight.controller.usermanager.IUserManager;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * AuthProvider implementation delegating to AD-SAL UserManager instance.
 */
public class AuthProviderImpl implements AuthProvider {
    private static final Logger LOG = LoggerFactory.getLogger(AuthProviderImpl.class);

    private IUserManager nullableUserManager;

    public AuthProviderImpl(final BundleContext bundleContext) {

        final ServiceTrackerCustomizer<IUserManager, IUserManager> customizer = new ServiceTrackerCustomizer<IUserManager, IUserManager>() {
            @Override
            public IUserManager addingService(final ServiceReference<IUserManager> reference) {
                LOG.trace("UerManager {} added", reference);
                nullableUserManager = bundleContext.getService(reference);
                return nullableUserManager;
            }

            @Override
            public void modifiedService(final ServiceReference<IUserManager> reference, final IUserManager service) {
                LOG.trace("Replacing modified UerManager {}", reference);
                nullableUserManager = service;
            }

            @Override
            public void removedService(final ServiceReference<IUserManager> reference, final IUserManager service) {
                LOG.trace("Removing UerManager {}. This AuthProvider will fail to authenticate every time", reference);
                synchronized (AuthProviderImpl.this) {
                    nullableUserManager = null;
                }
            }
        };
        final ServiceTracker<IUserManager, IUserManager> listenerTracker = new ServiceTracker<>(bundleContext, IUserManager.class, customizer);
        listenerTracker.open();
    }

    /**
     * Authenticate user. This implementation tracks IUserManager and delegates the decision to it. If the service is not
     * available, IllegalStateException is thrown.
     */
    @Override
    public synchronized boolean authenticated(final String username, final String password) {
        if (nullableUserManager == null) {
            LOG.warn("Cannot authenticate user '{}', user manager service is missing", username);
            throw new IllegalStateException("User manager service is not available");
        }
        final AuthResultEnum authResult = nullableUserManager.authenticate(username, password);
        LOG.debug("Authentication result for user '{}' : {}", username, authResult);
        return authResult.equals(AuthResultEnum.AUTH_ACCEPT) || authResult.equals(AuthResultEnum.AUTH_ACCEPT_LOC);
    }

    @VisibleForTesting
    synchronized void setNullableUserManager(final IUserManager nullableUserManager) {
        this.nullableUserManager = nullableUserManager;
    }
}
