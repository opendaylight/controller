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
import org.opendaylight.controller.sal.binding.api.NotificationProviderService;
import org.opendaylight.controller.sal.binding.api.data.DataProviderService;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FRMActivator extends AbstractBindingAwareProvider {

    private final static Logger LOG = LoggerFactory.getLogger(FRMActivator.class);

    private static FlowProvider flowProvider = new FlowProvider();
    private static GroupProvider groupProvider = new GroupProvider();
    private static MeterProvider meterProvider = new MeterProvider();
    private static FlowNodeReconcilProvider flowNodeReconcilProvider =
            new FlowNodeReconcilProvider();

    @Override
    public void onSessionInitiated(final ProviderContext session) {
        LOG.info("FRMActivator is starting.");
        /* Flow */
        try {
            final DataProviderService flowSalService = session.getSALService(DataProviderService.class);
            final NotificationProviderService salNotificationService =
                    session.getSALService(NotificationProviderService.class);
            FRMActivator.flowProvider.init(flowSalService, salNotificationService);
            FRMActivator.flowProvider.start(session);
            /* Group */
            final DataProviderService groupSalService = session.getSALService(DataProviderService.class);
            FRMActivator.groupProvider.init(groupSalService);
            FRMActivator.groupProvider.start(session);
            /* Meter */
            final DataProviderService meterSalService = session.getSALService(DataProviderService.class);
            FRMActivator.meterProvider.init(meterSalService);
            FRMActivator.meterProvider.start(session);
            /* FlowNode Reconciliation */
            final DataProviderService dps = session.getSALService(DataProviderService.class);
            final NotificationProviderService nps = session.getSALService(NotificationProviderService.class);
            FRMActivator.flowNodeReconcilProvider.init(dps, nps);
            FRMActivator.flowNodeReconcilProvider.start(session);

            LOG.info("FRMActivator started successfully");
        } catch (Exception e) {
            String errMsg = "Unexpected error by starting FRMActivator";
            LOG.error(errMsg, e);
            throw new IllegalStateException(errMsg, e);
        }
    }

    @Override
    protected void stopImpl(final BundleContext context) {
        try {
            FRMActivator.flowProvider.close();
            FRMActivator.groupProvider.close();
            FRMActivator.meterProvider.close();
            FRMActivator.flowNodeReconcilProvider.close();
        } catch (Exception e) {
            String errMsg = "Unexpected error by stopping FRMActivator";
            LOG.error(errMsg, e);
            throw new IllegalStateException(errMsg, e);
        }
    }
  }