/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 * 
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.frm

import org.opendaylight.controller.frm.flow.FlowProvider
import org.opendaylight.controller.frm.group.GroupProvider
import org.opendaylight.controller.frm.meter.MeterProvider
import org.opendaylight.controller.sal.binding.api.AbstractBindingAwareProvider
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker.ProviderContext
import org.opendaylight.controller.sal.binding.api.data.DataProviderService
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.service.rev130819.SalFlowService
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.service.rev130918.SalGroupService
import org.osgi.framework.BundleContext
import org.opendaylight.yang.gen.v1.urn.opendaylight.meter.service.rev130918.SalMeterService

class FRMActivator extends AbstractBindingAwareProvider {

    static var FlowProvider provider = new FlowProvider();
    static var GroupProvider groupProvider = new GroupProvider();
    static var MeterProvider meterProvider = new MeterProvider();

    override onSessionInitiated(ProviderContext session) {
        provider.dataService = session.getSALService(DataProviderService)
        provider.salFlowService = session.getRpcService(SalFlowService);
        provider.start();
        
        groupProvider.dataService = session.getSALService(DataProviderService)
        groupProvider.salGroupService = session.getRpcService(SalGroupService)
        groupProvider.start();
        
        meterProvider.dataService = session.getSALService(DataProviderService)
        meterProvider.salMeterService = session.getRpcService(SalMeterService)
        meterProvider.start();
    }

    override protected stopImpl(BundleContext context) {
        provider.close();
        groupProvider.close();
        meterProvider.close();
    }

}
