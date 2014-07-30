/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.remote.rpc.registry;

import junit.framework.Assert;
import org.junit.Test;
import org.opendaylight.controller.remote.rpc.RouteIdentifierImpl;
import org.opendaylight.controller.sal.connector.api.RpcRouter;
import org.opendaylight.yangtools.yang.common.QName;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashSet;
import java.util.Set;

public class RoutingTableTest {

  private RoutingTable<RpcRouter.RouteIdentifier<?, ?, ?>, String> routingTable =
      new RoutingTable<>();

  @Test
  public void addGlobalRouteNullRouteIdTest() {
    try {
      routingTable.addGlobalRoute(null, null);

      Assert.fail("Null pointer exception was not thrown.");
    } catch (Exception e) {
      Assert.assertEquals(NullPointerException.class.getName(), e.getClass().getName());
      Assert.assertEquals("addGlobalRoute: routeId cannot be null!", e.getMessage());
    }
  }

  @Test
  public void addGlobalRouteNullRouteTest() {
    try {
      QName type = new QName(new URI("actor1"), "actor1");
      RouteIdentifierImpl routeId = new RouteIdentifierImpl(null, type, null);
      routingTable.addGlobalRoute(routeId, null);

      Assert.fail("Null pointer exception was not thrown.");
    } catch (Exception e) {
      Assert.assertEquals(NullPointerException.class.getName(), e.getClass().getName());
      Assert.assertEquals("addGlobalRoute: route cannot be null!", e.getMessage());
    }
  }

  @Test
  public void getGlobalRouteNullTest() {
    try {
      routingTable.getGlobalRoute(null);

      Assert.fail("Null pointer exception was not thrown.");
    } catch (Exception e) {
      Assert.assertEquals(NullPointerException.class.getName(), e.getClass().getName());
      Assert.assertEquals("getGlobalRoute: routeId cannot be null!", e.getMessage());
    }
  }

  @Test
  public void getGlobalRouteTest() throws URISyntaxException {
    QName type = new QName(new URI("actor1"), "actor1");
    RouteIdentifierImpl routeId = new RouteIdentifierImpl(null, type, null);
    String route = "actor1";

    routingTable.addGlobalRoute(routeId, route);

    String returnedRoute = routingTable.getGlobalRoute(routeId);

    Assert.assertEquals(route, returnedRoute);

  }

  @Test
  public void removeGlobalRouteTest() throws URISyntaxException {
    QName type = new QName(new URI("actorRemove"), "actorRemove");
    RouteIdentifierImpl routeId = new RouteIdentifierImpl(null, type, null);
    String route = "actorRemove";

    routingTable.addGlobalRoute(routeId, route);

    String returnedRoute = routingTable.getGlobalRoute(routeId);

    Assert.assertEquals(route, returnedRoute);

    routingTable.removeGlobalRoute(routeId);

    String deletedRoute = routingTable.getGlobalRoute(routeId);

    Assert.assertNull(deletedRoute);
  }

  @Test
  public void addRoutedRpcNullRouteIdTest() {
    try {
      routingTable.addRoutedRpc(null, null);

      Assert.fail("Null pointer exception was not thrown.");
    } catch (Exception e) {
      Assert.assertEquals(NullPointerException.class.getName(), e.getClass().getName());
      Assert.assertEquals("addRoute: routeId cannot be null", e.getMessage());
    }
  }

  @Test
  public void addRoutedRpcNullRouteTest() {
    try {
      QName type = new QName(new URI("actor1"), "actor1");
      RouteIdentifierImpl routeId = new RouteIdentifierImpl(null, type, null);

      routingTable.addRoutedRpc(routeId, null);

      Assert.fail("Null pointer exception was not thrown.");
    } catch (Exception e) {
      Assert.assertEquals(NullPointerException.class.getName(), e.getClass().getName());
      Assert.assertEquals("addRoute: route cannot be null", e.getMessage());
    }
  }

  @Test
  public void getRoutedRpcNullTest() {
    try {
      routingTable.getRoutedRpc(null);

      Assert.fail("Null pointer exception was not thrown.");
    } catch (Exception e) {
      Assert.assertEquals(NullPointerException.class.getName(), e.getClass().getName());
      Assert.assertEquals("getRoutes: routeId cannot be null!", e.getMessage());
    }
  }

  @Test
  public void getRoutedRpcTest() throws URISyntaxException {
    QName type = new QName(new URI("actor1"), "actor1");
    RouteIdentifierImpl routeId = new RouteIdentifierImpl(null, type, null);
    String route = "actor1";

    routingTable.addRoutedRpc(routeId, route);

    Set<String> routes = routingTable.getRoutedRpc(routeId);

    Assert.assertEquals(1, routes.size());
    Assert.assertTrue(routes.contains(route));

  }

  @Test
  public void getLastRoutedRpcTest() throws URISyntaxException {
    QName type = new QName(new URI("first1"), "first1");
    RouteIdentifierImpl routeId = new RouteIdentifierImpl(null, type, null);
    String route = "first1";

    routingTable.addRoutedRpc(routeId, route);

    String route2 = "second1";
    routingTable.addRoutedRpc(routeId, route2);

    String latest = routingTable.getLastAddedRoutedRpc(routeId);
    Assert.assertEquals(route2, latest);

  }

  @Test
  public void removeRoutedRpcTest() throws URISyntaxException {
    QName type = new QName(new URI("remove"), "remove");
    RouteIdentifierImpl routeId = new RouteIdentifierImpl(null, type, null);
    String route = "remove";
    routingTable.addRoutedRpc(routeId, route);

    String latest = routingTable.getLastAddedRoutedRpc(routeId);
    Assert.assertEquals(route, latest);

    routingTable.removeRoute(routeId, route);
    String removed = routingTable.getLastAddedRoutedRpc(routeId);
    Assert.assertNull(removed);
  }

  @Test
  public void removeRoutedRpcsTest() throws URISyntaxException {
    QName type = new QName(new URI("remove1"), "remove1");
    RouteIdentifierImpl routeId = new RouteIdentifierImpl(null, type, null);

    QName type2 = new QName(new URI("remove2"), "remove2");
    RouteIdentifierImpl routeId2 = new RouteIdentifierImpl(null, type2, null);

    Set<RpcRouter.RouteIdentifier<?, ?, ?>> routeIds = new HashSet<>();
    routeIds.add(routeId);
    routeIds.add(routeId2);
    String route = "remove1";

    routingTable.addRoutedRpcs(routeIds, route);
    String latest1 = routingTable.getLastAddedRoutedRpc(routeId);
    Assert.assertEquals(route, latest1);

    String latest2 = routingTable.getLastAddedRoutedRpc(routeId2);
    Assert.assertEquals(route, latest2);

    routingTable.removeRoutes(routeIds, route);
    String removed1 = routingTable.getLastAddedRoutedRpc(routeId);
    Assert.assertNull(removed1);

    String removed2 = routingTable.getLastAddedRoutedRpc(routeId2);
    Assert.assertNull(removed2);
  }

}
