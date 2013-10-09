/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.core.api;

import java.util.Collection;
import java.util.Collections;

import org.opendaylight.controller.sal.core.api.Broker.ProviderSession;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

public abstract class AbstractProvider implements BundleActivator, Provider {

    private ServiceReference<Broker> brokerRef;
    private Broker broker;

    @Override
    public Collection<ProviderFunctionality> getProviderFunctionality() {
        return Collections.emptySet();
    }

    @Override
    public final void start(BundleContext context) throws Exception {
        brokerRef = context.getServiceReference(Broker.class);
        broker = context.getService(brokerRef);

        this.startImpl(context);

        broker.registerProvider(this,context);
    }

    protected void startImpl(BundleContext context) {
        // NOOP
    }
    protected void stopImpl(BundleContext context) {
        // NOOP
    }

    @Override
    public final void stop(BundleContext context) throws Exception {
        stopImpl(context);
    }

}
