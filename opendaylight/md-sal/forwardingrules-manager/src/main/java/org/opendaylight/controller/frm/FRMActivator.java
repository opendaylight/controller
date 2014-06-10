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
import org.opendaylight.controller.frm.reconil.FlowNodeReconcilProvider;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.sal.binding.api.AbstractBindingAwareProvider;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker.ProviderContext;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Forwarding Rules Manager Activator
 *
 * Activator manages all Providers ({@link FlowProvider}, {@link GroupProvider},
 * {@link MeterProvider} and the {@link FlowNodeReconcilProvider}).
 * It registers all listeners (DataChangeEvent, ReconcilNotification)
 * in the Session Initialization phase.
 *
 * @author <a href="mailto:vdemcak@cisco.com">Vaclav Demcak</a>
 * *
 */
public class FRMActivator extends AbstractBindingAwareProvider {

    private final static Logger LOG = LoggerFactory.getLogger(FRMActivator.class);

    private final FlowProvider flowProvider;
    private final GroupProvider groupProvider;
    private final MeterProvider meterProvider;
    private final FlowNodeReconcilProvider flowNodeReconcilProvider;

    public FRMActivator() {
        this.flowProvider = new FlowProvider();
        this.groupProvider = new GroupProvider();
        this.meterProvider = new MeterProvider();
        this.flowNodeReconcilProvider = new FlowNodeReconcilProvider();
    }

    @Override
    public void onSessionInitiated(final ProviderContext session) {
        LOG.info("FRMActivator initialization.");
        /* Flow */
        try {
            final DataBroker flowSalService = session.getSALService(DataBroker.class);
            this.flowProvider.init(flowSalService);
            this.flowProvider.start(session);
            /* Group */
            final DataBroker groupSalService = session.getSALService(DataBroker.class);
            this.groupProvider.init(groupSalService);
            this.groupProvider.start(session);
            /* Meter */
            final DataBroker meterSalService = session.getSALService(DataBroker.class);
            this.meterProvider.init(meterSalService);
            this.meterProvider.start(session);
            /* FlowNode Reconciliation */
            final DataBroker dbs = session.getSALService(DataBroker.class);
            this.flowNodeReconcilProvider.init(dbs);
            this.flowNodeReconcilProvider.start(session);

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
            this.flowProvider.close();
            this.groupProvider.close();
            this.meterProvider.close();
            this.flowNodeReconcilProvider.close();
        } catch (Exception e) {
            LOG.error("Unexpected error by stopping FRMActivator", e);
        }
    }
  }