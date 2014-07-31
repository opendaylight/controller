/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.remote.rpc.registry;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class RoutingTableOld<I, R> {

  private final Logger LOG = LoggerFactory.getLogger(RoutingTableOld.class);

  private ConcurrentMap<I,R> globalRpcMap = new ConcurrentHashMap<>();
  private ConcurrentMap<I, LinkedHashSet<R>> routedRpcMap = new ConcurrentHashMap<>();

  public ConcurrentMap<I, R> getGlobalRpcMap() {
    return globalRpcMap;
  }

  public ConcurrentMap<I, LinkedHashSet<R>> getRoutedRpcMap() {
    return routedRpcMap;
  }

  public R getGlobalRoute(final I routeId) {
    Preconditions.checkNotNull(routeId, "getGlobalRoute: routeId cannot be null!");
    return globalRpcMap.get(routeId);
  }

  public void addGlobalRoute(final I routeId, final R route) {
    Preconditions.checkNotNull(routeId, "addGlobalRoute: routeId cannot be null!");
    Preconditions.checkNotNull(route, "addGlobalRoute: route cannot be null!");
    LOG.debug("addGlobalRoute: adding  a new route with id[{}] and value [{}]", routeId, route);
    if(globalRpcMap.putIfAbsent(routeId, route) != null) {
      LOG.debug("A route already exist for route id [{}] ", routeId);
    }
  }

  public void removeGlobalRoute(final I routeId) {
    Preconditions.checkNotNull(routeId, "removeGlobalRoute: routeId cannot be null!");
    LOG.debug("removeGlobalRoute: removing  a new route with id [{}]", routeId);
    globalRpcMap.remove(routeId);
  }

  public Set<R> getRoutedRpc(final I routeId) {
    Preconditions.checkNotNull(routeId, "getRoutes: routeId cannot be null!");
    Set<R> routes = routedRpcMap.get(routeId);

    if (routes == null) {
      return Collections.emptySet();
    }

    return ImmutableSet.copyOf(routes);
  }

  public R getLastAddedRoutedRpc(final I routeId) {

    Set<R> routes = getRoutedRpc(routeId);

    if (routes.isEmpty()) {
      return null;
    }

    R route = null;
    Iterator<R> iter = routes.iterator();
    while (iter.hasNext()) {
      route = iter.next();
    }

    return route;
  }

  public void addRoutedRpc(final I routeId, final R route)   {
    Preconditions.checkNotNull(routeId, "addRoute: routeId cannot be null");
    Preconditions.checkNotNull(route, "addRoute: route cannot be null");
    LOG.debug("addRoute: adding a route with k/v [{}/{}]", routeId, route);
    threadSafeAdd(routeId, route);
  }

  public void addRoutedRpcs(final Set<I> routeIds, final R route) {
    Preconditions.checkNotNull(routeIds, "addRoutes: routeIds must not be null");
    for (I routeId : routeIds){
      addRoutedRpc(routeId, route);
    }
  }

  public void removeRoute(final I routeId, final R route) {
    Preconditions.checkNotNull(routeId, "removeRoute: routeId cannot be null!");
    Preconditions.checkNotNull(route, "removeRoute: route cannot be null!");

    LinkedHashSet<R> routes = routedRpcMap.get(routeId);
    if (routes == null) {
      return;
    }
    LOG.debug("removeRoute: removing  a new route with k/v [{}/{}]", routeId, route);
    threadSafeRemove(routeId, route);
  }

  public void removeRoutes(final Set<I> routeIds, final R route) {
    Preconditions.checkNotNull(routeIds, "removeRoutes: routeIds must not be null");
    for (I routeId : routeIds){
      removeRoute(routeId, route);
    }
  }

  /**
   * This method guarantees that no 2 thread over write each other's changes.
   * Just so that we dont end up in infinite loop, it tries for 100 times then throw
   */
  private void threadSafeAdd(final I routeId, final R route) {

    for (int i=0;i<100;i++){

      LinkedHashSet<R> updatedRoutes = new LinkedHashSet<>();
      updatedRoutes.add(route);
      LinkedHashSet<R> oldRoutes = routedRpcMap.putIfAbsent(routeId, updatedRoutes);
      if (oldRoutes == null) {
        return;
      }

      updatedRoutes = new LinkedHashSet<>(oldRoutes);
      updatedRoutes.add(route);

      if (routedRpcMap.replace(routeId, oldRoutes, updatedRoutes)) {
        return;
      }
    }
    //the method did not already return means it failed to add route in 100 attempts
    throw new IllegalStateException("Failed to add route [" + routeId + "]");
  }

  /**
   * This method guarantees that no 2 thread over write each other's changes.
   * Just so that we dont end up in infinite loop, it tries for 100 times then throw
   */
  private void threadSafeRemove(final I routeId, final R route) {
    LinkedHashSet<R> updatedRoutes = null;
    for (int i=0;i<100;i++){
      LinkedHashSet<R> oldRoutes = routedRpcMap.get(routeId);

      // if route to be deleted is the only entry in the set then remove routeId from the cache
      if ((oldRoutes.size() == 1) && oldRoutes.contains(route)){
        routedRpcMap.remove(routeId);
        return;
      }

      // if there are multiple routes for this routeId, remove the route to be deleted only from the set.
      updatedRoutes = new LinkedHashSet<>(oldRoutes);
      updatedRoutes.remove(route);
      if (routedRpcMap.replace(routeId, oldRoutes, updatedRoutes)) {
        return;
      }

    }
    //the method did not already return means it failed to remove route in 100 attempts
    throw new IllegalStateException("Failed to remove route [" + routeId + "]");
  }
}
