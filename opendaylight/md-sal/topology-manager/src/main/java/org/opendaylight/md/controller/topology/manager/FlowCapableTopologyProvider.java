/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.md.controller.topology.manager;

import java.util.concurrent.ExecutionException;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.ReadWriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.sal.binding.api.NotificationProviderService;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TopologyId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyKey;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.binding.NotificationListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FlowCapableTopologyProvider implements AutoCloseable {
    private final static Logger LOG = LoggerFactory.getLogger(FlowCapableTopologyProvider.class);
    private ListenerRegistration<NotificationListener> listenerRegistration;
    private Thread thread;

    private DataBroker dataBroker;
    private NotificationProviderService notificationService;

    public FlowCapableTopologyProvider(DataBroker dataBroker, NotificationProviderService notificationService) {
        this.dataBroker = dataBroker;
        this.notificationService = notificationService;
    }

    /**
     * Initializes topology provider.
     */
    public synchronized void intialize() {
        final String name = "flow:1";
        final TopologyKey key = new TopologyKey(new TopologyId(name));
        final InstanceIdentifier<Topology> path = InstanceIdentifier
                .create(NetworkTopology.class)
                .child(Topology.class, key);

        final OperationProcessor processor = new OperationProcessor(dataBroker);
        final FlowCapableTopologyExporter listener = new FlowCapableTopologyExporter(processor, path);
        this.listenerRegistration = notificationService.registerNotificationListener(listener);

        final ReadWriteTransaction tx = dataBroker.newReadWriteTransaction();
        tx.put(LogicalDatastoreType.OPERATIONAL, path, new TopologyBuilder().setKey(key).build(), true);
        try {
            tx.submit().get();
        } catch (InterruptedException | ExecutionException e) {
            LOG.warn("Initial topology export failed, continuing anyway", e);
        }

        thread = new Thread(processor);
        thread.setDaemon(true);
        thread.setName("FlowCapableTopologyExporter-" + name);
        thread.start();
    }

    @Override
    public synchronized void close() throws InterruptedException {
        LOG.info("FlowCapableTopologyProvider stopped.");
        if (this.listenerRegistration != null) {
            try {
                this.listenerRegistration.close();
            } catch (Exception e) {
                LOG.error("Failed to close listener registration", e);
            }
            listenerRegistration = null;
        }
        if (thread != null) {
            thread.interrupt();
            thread.join();
            thread = null;
        }
    }

}
