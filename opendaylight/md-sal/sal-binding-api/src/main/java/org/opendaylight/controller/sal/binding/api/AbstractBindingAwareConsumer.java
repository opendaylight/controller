/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.binding.api;

import org.opendaylight.controller.sal.binding.api.BindingAwareBroker.ProviderContext;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

public abstract class AbstractBindingAwareConsumer extends AbstractBrokerAwareActivator implements BindingAwareConsumer {

    @Override
    protected final void onBrokerAvailable(BindingAwareBroker broker, BundleContext context) {
        broker.registerConsumer(this, context);
    }

}
