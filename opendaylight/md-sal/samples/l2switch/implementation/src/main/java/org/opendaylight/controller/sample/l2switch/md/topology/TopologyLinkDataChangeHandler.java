/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sample.l2switch.md.topology;

import com.google.common.base.Preconditions;
import org.opendaylight.controller.sample.l2switch.md.util.InstanceIdentifierUtils;
import org.opendaylight.controller.md.sal.common.api.data.DataChangeEvent;
import org.opendaylight.controller.sal.binding.api.data.DataBrokerService;
import org.opendaylight.controller.sal.binding.api.data.DataChangeListener;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TopologyId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Listens to data change events on topology links
 * {@link org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link}
 * and maintains a topology graph using provided NetworkGraphService
 * {@link org.opendaylight.controller.sample.l2switch.md.topology.NetworkGraphService}.
 * It refreshes the graph after a delay(default 10 sec) to accommodate burst of change events if they come in bulk.
 * This is to avoid continuous refresh of graph on a series of change events in short time.
 */
public class TopologyLinkDataChangeHandler implements DataChangeListener {
  private static final Logger _logger = LoggerFactory.getLogger(TopologyLinkDataChangeHandler.class);
  private static final String DEFAULT_TOPOLOGY_ID = "flow:1";

  private boolean networkGraphRefreshScheduled = false;
  private final ScheduledExecutorService networkGraphRefreshScheduler = Executors.newScheduledThreadPool(1);
  private final long DEFAULT_GRAPH_REFRESH_DELAY = 10;
  private final long graphRefreshDelayInSec;

  private final NetworkGraphService networkGraphService;
  private final DataBrokerService dataBrokerService;

  /**
   * Uses default delay to refresh topology graph if this constructor is used.
   * @param dataBrokerService
   * @param networkGraphService
   */
  public TopologyLinkDataChangeHandler(DataBrokerService dataBrokerService, NetworkGraphService networkGraphService) {
    Preconditions.checkNotNull(dataBrokerService, "dataBrokerService should not be null.");
    Preconditions.checkNotNull(networkGraphService, "networkGraphService should not be null.");
    this.dataBrokerService = dataBrokerService;
    this.networkGraphService = networkGraphService;
    this.graphRefreshDelayInSec = DEFAULT_GRAPH_REFRESH_DELAY;
  }

  /**
   *
   * @param dataBrokerService
   * @param networkGraphService
   * @param graphRefreshDelayInSec
   */
  public TopologyLinkDataChangeHandler(DataBrokerService dataBrokerService, NetworkGraphService networkGraphService,
                                       long graphRefreshDelayInSec) {
    Preconditions.checkNotNull(dataBrokerService, "dataBrokerService should not be null.");
    Preconditions.checkNotNull(networkGraphService, "networkGraphService should not be null.");
    this.dataBrokerService = dataBrokerService;
    this.networkGraphService = networkGraphService;
    this.graphRefreshDelayInSec = graphRefreshDelayInSec;
  }

  /**
   * Based on if links have been added or removed in topology data store, schedules a refresh of network graph.
   * @param dataChangeEvent
   */
  @Override
  public void onDataChanged(DataChangeEvent<InstanceIdentifier<?>, DataObject> dataChangeEvent) {
    if(dataChangeEvent == null) {
      _logger.info("In onDataChanged: No Processing done as dataChangeEvent is null.");
    }
    Map<InstanceIdentifier<?>, DataObject> linkOriginalData = dataChangeEvent.getOriginalOperationalData();
    Map<InstanceIdentifier<?>, DataObject> linkUpdatedData = dataChangeEvent.getUpdatedOperationalData();
    // change this logic, once MD-SAL start populating DeletedOperationData Set
    if(linkOriginalData != null && linkUpdatedData != null
        && (linkOriginalData.size() != 0 || linkUpdatedData.size() != 0)
        && !networkGraphRefreshScheduled) {
      networkGraphRefreshScheduled = linkOriginalData.size() != linkUpdatedData.size();
      if(networkGraphRefreshScheduled) {
        networkGraphRefreshScheduler.schedule(new NetworkGraphRefresher(), graphRefreshDelayInSec, TimeUnit.SECONDS);
      }
    }

  }

  /**
   * Registers as a data listener to receive changes done to
   * {@link org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link}
   * under {@link org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology}
   * operation data root.
   */

  public void registerAsDataChangeListener() {
    InstanceIdentifier<Link> linkInstance = InstanceIdentifier.builder(NetworkTopology.class)
        .child(Topology.class, new TopologyKey(new TopologyId(DEFAULT_TOPOLOGY_ID))).child(Link.class).toInstance();
    dataBrokerService.registerDataChangeListener(linkInstance, this);
  }

  /**
   *
   */
  private class NetworkGraphRefresher implements Runnable {
    /**
     *
     */
    @Override
    public void run() {
      networkGraphRefreshScheduled = false;
      //TODO: it should refer to changed links only from DataChangeEvent above.
      List<Link> links = getLinksFromTopology(DEFAULT_TOPOLOGY_ID);
      networkGraphService.clear();// can remove this once changed links are addressed
      if(links != null && !links.isEmpty()) {
        networkGraphService.addLinks(links);
      }
    }

    /**
     * @param topologyId
     * @return
     */
    private List<Link> getLinksFromTopology(String topologyId) {
      InstanceIdentifier<Topology> topologyInstanceIdentifier = InstanceIdentifierUtils.generateTopologyInstanceIdentifier(topologyId);
      Topology topology = (Topology) dataBrokerService.readOperationalData(topologyInstanceIdentifier);
      return topology.getLink();
    }
  }
}
