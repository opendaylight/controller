/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.connector.remoterpc.api;

import java.util.Set;

public interface RoutingTable<I,R> {



  /**
   * Adds a network address for the route. If address for route
   * exists, appends the address to the list
   *
   * @param routeId route identifier
   * @param route network address
   * @throws RoutingTableException for any logical exception
   * @throws SystemException
   */
  public void addRoute(I routeId, R route) throws  RoutingTableException,SystemException;

    /**
   * Adds a network address for the route. If the route already exists,
   * it throws <code>DuplicateRouteException</code>.
   * This method would be used when registering a global service.
   *
   *
   * @param routeId route identifier
   * @param route network address
   * @throws DuplicateRouteException
   * @throws RoutingTableException
   */
  public void addGlobalRoute(I routeId, R route) throws  RoutingTableException, SystemException;




  /**
   * Removes the network address for the route from routing table. If only
   * one network address existed, remove the route as well.
   * @param routeId
   * @param route
   */
  public void removeRoute(I routeId, R route);


    /**
     * Remove the route.
     * This method would be used when registering a global service.
     * @param routeId
     * @throws RoutingTableException
     * @throws SystemException
     */
    public void removeGlobalRoute(I routeId) throws RoutingTableException, SystemException;

  /**
   * Returns a set of network addresses associated with this route
   * @param routeId
   * @return
   */
  public Set<R> getRoutes(I routeId);

  /**
   * Returns only one address from the list of network addresses
   * associated with the route. The algorithm to determine that
   * one address is upto the implementer
   * @param routeId
   * @return
   */
  public R getARoute(I routeId);

    /**
     *
     * This will be removed after listeners
     * have made change on their end to use whiteboard pattern
     * @deprecated
     */

  public void registerRouteChangeListener(RouteChangeListener listener);

  public class DuplicateRouteException extends RoutingTableException {
      public DuplicateRouteException(String message) {
          super(message);
      }

  }

}
