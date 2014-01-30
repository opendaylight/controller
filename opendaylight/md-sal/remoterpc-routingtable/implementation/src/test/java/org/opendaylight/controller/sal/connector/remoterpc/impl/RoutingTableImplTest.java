/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.sal.connector.remoterpc.impl;

import junit.framework.Assert;
import org.apache.felix.dm.Component;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.controller.clustering.services.IClusterGlobalServices;
import org.opendaylight.controller.clustering.services.IClusterServices;
import org.opendaylight.controller.sal.connector.api.RpcRouter;
import org.opendaylight.controller.sal.connector.remoterpc.api.RoutingTable;
import org.opendaylight.controller.sal.connector.remoterpc.api.RoutingTableException;
import org.opendaylight.controller.sal.connector.remoterpc.api.SystemException;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.InstanceIdentifier;

import java.net.URI;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.*;

import static org.mockito.Mockito.*;

public class RoutingTableImplTest {

  private final URI namespace = URI.create("http://cisco.com/example");
  private final QName QNAME = new QName(namespace, "global");

  private IClusterGlobalServices clusterService;
  private RoutingTableImpl<RpcRouter.RouteIdentifier<QName, QName, InstanceIdentifier>, String> routingTable;
  ConcurrentMap mockGlobalRpcCache;
  ConcurrentMap mockRpcCache;

  @Before
  public void setUp() throws Exception{
    clusterService = mock(IClusterGlobalServices.class);
    routingTable = new RoutingTableImpl<RpcRouter.RouteIdentifier<QName, QName, InstanceIdentifier>, String>();
    mockGlobalRpcCache = new ConcurrentHashMap<>();
    mockRpcCache = new ConcurrentHashMap<>();
    createRoutingTableCache();
  }

  @After
  public void tearDown(){
    reset(clusterService);
    mockGlobalRpcCache = null;
    mockRpcCache = null;
  }

  @Test
  public void addGlobalRoute_ValidArguments_ShouldAdd() throws Exception {

    Assert.assertNotNull(mockGlobalRpcCache);
    RpcRouter.RouteIdentifier<QName, QName, InstanceIdentifier> routeIdentifier = getRouteIdentifier();

    final String expectedRoute = "172.27.12.1:5000";
    routingTable.addGlobalRoute(routeIdentifier, expectedRoute);

    ConcurrentMap latestCache = routingTable.getGlobalRpcCache();
    Assert.assertEquals(mockGlobalRpcCache, latestCache);
    Assert.assertEquals(expectedRoute, latestCache.get(routeIdentifier));
  }

  @Test (expected = RoutingTable.DuplicateRouteException.class)
  public void addGlobalRoute_DuplicateRoute_ShouldThrow() throws Exception{

    Assert.assertNotNull(mockGlobalRpcCache);

    RpcRouter.RouteIdentifier<QName, QName, InstanceIdentifier> routeIdentifier = getRouteIdentifier();
    routingTable.addGlobalRoute(routeIdentifier, new String());
    routingTable.addGlobalRoute(routeIdentifier, new String());
  }

  @Test
  public void getGlobalRoute_ExistingRouteId_ShouldReturnRoute() throws Exception {

    Assert.assertNotNull(mockGlobalRpcCache);
    RpcRouter.RouteIdentifier<QName, QName, InstanceIdentifier> routeIdentifier = getRouteIdentifier();
    String expectedRoute = "172.27.12.1:5000";

    routingTable.addGlobalRoute(routeIdentifier, expectedRoute);

    String actualRoute = (String) routingTable.getGlobalRoute(routeIdentifier);
    Assert.assertEquals(expectedRoute, actualRoute);
  }

  @Test
  public void getGlobalRoute_NonExistentRouteId_ShouldReturnNull() throws Exception {

    Assert.assertNotNull(mockGlobalRpcCache);
    RpcRouter.RouteIdentifier<QName, QName, InstanceIdentifier> routeIdentifier = getRouteIdentifier();

    String actualRoute = (String) routingTable.getGlobalRoute(routeIdentifier);
    Assert.assertNull(actualRoute);
  }

  @Test
  public void removeGlobalRoute_ExistingRouteId_ShouldRemove() throws Exception {

    Assert.assertNotNull(mockGlobalRpcCache);
    RpcRouter.RouteIdentifier<QName, QName, InstanceIdentifier> routeIdentifier = getRouteIdentifier();

    ConcurrentMap cache = routingTable.getGlobalRpcCache();
    Assert.assertTrue(cache.size() == 0);
    routingTable.addGlobalRoute(routeIdentifier, "172.27.12.1:5000");
    Assert.assertTrue(cache.size() == 1);

    routingTable.removeGlobalRoute(routeIdentifier);
    Assert.assertTrue(cache.size() == 0);

  }

  @Test
  public void removeGlobalRoute_NonExistentRouteId_ShouldDoNothing() throws Exception {

    Assert.assertNotNull(mockGlobalRpcCache);
    RpcRouter.RouteIdentifier<QName, QName, InstanceIdentifier> routeIdentifier = getRouteIdentifier();

    ConcurrentMap cache = routingTable.getGlobalRpcCache();
    Assert.assertTrue(cache.size() == 0);

    routingTable.removeGlobalRoute(routeIdentifier);
    Assert.assertTrue(cache.size() == 0);

  }

  @Test
  public void addRoute_ForNewRouteId_ShouldAddRoute() throws Exception {
    Assert.assertTrue(mockRpcCache.size() == 0);

    RpcRouter.RouteIdentifier<QName, QName, InstanceIdentifier> routeId = getRouteIdentifier();

    routingTable.addRoute(routeId, new String());
    Assert.assertTrue(mockRpcCache.size() == 1);

    Set<String> routes = routingTable.getRoutes(routeId);
    Assert.assertEquals(1, routes.size());
  }

  @Test
  public void addRoute_ForExistingRouteId_ShouldAppendRoute() throws Exception {

    Assert.assertTrue(mockRpcCache.size() == 0);

    RpcRouter.RouteIdentifier<QName, QName, InstanceIdentifier> routeId = getRouteIdentifier();

    String route_1 = "10.0.0.1:5955";
    String route_2 = "10.0.0.2:5955";

    routingTable.addRoute(routeId, route_1);
    routingTable.addRoute(routeId, route_2);

    Assert.assertTrue(mockRpcCache.size() == 1);

    Set<String> routes = routingTable.getRoutes(routeId);
    Assert.assertEquals(2, routes.size());
    Assert.assertTrue(routes.contains(route_1));
    Assert.assertTrue(routes.contains(route_2));
  }

  @Test
  public void addRoute_UsingMultipleThreads_ShouldNotOverwrite(){
    ExecutorService threadPool = Executors.newCachedThreadPool();

    int numOfRoutesToAdd = 100;
    String routePrefix_1   = "10.0.0.1:555";
    RpcRouter.RouteIdentifier routeId = getRouteIdentifier();
    threadPool.submit(addRoutes(numOfRoutesToAdd, routePrefix_1, routeId));
    String routePrefix_2   = "10.0.0.1:556";
    threadPool.submit(addRoutes(numOfRoutesToAdd, routePrefix_2, routeId));

    // wait for all tasks to complete; timeout in 10 sec
    threadPool.shutdown();
    try {
      threadPool.awaitTermination(10, TimeUnit.SECONDS); //
    } catch (InterruptedException e) {
      e.printStackTrace();
    }

    Assert.assertEquals(2*numOfRoutesToAdd, routingTable.getRoutes(routeId).size());
  }

  @Test(expected = NullPointerException.class)
  public void addRoute_NullRouteId_shouldThrowNpe() throws Exception {

    routingTable.addRoute(null, new String());
  }

  @Test(expected = NullPointerException.class)
  public void addRoute_NullRoute_shouldThrowNpe() throws Exception{

    routingTable.addRoute(getRouteIdentifier(), null);
  }

  @Test (expected = UnsupportedOperationException.class)
  public void getRoutes_Call_ShouldReturnImmutableCopy() throws Exception{
    Assert.assertNotNull(routingTable);
    RpcRouter.RouteIdentifier routeId = getRouteIdentifier();
    routingTable.addRoute(routeId, new String());

    Set<String> routes = routingTable.getRoutes(routeId); //returns Immutable Set

    routes.add(new String()); //can not be modified; should throw
  }

  @Test
  public void getRoutes_With2RoutesFor1RouteId_ShouldReturnASetWithSize2() throws Exception{
    Assert.assertNotNull(routingTable);
    RpcRouter.RouteIdentifier routeId = getRouteIdentifier();
    routingTable.addRoute(routeId, "10.0.0.1:5555");
    routingTable.addRoute(routeId, "10.0.0.2:5555");

    Set<String> routes = routingTable.getRoutes(routeId); //returns Immutable Set

    Assert.assertEquals(2, routes.size());
  }

  @Test
  public void getLastAddedRoute_WhenMultipleRoutesExists_ShouldReturnLatestRoute()
    throws Exception {

    Assert.assertNotNull(routingTable);
    RpcRouter.RouteIdentifier routeId = getRouteIdentifier();
    String route_1 = "10.0.0.1:5555";
    String route_2 = "10.0.0.2:5555";
    routingTable.addRoute(routeId, route_1);
    routingTable.addRoute(routeId, route_2);

    Assert.assertEquals(route_2, routingTable.getLastAddedRoute(routeId));
  }

  @Test
  public void removeRoute_WhenMultipleRoutesExist_RemovesGivenRoute() throws Exception{
    Assert.assertNotNull(routingTable);
    RpcRouter.RouteIdentifier routeId = getRouteIdentifier();
    String route_1 = "10.0.0.1:5555";
    String route_2 = "10.0.0.2:5555";

    routingTable.addRoute(routeId, route_1);
    routingTable.addRoute(routeId, route_2);

    Assert.assertEquals(2, routingTable.getRoutes(routeId).size());

    routingTable.removeRoute(routeId, route_1);
    Assert.assertEquals(1, routingTable.getRoutes(routeId).size());

  }

  @Test
  public void removeRoute_WhenOnlyOneRouteExists_RemovesRouteId() throws Exception{
    Assert.assertNotNull(routingTable);
    RpcRouter.RouteIdentifier routeId = getRouteIdentifier();
    String route_1 = "10.0.0.1:5555";

    routingTable.addRoute(routeId, route_1);
    Assert.assertEquals(1, routingTable.getRoutes(routeId).size());

    routingTable.removeRoute(routeId, route_1);
    ConcurrentMap cache = routingTable.getRpcCache();
    Assert.assertFalse(cache.containsKey(routeId));

  }

  /*
   * Private helper methods
   */
  private void createRoutingTableCache() throws Exception {

    //here init
    Component c = mock(Component.class);

    when(clusterService.existCache(
        RoutingTableImpl.GLOBALRPC_CACHE)).thenReturn(false);

    when(clusterService.createCache(RoutingTableImpl.GLOBALRPC_CACHE,
        EnumSet.of(IClusterServices.cacheMode.TRANSACTIONAL))).
        thenReturn(mockGlobalRpcCache);

    when(clusterService.existCache(
        RoutingTableImpl.RPC_CACHE)).thenReturn(false);

    when(clusterService.createCache(RoutingTableImpl.RPC_CACHE,
        EnumSet.of(IClusterServices.cacheMode.TRANSACTIONAL))).
        thenReturn(mockRpcCache);

    doNothing().when(clusterService).tbegin();
    doNothing().when(clusterService).tcommit();

    routingTable.setClusterGlobalServices(this.clusterService);
    routingTable.init(c);

    Assert.assertEquals(mockGlobalRpcCache, routingTable.getGlobalRpcCache());
    Assert.assertEquals(mockRpcCache, routingTable.getRpcCache());
  }

  private RpcRouter.RouteIdentifier<QName, QName, InstanceIdentifier> getRouteIdentifier(){
    RpcRouter.RouteIdentifier<QName, QName, InstanceIdentifier> routeIdentifier = mock(RpcRouter.RouteIdentifier.class);
    InstanceIdentifier identifier = mock(InstanceIdentifier.class);
    when(routeIdentifier.getType()).thenReturn(QNAME);
    when(routeIdentifier.getRoute()).thenReturn(identifier);

    return routeIdentifier;
  }

  private Runnable addRoutes(final int numRoutes, final String routePrefix, final RpcRouter.RouteIdentifier routeId){
    return new Runnable() {
      @Override
      public void run() {
        for (int i=0;i<numRoutes;i++){
          String route = routePrefix + i;
          try {
            routingTable.addRoute(routeId, route);
          } catch (Exception e) {
            e.printStackTrace();
          }
        }
      }
    };
  }
}
