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

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

public abstract class AbstractConsumer implements Consumer, BundleActivator {

    Broker broker;
    ServiceReference<Broker> brokerRef;
    @Override
    public final void start(BundleContext context) throws Exception {
        this.startImpl(context);
        brokerRef = context.getServiceReference(Broker.class);
        broker = context.getService(brokerRef);
        broker.registerConsumer(this,context);
    }



    @Override
    public final void stop(BundleContext context) throws Exception {
        stopImpl(context);
        broker = null;
        if(brokerRef != null) {
            context.ungetService(brokerRef);
        }
    }

    protected void startImpl(BundleContext context) {
        // NOOP
    }
    protected void stopImpl(BundleContext context) {
        // NOOP
    }

    @Override
    public Collection<ConsumerFunctionality> getConsumerFunctionality() {
        return Collections.emptySet();
    }

}
