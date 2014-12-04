/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.compatibility.topology;

import static org.opendaylight.controller.sal.compatibility.topology.TopologyMapping.toAdEdge;
import static org.opendaylight.controller.sal.compatibility.topology.TopologyMapping.toTopoEdgeUpdate;

import java.util.Map.Entry;
import java.util.concurrent.CopyOnWriteArrayList;

import org.opendaylight.controller.md.sal.binding.util.TypeSafeDataReader;
import org.opendaylight.controller.md.sal.common.api.data.DataChangeEvent;
import org.opendaylight.controller.sal.binding.api.data.DataChangeListener;
import org.opendaylight.controller.sal.binding.api.data.DataProviderService;
import org.opendaylight.controller.sal.core.UpdateType;
import org.opendaylight.controller.sal.topology.IPluginOutTopologyService;
import org.opendaylight.controller.sal.topology.TopoEdgeUpdate;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TopologyId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TopologyCommitHandler implements DataChangeListener {
    private static final Logger LOG = LoggerFactory.getLogger(TopologyCommitHandler.class);

    private IPluginOutTopologyService topologyPublisher;

    private final DataProviderService dataService;

    public TopologyCommitHandler(final DataProviderService dataService, final IPluginOutTopologyService topologyPub) {
        this.topologyPublisher = topologyPub;
        this.dataService = dataService;
    }

    @Override
    public void onDataChanged(final DataChangeEvent<InstanceIdentifier<?>, DataObject> modification) {
        CopyOnWriteArrayList<TopoEdgeUpdate> msg = new CopyOnWriteArrayList<TopoEdgeUpdate>();
        try {
            TypeSafeDataReader reader = TypeSafeDataReader.forReader(dataService);
            InstanceIdentifier<Topology> topologyPath = InstanceIdentifier.builder(NetworkTopology.class)
                    .child(Topology.class, new TopologyKey(new TopologyId("flow:1"))).build();
            Topology topology = reader.readOperationalData(topologyPath);

            for (Entry<InstanceIdentifier<? extends DataObject>, DataObject> entry : modification
                    .getCreatedOperationalData().entrySet()) {
                if (entry.getValue() instanceof Link
                        && modification.getCreatedOperationalData().containsKey(entry.getKey())) {
                    msg.add(toTopoEdgeUpdate(toAdEdge((Link) entry.getValue(), topology), UpdateType.ADDED, reader));
                }
            }

            for (Entry<InstanceIdentifier<? extends DataObject>, DataObject> entry : modification
                    .getUpdatedOperationalData().entrySet()) {
                if (entry.getValue() instanceof Link) {
                    msg.add(toTopoEdgeUpdate(toAdEdge((Link) entry.getValue(), topology), UpdateType.CHANGED, reader));
                }
            }
            for (InstanceIdentifier<? extends DataObject> path : modification.getRemovedOperationalData()) {
                if (path.getTargetType() == Link.class) {
                    Link link = (Link) modification.getOriginalOperationalData().get(path);
                    msg.add(toTopoEdgeUpdate(toAdEdge(link, topology), UpdateType.REMOVED, reader));
                }

            }

            if (topologyPublisher != null && msg != null && !msg.isEmpty()) {
                topologyPublisher.edgeUpdate(msg);
            }

        } catch (Exception e) {
            LOG.error("Exception caught", e);
        }
    }

    protected IPluginOutTopologyService getTopologyPublisher() {
        return topologyPublisher;
    }

    protected void setTopologyPublisher(final IPluginOutTopologyService topologyPublisher) {
        this.topologyPublisher = topologyPublisher;
    }

}
