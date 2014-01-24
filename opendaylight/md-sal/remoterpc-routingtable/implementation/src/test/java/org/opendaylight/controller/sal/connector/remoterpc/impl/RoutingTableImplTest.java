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
import org.opendaylight.controller.sal.connector.remoterpc.api.RouteChangeListener;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.InstanceIdentifier;

import java.net.URI;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

/**
 * @author: syedbahm
 */
public class RoutingTableImplTest {

  private final URI namespace = URI.create("http://cisco.com/example");
  private final QName QNAME = new QName(namespace, "global");

  private IClusterGlobalServices ics;
  private RoutingTableImpl rti;
  ConcurrentMap mockGlobalRpcCache;
  ConcurrentMap mockRpcCache;

  @Before
  public void setUp() throws Exception{
    ics = mock(IClusterGlobalServices.class);
    rti = new RoutingTableImpl();
    mockGlobalRpcCache = mock(ConcurrentMap.class);
    mockRpcCache = mock(ConcurrentMap.class);
    createRoutingTableCache();
  }

  @After
  public void tearDown(){
    reset(ics);
    reset(mockGlobalRpcCache);
    reset(mockRpcCache);
  }

  @Test
  public void testAddGlobalRoute() throws Exception {
    //ConcurrentMap concurrentMap = createRoutingTableCache();

    Assert.assertNotNull(mockGlobalRpcCache);
    RpcRouter.RouteIdentifier<QName, QName, InstanceIdentifier> routeIdentifier = mock(RpcRouter.RouteIdentifier.class);
    InstanceIdentifier identifier = mock(InstanceIdentifier.class);
    when(routeIdentifier.getType()).thenReturn(QNAME);
    when(routeIdentifier.getRoute()).thenReturn(identifier);

    rti.addGlobalRoute(routeIdentifier, "172.27.12.1:5000");

    Set<String> globalService = new HashSet<String>();
    globalService.add("172.27.12.1:5000");

    when(mockGlobalRpcCache.get(routeIdentifier)).thenReturn(globalService);
    ConcurrentMap latestCache = rti.getGlobalRpcCache();

    Assert.assertEquals(mockGlobalRpcCache, latestCache);

    Set<String> servicesGlobal = (Set<String>) latestCache.get(routeIdentifier);
    Assert.assertEquals(servicesGlobal.size(), 1);

    Assert.assertEquals(servicesGlobal.iterator().next(), "172.27.12.1:5000");

  }

  @Test
  public void testGetGlobalRoute() throws Exception {
    //ConcurrentMap concurrentMap = createRoutingTableCache();

    Assert.assertNotNull(mockGlobalRpcCache);
    RpcRouter.RouteIdentifier<QName, QName, InstanceIdentifier> routeIdentifier = mock(RpcRouter.RouteIdentifier.class);
    InstanceIdentifier identifier = mock(InstanceIdentifier.class);
    when(routeIdentifier.getContext()).thenReturn(QNAME);
    when(routeIdentifier.getRoute()).thenReturn(identifier);

    rti.addGlobalRoute(routeIdentifier, "172.27.12.1:5000");

    String globalService = "172.27.12.1:5000";

    when(mockGlobalRpcCache.get(routeIdentifier)).thenReturn(globalService);
    ConcurrentMap latestCache = rti.getGlobalRpcCache();

    Assert.assertEquals(mockGlobalRpcCache, latestCache);

    String servicesGlobal = (String) rti.getGlobalRoute(routeIdentifier);
    Assert.assertEquals(servicesGlobal, "172.27.12.1:5000");
  }

  //    @Test
//    public void testRegisterRouteChangeListener() throws Exception {
//        Assert.assertEquals(rti.getRegisteredRouteChangeListeners().size(),0);
//        rti.registerRouteChangeListener(new RouteChangeListenerImpl());
//
//        Assert.assertEquals(rti.getRegisteredRouteChangeListeners().size(),0); //old should not work
//        //what about the new approach - using whiteboard pattern
//        rti.setRouteChangeListener(new RouteChangeListenerImpl());
//
//        Assert.assertEquals(rti.getRegisteredRouteChangeListeners().size(),1); //should not work
//
//
//    }
  @Test
  public void testRemoveGlobalRoute() throws Exception {

    //ConcurrentMap concurrentMap = createRoutingTableCache();

    Assert.assertNotNull(mockGlobalRpcCache);
    RpcRouter.RouteIdentifier<QName, QName, InstanceIdentifier> routeIdentifier = mock(RpcRouter.RouteIdentifier.class);
    InstanceIdentifier identifier = mock(InstanceIdentifier.class);
    when(routeIdentifier.getContext()).thenReturn(QNAME);
    when(routeIdentifier.getRoute()).thenReturn(identifier);

    rti.addGlobalRoute(routeIdentifier, "172.27.12.1:5000");

    String globalService = "172.27.12.1:5000";

    when(mockGlobalRpcCache.get(routeIdentifier)).thenReturn(globalService);
    ConcurrentMap latestCache = rti.getGlobalRpcCache();

    Assert.assertEquals(mockGlobalRpcCache, latestCache);

    String servicesGlobal = (String)rti.getGlobalRoute(routeIdentifier);

    Assert.assertEquals(servicesGlobal, "172.27.12.1:5000");

    rti.removeGlobalRoute(routeIdentifier);

    Assert.assertNotNull(rti.getGlobalRoute(routeIdentifier));


  }

  private void createRoutingTableCache() throws Exception {

    //here init
    Component c = mock(Component.class);

    when(ics.existCache(
        RoutingTableImpl.GLOBALRPC_CACHE)).thenReturn(false);

    when(ics.createCache(RoutingTableImpl.GLOBALRPC_CACHE,
        EnumSet.of(IClusterServices.cacheMode.TRANSACTIONAL))).
        thenReturn(mockGlobalRpcCache);

    when(ics.existCache(
        RoutingTableImpl.RPC_CACHE)).thenReturn(false);

    when(ics.createCache(RoutingTableImpl.RPC_CACHE,
        EnumSet.of(IClusterServices.cacheMode.TRANSACTIONAL))).
        thenReturn(mockRpcCache);

    rti.setClusterGlobalServices(this.ics);
    rti.init(c);

    Assert.assertEquals(mockGlobalRpcCache, rti.getGlobalRpcCache());
    Assert.assertEquals(mockRpcCache, rti.getRpcCache());
  }


  @Test
  public void testCreateRoutingTableCacheReturnExistingCache() throws Exception {
    //ConcurrentMap concurrentMap = createRoutingTableCache();

    //OK here we should try creating again the cache but this time it should return the existing one
    when(ics.existCache(
        RoutingTableImpl.GLOBALRPC_CACHE)).thenReturn(true);

    when(ics.getCache(
        RoutingTableImpl.GLOBALRPC_CACHE)).thenReturn(mockGlobalRpcCache);


    //here init
    Component c = mock(Component.class);

    rti.init(c);

    Assert.assertEquals(mockGlobalRpcCache, rti.getGlobalRpcCache());


  }

  private class RouteChangeListenerImpl<I, R> implements RouteChangeListener<I, R> {

    @Override
    public void onRouteUpdated(I key, R new_value) {
      //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void onRouteDeleted(I key) {
      //To change body of implemented methods use File | Settings | File Templates.
    }
  }

}
