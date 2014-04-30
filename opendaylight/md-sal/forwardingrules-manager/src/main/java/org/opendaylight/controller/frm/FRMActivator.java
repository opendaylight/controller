/**
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 * 
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.frm;

import org.opendaylight.controller.frm.flow.FlowProvider;
import org.opendaylight.controller.frm.group.GroupProvider;
import org.opendaylight.controller.frm.meter.MeterProvider;
import org.opendaylight.controller.sal.binding.api.AbstractBindingAwareProvider;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker.ProviderContext;
import org.opendaylight.controller.sal.binding.api.data.DataProviderService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.service.rev130819.SalFlowService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.service.rev130918.SalGroupService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.meter.service.rev130918.SalMeterService;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FRMActivator extends AbstractBindingAwareProvider {

    private final static Logger LOG = LoggerFactory.getLogger(FRMActivator.class);

    private static FlowProvider flowProvider = new FlowProvider(); 
    private static GroupProvider groupProvider = new GroupProvider();
    private static MeterProvider meterProvider = new MeterProvider();
    
    @Override
    public void onSessionInitiated(final ProviderContext session) {
        DataProviderService flowSalService = session.<DataProviderService>getSALService(DataProviderService.class);
        FRMActivator.flowProvider.setDataService(flowSalService);
        SalFlowService rpcFlowSalService = session.<SalFlowService>getRpcService(SalFlowService.class);
        FRMActivator.flowProvider.setSalFlowService(rpcFlowSalService);
        FRMActivator.flowProvider.start();
        DataProviderService groupSalService = session.<DataProviderService>getSALService(DataProviderService.class);
        FRMActivator.groupProvider.setDataService(groupSalService);
        SalGroupService rpcGroupSalService = session.<SalGroupService>getRpcService(SalGroupService.class);
        FRMActivator.groupProvider.setSalGroupService(rpcGroupSalService);
        FRMActivator.groupProvider.start();
        DataProviderService meterSalService = session.<DataProviderService>getSALService(DataProviderService.class);
        FRMActivator.meterProvider.setDataService(meterSalService);
        SalMeterService rpcMeterSalService = session.<SalMeterService>getRpcService(SalMeterService.class);
        FRMActivator.meterProvider.setSalMeterService(rpcMeterSalService);
        FRMActivator.meterProvider.start();
    }
    
    @Override
    protected void stopImpl(final BundleContext context) {
        try {
            FRMActivator.flowProvider.close();
            FRMActivator.groupProvider.close();
            FRMActivator.meterProvider.close();
        } catch (Throwable e) {
            LOG.error("Unexpected error by stopping FRMActivator", e);
            throw new RuntimeException(e);
        }
    }
  }