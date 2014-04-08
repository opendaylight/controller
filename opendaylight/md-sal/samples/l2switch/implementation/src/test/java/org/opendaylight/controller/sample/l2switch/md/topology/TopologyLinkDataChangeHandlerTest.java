/**
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sample.l2switch.md.topology;

import org.junit.Before;
import org.junit.Test;
import org.opendaylight.controller.sample.l2switch.md.util.InstanceIdentifierUtils;
import org.opendaylight.controller.md.sal.common.api.data.DataChangeEvent;
import org.opendaylight.controller.sal.binding.api.data.DataBrokerService;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 */
public class TopologyLinkDataChangeHandlerTest {
  NetworkGraphService networkGraphService;
  DataBrokerService dataBrokerService;
  DataChangeEvent dataChangeEvent;
  Topology topology;
  Link link;

  @Before
  public void init() {
    networkGraphService = mock(NetworkGraphService.class);
    dataBrokerService = mock(DataBrokerService.class);
    dataChangeEvent = mock(DataChangeEvent.class);
    link = mock(Link.class);
    topology = mock(Topology.class);
  }

  @Test
  public void testOnDataChange() throws Exception {
    TopologyLinkDataChangeHandler topologyLinkDataChangeHandler = new TopologyLinkDataChangeHandler(dataBrokerService, networkGraphService, 2);
    Map<InstanceIdentifier<?>, DataObject> original = new HashMap<InstanceIdentifier<?>, DataObject>();
    InstanceIdentifier<?> instanceIdentifier = InstanceIdentifierUtils.generateTopologyInstanceIdentifier("flow:1");
    DataObject dataObject = mock(DataObject.class);
    Map<InstanceIdentifier<?>, DataObject> updated = new HashMap<InstanceIdentifier<?>, DataObject>();
    updated.put(instanceIdentifier, dataObject);
    when(dataChangeEvent.getUpdatedOperationalData()).thenReturn(updated);
    when(dataChangeEvent.getOriginalOperationalData()).thenReturn(original);
    List<Link> links = new ArrayList<>();
    links.add(link);
    when(dataBrokerService.readOperationalData(instanceIdentifier)).thenReturn(topology);
    when(topology.getLink()).thenReturn(links);

    topologyLinkDataChangeHandler.onDataChanged(dataChangeEvent);
    Thread.sleep(2100);
    verify(networkGraphService, times(1)).addLinks(links);
  }
}
