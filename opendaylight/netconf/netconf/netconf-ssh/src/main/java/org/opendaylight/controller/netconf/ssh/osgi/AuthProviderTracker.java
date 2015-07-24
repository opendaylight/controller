/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.netconf.ssh.osgi;

import com.google.common.base.Preconditions;
import org.apache.sshd.server.PasswordAuthenticator;
import org.apache.sshd.server.session.ServerSession;
import org.opendaylight.controller.netconf.auth.AuthConstants;
import org.opendaylight.controller.netconf.auth.AuthProvider;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class AuthProviderTracker implements ServiceTrackerCustomizer<AuthProvider, AuthProvider>, PasswordAuthenticator {
    private static final Logger LOG = LoggerFactory.getLogger(AuthProviderTracker.class);

    private final BundleContext bundleContext;

    private Integer maxPreference;
    private final ServiceTracker<AuthProvider, AuthProvider> listenerTracker;
    private AuthProvider authProvider;

    public AuthProviderTracker(final BundleContext bundleContext) {
        this.bundleContext = bundleContext;
        listenerTracker = new ServiceTracker<>(bundleContext, AuthProvider.class, this);
        listenerTracker.open();
    }

    @Override
    public AuthProvider addingService(final ServiceReference<AuthProvider> reference) {
        LOG.trace("Service {} added", reference);
        final AuthProvider authService = bundleContext.getService(reference);
        final Integer newServicePreference = getPreference(reference);
        if(isBetter(newServicePreference)) {
            maxPreference = newServicePreference;
            this.authProvider = authService;
        }
        return authService;
    }

    private static Integer getPreference(final ServiceReference<AuthProvider> reference) {
        final Object preferenceProperty = reference.getProperty(AuthConstants.SERVICE_PREFERENCE_KEY);
        return preferenceProperty == null ? Integer.MIN_VALUE : Integer.valueOf(preferenceProperty.toString());
    }

    private boolean isBetter(final Integer newServicePreference) {
        Preconditions.checkNotNull(newServicePreference);
        if(maxPreference == null) {
            return true;
        }

        return newServicePreference > maxPreference;
    }

    @Override
    public void modifiedService(final ServiceReference<AuthProvider> reference, final AuthProvider service) {
        final AuthProvider authService = bundleContext.getService(reference);
        final Integer newServicePreference = getPreference(reference);
        if(isBetter(newServicePreference)) {
            LOG.trace("Replacing modified service {} in netconf SSH.", reference);
            this.authProvider = authService;
        }
    }

    @Override
    public void removedService(final ServiceReference<AuthProvider> reference, final AuthProvider service) {
        LOG.trace("Removing service {} from netconf SSH. {}", reference,
                " SSH won't authenticate users until AuthProvider service will be started.");
        maxPreference = null;
        this.authProvider = null;
    }

    public void stop() {
        listenerTracker.close();
        // sshThread should finish normally since sshServer.close stops processing
    }

    @Override
    public boolean authenticate(final String username, final String password, final ServerSession session) {
        if (authProvider == null) {
            LOG.warn("AuthProvider is missing, failing authentication");
            return false;
        }
        return authProvider.authenticated(username, password);
    }
}
