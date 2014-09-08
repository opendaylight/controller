/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.test.benchmark;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.dom.api.DOMDataBroker;
import org.opendaylight.controller.sal.binding.api.AbstractBindingAwareProvider;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker.ProviderContext;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker.RpcRegistration;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.test.benchmark.rev140701.OpendaylightMdsalTestBenchmarkService;
import org.osgi.framework.BundleContext;
import org.osgi.util.tracker.ServiceTracker;

public class Activator extends AbstractBindingAwareProvider {

    private ServiceTracker<DOMDataBroker, DOMDataBroker> domBroker;
    private RpcRegistration<OpendaylightMdsalTestBenchmarkService> registration;

    @Override
    protected void startImpl(BundleContext context) {
        domBroker = new ServiceTracker<>(context, DOMDataBroker.class, null);
        domBroker.open();
    }

    @Override
    public void onSessionInitiated(ProviderContext session) {
        DataBroker bindingBroker = session.getSALService(DataBroker.class);
        BenchmarkServiceImpl benchmark = new BenchmarkServiceImpl(bindingBroker, domBroker.getService());
        registration = session.addRpcImplementation(OpendaylightMdsalTestBenchmarkService.class, benchmark);
    }

    @Override
    protected void onBrokerRemoved(BindingAwareBroker broker, BundleContext context) {
        registration.close();
    }

    @Override
    protected void stopImpl(BundleContext context) {
        super.stopImpl(context);
        domBroker.close();
    }

}
