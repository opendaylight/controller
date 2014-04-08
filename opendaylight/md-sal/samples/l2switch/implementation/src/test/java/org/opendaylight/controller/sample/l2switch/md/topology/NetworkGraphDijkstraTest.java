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
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.link.attributes.Destination;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.link.attributes.Source;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;

import java.util.ArrayList;
import java.util.List;

import static junit.framework.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 */
public class NetworkGraphDijkstraTest {
  Link link1, link2, link3, link4, link5, link6, link7, link8, link9, link10,link11,link12;
  Destination dest1, dest2, dest3, dest4, dest5, dest6,dest7,dest8,dest9,dest10,dest11,dest12;
  Source src1, src2, src3, src4, src5, src6,src7,src8,src9,src10,src11,src12;
  NodeId nodeId1 = new NodeId("openflow:1");
  NodeId nodeId2 = new NodeId("openflow:2");
  NodeId nodeId3 = new NodeId("openflow:3");
  NodeId nodeId4 = new NodeId("openflow:4");
  NodeId nodeId5 = new NodeId("openflow:5");
  NodeId nodeId6 = new NodeId("openflow:6");
  NodeId nodeId7 = new NodeId("openflow:7");
  List<Link> links = new ArrayList<>();

  @Before
  public void init() {
    link1 = mock(Link.class);
    link2 = mock(Link.class);
    link3 = mock(Link.class);
    link4 = mock(Link.class);
    link5 = mock(Link.class);
    link6 = mock(Link.class);
    link7 = mock(Link.class);
    link8 = mock(Link.class);
    link9 = mock(Link.class);
    link10 = mock(Link.class);
    link11 = mock(Link.class);
    link12 = mock(Link.class);
    dest1 = mock(Destination.class);
    dest2 = mock(Destination.class);
    dest3 = mock(Destination.class);
    dest4 = mock(Destination.class);
    dest5 = mock(Destination.class);
    dest6 = mock(Destination.class);
    dest7 = mock(Destination.class);
    dest8 = mock(Destination.class);
    dest9 = mock(Destination.class);
    dest10 = mock(Destination.class);
    dest11 = mock(Destination.class);
    dest12 = mock(Destination.class);
    src1 = mock(Source.class);
    src2 = mock(Source.class);
    src3 = mock(Source.class);
    src4 = mock(Source.class);
    src5 = mock(Source.class);
    src6 = mock(Source.class);
    src7 = mock(Source.class);
    src8 = mock(Source.class);
    src9 = mock(Source.class);
    src10 = mock(Source.class);
    src11 = mock(Source.class);
    src12 = mock(Source.class);
    when(link1.getSource()).thenReturn(src1);
    when(link2.getSource()).thenReturn(src2);
    when(link3.getSource()).thenReturn(src3);
    when(link4.getSource()).thenReturn(src4);
    when(link5.getSource()).thenReturn(src5);
    when(link6.getSource()).thenReturn(src6);
    when(link7.getSource()).thenReturn(src7);
    when(link8.getSource()).thenReturn(src8);
    when(link9.getSource()).thenReturn(src9);
    when(link10.getSource()).thenReturn(src10);
    when(link11.getSource()).thenReturn(src11);
    when(link12.getSource()).thenReturn(src12);
    when(link1.getDestination()).thenReturn(dest1);
    when(link2.getDestination()).thenReturn(dest2);
    when(link3.getDestination()).thenReturn(dest3);
    when(link4.getDestination()).thenReturn(dest4);
    when(link5.getDestination()).thenReturn(dest5);
    when(link6.getDestination()).thenReturn(dest6);
    when(link7.getDestination()).thenReturn(dest7);
    when(link8.getDestination()).thenReturn(dest8);
    when(link9.getDestination()).thenReturn(dest9);
    when(link10.getDestination()).thenReturn(dest10);
    when(link11.getDestination()).thenReturn(dest11);
    when(link12.getDestination()).thenReturn(dest12);
    when(src1.getSourceNode()).thenReturn(nodeId1);
    when(dest1.getDestNode()).thenReturn(nodeId2);
    when(src2.getSourceNode()).thenReturn(nodeId2);
    when(dest2.getDestNode()).thenReturn(nodeId1);
    when(src3.getSourceNode()).thenReturn(nodeId1);
    when(dest3.getDestNode()).thenReturn(nodeId3);
    when(src4.getSourceNode()).thenReturn(nodeId3);
    when(dest4.getDestNode()).thenReturn(nodeId1);
    when(src5.getSourceNode()).thenReturn(nodeId2);
    when(dest5.getDestNode()).thenReturn(nodeId4);
    when(src6.getSourceNode()).thenReturn(nodeId4);
    when(dest6.getDestNode()).thenReturn(nodeId2);
    when(src7.getSourceNode()).thenReturn(nodeId2);
    when(dest7.getDestNode()).thenReturn(nodeId5);
    when(src8.getSourceNode()).thenReturn(nodeId5);
    when(dest8.getDestNode()).thenReturn(nodeId2);
    when(src9.getSourceNode()).thenReturn(nodeId6);
    when(dest9.getDestNode()).thenReturn(nodeId3);
    when(src10.getSourceNode()).thenReturn(nodeId3);
    when(dest10.getDestNode()).thenReturn(nodeId6);
    when(src11.getSourceNode()).thenReturn(nodeId7);
    when(dest11.getDestNode()).thenReturn(nodeId3);
    when(src12.getSourceNode()).thenReturn(nodeId3);
    when(dest12.getDestNode()).thenReturn(nodeId7);
    links.add(link1);
    links.add(link2);
    links.add(link3);
    links.add(link4);
    links.add(link5);
    links.add(link6);
    links.add(link7);
    links.add(link8);
    links.add(link9);
    links.add(link10);
    links.add(link11);
    links.add(link12);

  }

  @Test
  public void testAddLinksAndGetPath() throws Exception {
    NetworkGraphService networkGraphService = new NetworkGraphDijkstra();
    networkGraphService.addLinks(links);
    List<Link> path = networkGraphService.getPath(nodeId2, nodeId3);
    assertEquals("path size is not as expected.", 2, path.size());
    assertEquals("link source is not as expected.", nodeId2, path.get(0).getSource().getSourceNode());
    assertEquals("link destination is not as expected.", nodeId1, path.get(0).getDestination().getDestNode());
    path = networkGraphService.getPath(nodeId3, nodeId2);
    assertEquals("path size is not as expected.", 2, path.size());
    assertEquals("link source is not as expected.", nodeId3, path.get(0).getSource().getSourceNode());
    assertEquals("link destination is not as expected.", nodeId1, path.get(0).getDestination().getDestNode());

    path = networkGraphService.getPath(nodeId4, nodeId6);
    assertEquals("path size is not as expected.", 4, path.size());
    assertEquals("link source is not as expected.", nodeId4, path.get(0).getSource().getSourceNode());
    assertEquals("link destination is not as expected.", nodeId2, path.get(0).getDestination().getDestNode());
  }
}
