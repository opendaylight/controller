package org.opendaylight.controller.sal.connector.remoterpc;

import org.opendaylight.controller.sal.connector.remoterpc.api.RouteChangeListener;
import org.opendaylight.controller.sal.connector.remoterpc.api.RoutingTable;
import org.opendaylight.controller.sal.connector.remoterpc.api.RoutingTableException;
import org.opendaylight.controller.sal.connector.remoterpc.api.SystemException;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Mock implementation of routing table
 */
public class MockRoutingTable<K, V> implements RoutingTable {


  @Override
  public void addRoute(Object o, Object o2) throws RoutingTableException, SystemException {

  }

  @Override
  public void addGlobalRoute(Object o, Object o2) throws RoutingTableException, SystemException {

  }

  @Override
  public void removeRoute(Object o, Object o2) {

  }

  @Override
  public void removeGlobalRoute(Object o) throws RoutingTableException, SystemException {

  }

  @Override
  public Set getRoutes(Object o) {
    Set<String> routes = new HashSet<String>();
    routes.add("localhost:5554");
    return routes;
  }

  @Override
  public Set<Map.Entry> getAllRoutes() {
    return Collections.emptySet();
  }

  @Override
  public Object getARoute(Object o) {
    return null;
  }

  @Override
  public void registerRouteChangeListener(RouteChangeListener routeChangeListener) {

  }
}
