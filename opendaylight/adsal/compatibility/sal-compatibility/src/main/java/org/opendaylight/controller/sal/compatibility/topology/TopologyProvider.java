/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.compatibility.topology;

import org.opendaylight.controller.sal.binding.api.data.DataChangeListener;
import org.opendaylight.controller.sal.binding.api.data.DataProviderService;
import org.opendaylight.controller.sal.topology.IPluginOutTopologyService;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TopologyId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;

public class TopologyProvider implements AutoCloseable{
    private static final Logger LOG = LoggerFactory.getLogger(TopologyProvider.class);
    private static final InstanceIdentifier<Link> PATH = InstanceIdentifier.builder(NetworkTopology.class)
            .child(Topology.class ,new TopologyKey(new TopologyId("flow:1")))
            .child(Link.class)
            .toInstance();
    private TopologyCommitHandler commitHandler;

    private ListenerRegistration<DataChangeListener> listenerRegistration;
    private IPluginOutTopologyService topologyPublisher;
    private DataProviderService dataService;

    public void startAdapter() {
        if(dataService == null){
            LOG.error("dataService not set");
            return;
        }
        commitHandler = new TopologyCommitHandler(dataService,topologyPublisher);
        listenerRegistration = dataService.registerDataChangeListener(PATH, commitHandler);
        LOG.info("TopologyProvider started");
    }

    @Override
    public void close() {
        if (listenerRegistration != null) {
            listenerRegistration.close();
        }
    }

    void setTopologyPublisher(final IPluginOutTopologyService topologyPublisher) {
        this.topologyPublisher = topologyPublisher;
        if (commitHandler != null) {
            commitHandler.setTopologyPublisher(topologyPublisher);
        }
    }

    public void setDataService(final DataProviderService dataService) {
        this.dataService = Preconditions.checkNotNull(dataService);
    }
}
