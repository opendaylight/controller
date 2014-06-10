/**
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.frm;

import org.opendaylight.controller.sal.binding.api.NotificationProviderService;
import org.opendaylight.controller.sal.binding.api.RpcConsumerRegistry;
import org.opendaylight.controller.sal.binding.api.data.DataModificationTransaction;
import org.opendaylight.controller.sal.binding.api.data.DataProviderService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.service.rev130819.SalFlowService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.service.rev130918.SalGroupService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.meter.service.rev130918.SalMeterService;
import org.opendaylight.yangtools.concepts.Registration;
import org.opendaylight.yangtools.yang.binding.NotificationListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * forwardingrules-manager
 * org.opendaylight.controller.frm
 *
 *
 *
 * @author <a href="mailto:vdemcak@cisco.com">Vaclav Demcak</a>
 *
 * Created: Jun 13, 2014
 */
public class FlowNodeReconcilProvider implements AutoCloseable {

    private final static Logger LOG = LoggerFactory.getLogger(FlowNodeReconcilProvider.class);

    private SalFlowService salFlowService;
    private SalMeterService salMeterService;
    private SalGroupService salGroupService;
    private DataProviderService dataService;
    private NotificationProviderService notificationService;

    /* Reconciliation NotificationListener */
    private final FlowNodeReconcilListener flowNodeReconcilListener =
            new FlowNodeReconcilListener( FlowNodeReconcilProvider.this );
    private Registration<NotificationListener> flowNodeReconcilListnenerRegistration;

    public void init (final DataProviderService dataService,
                      final NotificationProviderService nps) {
        this.dataService = dataService;
        this.notificationService = nps;
    }

    public void start( final RpcConsumerRegistry rpcRegistry ) {

        this.salFlowService = rpcRegistry.<SalFlowService>getRpcService(SalFlowService.class);
        this.salMeterService = rpcRegistry.<SalMeterService>getRpcService(SalMeterService.class);
        this.salGroupService = rpcRegistry.<SalGroupService>getRpcService(SalGroupService.class);
        /* ReconcilNotificationListener registration */
        this.flowNodeReconcilListnenerRegistration =
                this.notificationService.registerNotificationListener(this.flowNodeReconcilListener);
        LOG.info("FRM Flow Node Config Provider started.");
    }

    protected DataModificationTransaction startChange() {
        return this.dataService.beginTransaction();
    }

    @Override
    public void close() throws Exception {
        try {
            LOG.info("FRM Flow Node Config Provider stopped.");
            if (flowNodeReconcilListnenerRegistration != null) {
                flowNodeReconcilListnenerRegistration.close();
            }
        } catch (Exception e) {
            String errMsg = "Error by stop FRM Flow Node Config Provider.";
            LOG.error(errMsg, e);
            throw new RuntimeException(errMsg, e);
        }
    }

    public NotificationProviderService getNotificationService() {
        return notificationService;
    }

    public DataProviderService getDataService() {
        return dataService;
    }

    public SalFlowService getSalFlowService() {
        return salFlowService;
    }

    public SalMeterService getSalMeterService() {
        return salMeterService;
    }

    public SalGroupService getSalGroupService() {
        return salGroupService;
    }
}
