package org.opendaylight.controller.remote.rpc.registry;

import org.opendaylight.controller.sal.connector.api.RpcRouter;

import java.util.HashMap;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * Created by harmansingh on 7/21/14.
 */
public class RouteTable {

  /**
   * Each node will have its own map
   */
  private Map<GossipElement, Map<RpcRouter.RouteIdentifier<?, ?, ?>, RoutePath>> rpcMap = new HashMap<>();

  private Map<RpcRouter.RouteIdentifier<?, ?, ?>, SortedSet<GossipElement>> rpcReverseLookup = new HashMap<>();

  private long localNodeVersion;

  /**
   * Get the map of current node, Add route id to that map and increase the version
   * Also, add it in reverse lookup map to get entries faster
   * @param node
   * @param routeId
   * @param routePath
   */
  public void addRpc(String node, RpcRouter.RouteIdentifier<?, ?, ?> routeId, RoutePath routePath) {
    GossipElement existingKey = new GossipElement(node, localNodeVersion);
    Map<RpcRouter.RouteIdentifier<?, ?, ?>, RoutePath> map = rpcMap.get(existingKey);
    if(map == null) {
      map = new HashMap<>();
    }
    map.put(routeId, routePath);
    rpcMap.remove(existingKey);
    localNodeVersion = localNodeVersion+1;
    GossipElement newKey = new GossipElement(node, localNodeVersion);
    rpcMap.put(newKey, map);

    SortedSet<GossipElement> elements = rpcReverseLookup.get(routeId);
    if(elements == null) {
      elements = new TreeSet<>();
    } else if(elements.contains(existingKey)) {
      elements.remove(existingKey);
    }
    elements.add(newKey);
    rpcReverseLookup.put(routeId, elements);
  }

  public void removeRpc(String node, RpcRouter.RouteIdentifier<?, ?, ?> routeId) {
    GossipElement existingKey = new GossipElement(node, localNodeVersion);
    Map<RpcRouter.RouteIdentifier<?, ?, ?>, RoutePath> map = rpcMap.get(existingKey);
    if(map != null) {
      map.remove(routeId);
      rpcMap.remove(existingKey);
      localNodeVersion = localNodeVersion+1;
      GossipElement newKey = new GossipElement(node, localNodeVersion);
      rpcMap.put(newKey, map);
      SortedSet<GossipElement> elements = rpcReverseLookup.get(routeId);
      elements.remove(existingKey);
      if(elements.isEmpty()) {
        rpcReverseLookup.remove(routeId);
      } else {
        rpcReverseLookup.put(routeId, elements);
      }
    }
  }

  public String getRoutePath(RpcRouter.RouteIdentifier<?, ?, ?> routeId) {
    String routePath = null;
    SortedSet<GossipElement> elements = rpcReverseLookup.get(routeId);
    if(elements != null && !elements.isEmpty()) {
      routePath = elements.last().getNode();
    }

    return routePath;
  }

}
