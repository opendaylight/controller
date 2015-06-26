/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.netconf.auth.usermanager;

import java.util.Hashtable;
import org.opendaylight.controller.netconf.auth.AuthConstants;
import org.opendaylight.controller.netconf.auth.AuthProvider;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

public class AuthProviderActivator implements BundleActivator {

    public static final int PREFERENCE = 0;
    private ServiceRegistration<AuthProvider> authProviderServiceRegistration;

    @Override
    public void start(final BundleContext context) throws Exception {
        final AuthProvider authProvider = new AuthProviderImpl(context);
        // Set preference of this service to 0
        final Hashtable<String, Object> properties = new Hashtable<>(1);
        properties.put(AuthConstants.SERVICE_PREFERENCE_KEY, PREFERENCE);

        authProviderServiceRegistration = context.registerService(AuthProvider.class, authProvider, properties);
    }

    @Override
    public void stop(final BundleContext context) throws Exception {
        if(authProviderServiceRegistration != null) {
            authProviderServiceRegistration.unregister();
        }
    }
}
