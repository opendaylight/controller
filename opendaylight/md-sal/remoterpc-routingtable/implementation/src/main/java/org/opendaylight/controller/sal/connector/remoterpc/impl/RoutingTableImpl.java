/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.sal.connector.remoterpc.impl;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSet;
import org.apache.felix.dm.Component;
import org.opendaylight.controller.clustering.services.*;
import org.opendaylight.controller.sal.connector.remoterpc.api.RoutingTable;
import org.opendaylight.controller.sal.connector.remoterpc.api.RoutingTableException;
import org.opendaylight.controller.sal.connector.remoterpc.api.SystemException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.transaction.HeuristicMixedException;
import javax.transaction.HeuristicRollbackException;
import javax.transaction.NotSupportedException;
import javax.transaction.RollbackException;
import java.util.*;
import java.util.concurrent.ConcurrentMap;

public class RoutingTableImpl<I, R> implements RoutingTable<I, R>, ICacheUpdateAware<I, R> {

  private Logger log = LoggerFactory.getLogger(RoutingTableImpl.class);

  private IClusterGlobalServices clusterGlobalServices = null;

  private ConcurrentMap<I,R> globalRpcCache = null;
  private ConcurrentMap<I, LinkedHashSet<R>> rpcCache = null;  //need routes to ordered by insert-order

  public static final String GLOBALRPC_CACHE = "remoterpc_routingtable.globalrpc_cache";
  public static final String RPC_CACHE = "remoterpc_routingtable.rpc_cache";

  public RoutingTableImpl() {
  }

  @Override
  public R getGlobalRoute(I routeId) throws RoutingTableException, SystemException {
    Preconditions.checkNotNull(routeId, "getGlobalRoute: routeId cannot be null!");
    return globalRpcCache.get(routeId);
  }

  @Override
  public void addGlobalRoute(I routeId, R route) throws RoutingTableException, SystemException {
    Preconditions.checkNotNull(routeId, "addGlobalRoute: routeId cannot be null!");
    Preconditions.checkNotNull(route, "addGlobalRoute: route cannot be null!");
    try {

      log.debug("addGlobalRoute: adding  a new route with id[{}] and value [{}]", routeId, route);
      clusterGlobalServices.tbegin();
      if (globalRpcCache.putIfAbsent(routeId, route) != null) {
        throw new DuplicateRouteException(" There is already existing route " + routeId);
      }
      clusterGlobalServices.tcommit();

    } catch (NotSupportedException | HeuristicRollbackException | RollbackException | HeuristicMixedException e) {
      throw new RoutingTableException("Transaction error - while trying to create route id="
          + routeId + "with route" + route, e);
    } catch (javax.transaction.SystemException e) {
      throw new SystemException("System error occurred - while trying to create with value", e);
    }

  }

  @Override
  public void removeGlobalRoute(I routeId) throws RoutingTableException, SystemException {
    Preconditions.checkNotNull(routeId, "removeGlobalRoute: routeId cannot be null!");
    try {
      log.debug("removeGlobalRoute: removing  a new route with id [{}]", routeId);

      clusterGlobalServices.tbegin();
      globalRpcCache.remove(routeId);
      clusterGlobalServices.tcommit();

    } catch (NotSupportedException | HeuristicRollbackException | RollbackException | HeuristicMixedException e) {
      throw new RoutingTableException("Transaction error - while trying to remove route id="
          + routeId, e);
    } catch (javax.transaction.SystemException e) {
      throw new SystemException("System error occurred - while trying to remove with value", e);
    }
  }


  @Override
  public Set<R> getRoutes(I routeId) {
    Preconditions.checkNotNull(routeId, "getRoutes: routeId cannot be null!");
    Set<R> routes = rpcCache.get(routeId);

    if (routes == null) return Collections.emptySet();

    return ImmutableSet.copyOf(routes);
  }



  public R getLastAddedRoute(I routeId) {

    Set<R> routes = getRoutes(routeId);

    if (routes.isEmpty()) return null;

    R route = null;
    Iterator<R> iter = routes.iterator();
    while (iter.hasNext())
      route = iter.next();

    return route;
  }

  @Override
  public void addRoute(I routeId, R route)  throws RoutingTableException, SystemException {
    Preconditions.checkNotNull(routeId, "addRoute: routeId cannot be null");
    Preconditions.checkNotNull(route, "addRoute: route cannot be null");

    try{
      clusterGlobalServices.tbegin();
      log.debug("addRoute: adding a route with k/v [{}/{}]", routeId, route);
      threadSafeAdd(routeId, route);
      clusterGlobalServices.tcommit();

    } catch (NotSupportedException | HeuristicRollbackException | RollbackException | HeuristicMixedException e) {
      throw new RoutingTableException("Transaction error - while trying to remove route id="
          + routeId, e);
    } catch (javax.transaction.SystemException e) {
      throw new SystemException("System error occurred - while trying to remove with value", e);
    }
  }

  @Override
  public void addRoutes(Set<I> routeIds, R route) throws RoutingTableException, SystemException {
    Preconditions.checkNotNull(routeIds, "addRoutes: routeIds must not be null");
    for (I routeId : routeIds){
      addRoute(routeId, route);
    }
  }

  @Override
  public void removeRoute(I routeId, R route) throws RoutingTableException, SystemException {
    Preconditions.checkNotNull(routeId, "removeRoute: routeId cannot be null!");
    Preconditions.checkNotNull(route, "removeRoute: route cannot be null!");

    LinkedHashSet<R> routes = rpcCache.get(routeId);
    if (routes == null) return;

    try {
      log.debug("removeRoute: removing  a new route with k/v [{}/{}]", routeId, route);

      clusterGlobalServices.tbegin();
      threadSafeRemove(routeId, route);
      clusterGlobalServices.tcommit();

    } catch (NotSupportedException | HeuristicRollbackException | RollbackException | HeuristicMixedException e) {
      throw new RoutingTableException("Transaction error - while trying to remove route id="
          + routeId, e);
    } catch (javax.transaction.SystemException e) {
      throw new SystemException("System error occurred - while trying to remove with value", e);
    }
  }

  @Override
  public void removeRoutes(Set<I> routeIds, R route) throws RoutingTableException, SystemException {
    Preconditions.checkNotNull(routeIds, "removeRoutes: routeIds must not be null");
    for (I routeId : routeIds){
      removeRoute(routeId, route);
    }
  }

  /**
   * This method guarantees that no 2 thread over write each other's changes.
   * Just so that we dont end up in infinite loop, it tries for 100 times then throw
   */
  private void threadSafeAdd(I routeId, R route) {

    for (int i=0;i<100;i++){

      LinkedHashSet<R> updatedRoutes = new LinkedHashSet<>();
      updatedRoutes.add(route);
      LinkedHashSet<R> oldRoutes = rpcCache.putIfAbsent(routeId, updatedRoutes);
      if (oldRoutes == null) return;

      updatedRoutes = new LinkedHashSet<>(oldRoutes);
      updatedRoutes.add(route);

      if (rpcCache.replace(routeId, oldRoutes, updatedRoutes)) return;
    }
    //the method did not already return means it failed to add route in 10 attempts
    throw new IllegalStateException("Failed to add route [" + routeId + "]");
  }

  /**
   * This method guarantees that no 2 thread over write each other's changes.
   * Just so that we dont end up in infinite loop, it tries for 10 times then throw
   */
  private void threadSafeRemove(I routeId, R route) {
    LinkedHashSet<R> updatedRoutes = null;
    for (int i=0;i<10;i++){
      LinkedHashSet<R> oldRoutes = rpcCache.get(routeId);

      // if route to be deleted is the only entry in the set then remove routeId from the cache
      if ((oldRoutes.size() == 1) && oldRoutes.contains(route)){
        rpcCache.remove(routeId);
        return;
      }

      // if there are multiple routes for this routeId, remove the route to be deleted only from the set.
      updatedRoutes = new LinkedHashSet<>(oldRoutes);
      updatedRoutes.remove(route);
      if (rpcCache.replace(routeId, oldRoutes, updatedRoutes)) return;

    }
    //the method did not already return means it failed to remove route in 10 attempts
    throw new IllegalStateException("Failed to remove route [" + routeId + "]");
  }


//    /**
//     * @deprecated doesn't do anything will be removed once listeners used
//     *             whiteboard pattern Registers listener for sending any change
//     *             notification
//     * @param listener
//     */
//    @Override
//    public void registerRouteChangeListener(RouteChangeListener listener) {
//
//    }

//    public void setRouteChangeListener(RouteChangeListener rcl) {
//        if(rcl != null){
//            routeChangeListeners.add(rcl);
//        }else{
//            log.warn("setRouteChangeListener called with null listener");
//        }
//    }
//
//    public void unSetRouteChangeListener(RouteChangeListener rcl) {
//        if(rcl != null){
//         routeChangeListeners.remove(rcl);
//        }else{
//            log.warn("unSetRouteChangeListener called with null listener");
//        }
//    }

  /**
   * Returning the set of route change listeners for Unit testing Note: the
   * package scope is default
   *
   * @return List of registered RouteChangeListener<I,R> listeners
   */
//    Set<RouteChangeListener> getRegisteredRouteChangeListeners() {
//        return routeChangeListeners;
//    }
  public void setClusterGlobalServices(IClusterGlobalServices clusterGlobalServices) {
    this.clusterGlobalServices = clusterGlobalServices;
  }

  public void unsetClusterGlobalServices(IClusterGlobalServices clusterGlobalServices) {
    if ((clusterGlobalServices != null) && (this.clusterGlobalServices.equals(clusterGlobalServices))) {
      this.clusterGlobalServices = null;
    }
  }

  /**
   * Finds OR Creates clustered cache for Global RPCs
   *
   * @throws CacheExistException       -- cluster global services exception when cache exist
   * @throws CacheConfigException      -- cluster global services exception during cache config
   * @throws CacheListenerAddException -- cluster global services exception during adding of listener
   */

  void findOrCreateGlobalRpcCache() throws CacheExistException, CacheConfigException,
      CacheListenerAddException {
    // TBD: HOW DO WE DECIDE ON PROPERTIES OF THE CACHE i.e. what duration it
    // should be caching?

    // let us check here if the cache already exists -- if so don't create
    if (!clusterGlobalServices.existCache(GLOBALRPC_CACHE)) {

      globalRpcCache = (ConcurrentMap<I,R>) clusterGlobalServices.createCache(GLOBALRPC_CACHE,
          EnumSet.of(IClusterServices.cacheMode.TRANSACTIONAL));
      log.debug("Cache created [{}] ", GLOBALRPC_CACHE);

    } else {
      globalRpcCache = (ConcurrentMap<I,R>) clusterGlobalServices.getCache(GLOBALRPC_CACHE);
      log.debug("Cache exists [{}] ", GLOBALRPC_CACHE);
    }
  }

  /**
   * Finds OR Creates clustered cache for Routed RPCs
   *
   * @throws CacheExistException       -- cluster global services exception when cache exist
   * @throws CacheConfigException      -- cluster global services exception during cache config
   * @throws CacheListenerAddException -- cluster global services exception during adding of listener
   */

  void findOrCreateRpcCache() throws CacheExistException, CacheConfigException,
      CacheListenerAddException {
    // TBD: HOW DO WE DECIDE ON PROPERTIES OF THE CACHE i.e. what duration it
    // should be caching?

    if (clusterGlobalServices.existCache(RPC_CACHE)){
      rpcCache = (ConcurrentMap<I,LinkedHashSet<R>>) clusterGlobalServices.getCache(RPC_CACHE);
      log.debug("Cache exists [{}] ", RPC_CACHE);
      return;
    }

    //cache doesnt exist, create one
    rpcCache = (ConcurrentMap<I,LinkedHashSet<R>>) clusterGlobalServices.createCache(RPC_CACHE,
          EnumSet.of(IClusterServices.cacheMode.TRANSACTIONAL));
    log.debug("Cache created [{}] ", RPC_CACHE);
  }


  /**
   * Function called by the dependency manager when all the required
   * dependencies are satisfied
   */
  void init(Component c) {
    try {

      findOrCreateGlobalRpcCache();
      findOrCreateRpcCache();

    } catch (CacheExistException|CacheConfigException|CacheListenerAddException e) {
      throw new IllegalStateException("could not construct routing table cache");
    }
  }

  /**
   * Useful for unit testing <note>It has package
   * scope</note>
   */
  ConcurrentMap getGlobalRpcCache() {
    return this.globalRpcCache;
  }

  /**
   * Useful for unit testing <note>It has package
   * scope</note>
   */
  ConcurrentMap getRpcCache() {
    return this.rpcCache;
  }

  /**
   * This is used from integration test NP rest API to check out the result of the
   * cache population
   * <Note> For testing purpose only-- use it wisely</Note>
   *
   * @return
   */
  public String dumpGlobalRpcCache() {
    Set<Map.Entry<I, R>> cacheEntrySet = this.globalRpcCache.entrySet();
    StringBuilder sb = new StringBuilder();
    for (Map.Entry<I, R> entry : cacheEntrySet) {
      sb.append("Key:").append(entry.getKey()).append("---->Value:")
          .append((entry.getValue() != null) ? entry.getValue() : "null")
          .append("\n");
    }
    return sb.toString();
  }

  public String dumpRpcCache() {
    Set<Map.Entry<I, LinkedHashSet<R>>> cacheEntrySet = this.rpcCache.entrySet();
    StringBuilder sb = new StringBuilder();
    for (Map.Entry<I, LinkedHashSet<R>> entry : cacheEntrySet) {
      sb.append("Key:").append(entry.getKey()).append("---->Value:")
          .append((entry.getValue() != null) ? entry.getValue() : "null")
          .append("\n");
    }
    return sb.toString();
  }
  /**
   * Invoked when a new entry is available in the cache, the key is only
   * provided, the value will come as an entryUpdate invocation
   *
   * @param key         Key for the entry just created
   * @param cacheName   name of the cache for which update has been received
   * @param originLocal true if the event is generated from this node
   */
  @Override
  public void entryCreated(I key, String cacheName, boolean originLocal) {
    // TBD: do we require this.
    if (log.isDebugEnabled()) {
      log.debug("RoutingTableUpdates: entryCreated  routeId = " + key + " cacheName=" + cacheName);
    }
  }

  /**
   * Called anytime a given entry is updated
   *
   * @param key         Key for the entry modified
   * @param new_value   the new value the key will have
   * @param cacheName   name of the cache for which update has been received
   * @param originLocal true if the event is generated from this node
   */
  @Override
  public void entryUpdated(I key, R new_value, String cacheName, boolean originLocal) {
    if (log.isDebugEnabled()) {
      log.debug("RoutingTableUpdates: entryUpdated  routeId = " + key + ",value = " + new_value
          + " ,cacheName=" + cacheName + " originLocal=" + originLocal);
    }
//        if (!originLocal) {
//            for (RouteChangeListener rcl : routeChangeListeners) {
//                rcl.onRouteUpdated(key, new_value);
//            }
//        }
  }

  /**
   * Called anytime a given key is removed from the ConcurrentHashMap we are
   * listening to.
   *
   * @param key         Key of the entry removed
   * @param cacheName   name of the cache for which update has been received
   * @param originLocal true if the event is generated from this node
   */
  @Override
  public void entryDeleted(I key, String cacheName, boolean originLocal) {
    if (log.isDebugEnabled()) {
      log.debug("RoutingTableUpdates: entryUpdated  routeId = " + key + " local = " + originLocal
          + " cacheName=" + cacheName + " originLocal=" + originLocal);
    }
//        if (!originLocal) {
//            for (RouteChangeListener rcl : routeChangeListeners) {
//                rcl.onRouteDeleted(key);
//            }
//        }
  }
}