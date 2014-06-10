/**
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.frm.meter;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataChangeListener;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataBroker.DataChangeScope;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.sal.binding.api.RpcConsumerRegistry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowCapableNode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.meters.Meter;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.Nodes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yang.gen.v1.urn.opendaylight.meter.service.rev130918.SalMeterService;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;

/**
 * Meter Provider registers the {@link MeterChangeListener} and it holds all needed
 * services for {@link MeterChangeListener}.
 *
 * @author <a href="mailto:vdemcak@cisco.com">Vaclav Demcak</a>
 *
 */
public class MeterProvider implements AutoCloseable {

    private static final Logger LOG = LoggerFactory.getLogger(MeterProvider.class);

    private SalMeterService salMeterService;
    private DataBroker dataService;

    /* DataChangeListener */
    private DataChangeListener meterDataChangeListener;
    private ListenerRegistration<DataChangeListener> meterDataChangeListenerRegistration;

    /**
     * Provider Initialization Phase.
     *
     * @param DataProviderService dataService
     */
    public void init(final DataBroker dataService) {
        LOG.info("FRM Meter Config Provider initialization.");
        this.dataService = Preconditions.checkNotNull(dataService, "DataProviderService can not be null !");
    }

    /**
     * Listener Registration Phase
     *
     * @param RpcConsumerRegistry rpcRegistry
     */
    public void start(final RpcConsumerRegistry rpcRegistry) {
        Preconditions.checkArgument(rpcRegistry != null, "RpcConsumerRegistry can not be null !");
        this.salMeterService = Preconditions.checkNotNull(rpcRegistry.getRpcService(SalMeterService.class),
                "RPC SalMeterService not found.");

        /* Build Path */
        InstanceIdentifier<Meter> meterIdentifier = InstanceIdentifier.create(Nodes.class)
                .child(Node.class).augmentation(FlowCapableNode.class).child(Meter.class);

        /* DataChangeListener registration */
        this.meterDataChangeListener = new MeterChangeListener(MeterProvider.this);
        this.meterDataChangeListenerRegistration =
                this.dataService.registerDataChangeListener(LogicalDatastoreType.CONFIGURATION,
                        meterIdentifier, meterDataChangeListener, DataChangeScope.SUBTREE);

        LOG.info("FRM Meter Config Provider started.");
    }

    @Override
    public void close() {
        LOG.info("FRM Meter Config Provider stopped.");
        if (meterDataChangeListenerRegistration != null) {
            try {
                meterDataChangeListenerRegistration.close();
            } catch (Exception e) {
                String errMsg = "Error by stop FRM Meter Config Provider.";
                LOG.error(errMsg, e);
                throw new IllegalStateException(errMsg, e);
            } finally {
                meterDataChangeListenerRegistration = null;
            }
        }
    }

    public DataChangeListener getMeterDataChangeListener() {
        return meterDataChangeListener;
    }

    public DataBroker getDataService() {
        return dataService;
    }

    public SalMeterService getSalMeterService() {
        return salMeterService;
    }
}
