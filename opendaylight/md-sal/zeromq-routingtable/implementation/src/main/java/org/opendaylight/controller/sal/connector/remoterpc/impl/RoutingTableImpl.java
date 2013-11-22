/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.sal.connector.remoterpc.impl;

import com.google.common.base.Preconditions;
import org.apache.felix.dm.Component;
import org.opendaylight.controller.clustering.services.CacheConfigException;
import org.opendaylight.controller.clustering.services.CacheExistException;
import org.opendaylight.controller.clustering.services.CacheListenerAddException;
import org.opendaylight.controller.clustering.services.ICacheUpdateAware;
import org.opendaylight.controller.clustering.services.IClusterGlobalServices;
import org.opendaylight.controller.clustering.services.IClusterServices;
import org.opendaylight.controller.sal.connector.remoterpc.api.RouteChangeListener;
import org.opendaylight.controller.sal.connector.remoterpc.api.RoutingTable;
import org.opendaylight.controller.sal.connector.remoterpc.api.RoutingTableException;
import org.opendaylight.controller.sal.connector.remoterpc.api.SystemException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.transaction.HeuristicMixedException;
import javax.transaction.HeuristicRollbackException;
import javax.transaction.NotSupportedException;
import javax.transaction.RollbackException;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;

/**
 * @author: syedbahm
 */
public class RoutingTableImpl<I, R> implements RoutingTable<I, R>, ICacheUpdateAware<I, R> {
    public static final String ROUTING_TABLE_GLOBAL_CACHE = "routing_table_global_cache";

    private Logger log = LoggerFactory.getLogger(RoutingTableImpl.class);

    private IClusterGlobalServices clusterGlobalServices = null;
    private RoutingTableImpl routingTableInstance = null;
    private ConcurrentMap routingTableCache = null;
    private Set<RouteChangeListener> routeChangeListeners = Collections
            .synchronizedSet(new HashSet<RouteChangeListener>());

    public RoutingTableImpl() {
    }

    @Override
    public void addRoute(I routeId, R route) throws RoutingTableException {
        throw new UnsupportedOperationException(" Not implemented yet!");
    }

    @Override
    public void addGlobalRoute(I routeId, R route) throws RoutingTableException, SystemException {
        Preconditions.checkNotNull(routeId, "addGlobalRoute: routeId cannot be null!");
        Preconditions.checkNotNull(route, "addGlobalRoute: route cannot be null!");
        try {

            Set<R> existingRoute = null;
            // ok does the global route is already registered ?
            if ((existingRoute = getRoutes(routeId)) == null) {

                if (log.isDebugEnabled()) {
                    log.debug("addGlobalRoute: adding  a new route with id" + routeId + " and value = "
                            + route);
                }
                // lets start a transaction
                clusterGlobalServices.tbegin();
                Set<R> routes = new HashSet<R>();
                routes.add(route);
                routingTableCache.put(routeId, routes);
                clusterGlobalServices.tcommit();
            } else {
                throw new DuplicateRouteException(" There is already existing route " + existingRoute);
            }

        } catch (NotSupportedException e) {
            throw new RoutingTableException("Transaction error - while trying to create route id="
                    + routeId + "with route" + route, e);
        } catch (HeuristicRollbackException e) {
            throw new RoutingTableException("Transaction error - while trying to create route id="
                    + routeId + "with route" + route, e);
        } catch (RollbackException e) {
            throw new RoutingTableException("Transaction error - while trying to create route id="
                    + routeId + "with route" + route, e);
        } catch (HeuristicMixedException e) {
            throw new RoutingTableException("Transaction error - while trying to create route id="
                    + routeId + "with route" + route, e);
        } catch (javax.transaction.SystemException e) {
            throw new SystemException("System error occurred - while trying to create with value", e);
        }

    }

    @Override
    public void removeRoute(I routeId, R route) {
        throw new UnsupportedOperationException("Not implemented yet!");
    }

    @Override
    public void removeGlobalRoute(I routeId) throws RoutingTableException, SystemException {
        Preconditions.checkNotNull(routeId, "removeGlobalRoute: routeId cannot be null!");
        try {
            if (log.isDebugEnabled()) {
                log.debug("removeGlobalRoute: removing  a new route with id" + routeId);
            }
            // lets start a transaction
            clusterGlobalServices.tbegin();

            routingTableCache.remove(routeId);
            clusterGlobalServices.tcommit();

        } catch (NotSupportedException e) {
            throw new RoutingTableException("Transaction error - while trying to remove route id="
                    + routeId, e);
        } catch (HeuristicRollbackException e) {
            throw new RoutingTableException("Transaction error - while trying to remove route id="
                    + routeId, e);
        } catch (RollbackException e) {
            throw new RoutingTableException("Transaction error - while trying to remove route id="
                    + routeId, e);
        } catch (HeuristicMixedException e) {
            throw new RoutingTableException("Transaction error - while trying to remove route id="
                    + routeId, e);
        } catch (javax.transaction.SystemException e) {
            throw new SystemException("System error occurred - while trying to remove with value", e);
        }
    }

    @Override
    public Set<R> getRoutes(I routeId) {

        // Note: currently works for global routes only wherein there is just single
        // route
        Preconditions.checkNotNull(routeId, "getARoute: routeId cannot be null!");
        return (Set<R>) routingTableCache.get(routeId);
    }

    @Override
    public R getARoute(I routeId) {
        throw new UnsupportedOperationException("Not implemented yet!");
    }

    /**
     * @deprecated doesn't do anything will be removed once listeners used
     *             whiteboard pattern Registers listener for sending any change
     *             notification
     * @param listener
     */
    @Override
    public void registerRouteChangeListener(RouteChangeListener listener) {

    }

    public void setRouteChangeListener(RouteChangeListener rcl) {
        if(rcl != null){
            routeChangeListeners.add(rcl);
        }else{
            log.warn("setRouteChangeListener called with null listener");
        }
    }

    public void unSetRouteChangeListener(RouteChangeListener rcl) {
        if(rcl != null){
         routeChangeListeners.remove(rcl);
        }else{
            log.warn("unSetRouteChangeListener called with null listener");
        }
    }

    /**
     * Returning the set of route change listeners for Unit testing Note: the
     * package scope is default
     *
     * @return List of registered RouteChangeListener<I,R> listeners
     */
    Set<RouteChangeListener> getRegisteredRouteChangeListeners() {
        return routeChangeListeners;
    }

    public void setClusterGlobalServices(IClusterGlobalServices clusterGlobalServices) {
        this.clusterGlobalServices = clusterGlobalServices;
    }

    public void unsetClusterGlobalServices(IClusterGlobalServices clusterGlobalServices) {
        if((clusterGlobalServices != null ) &&  (this.clusterGlobalServices.equals(clusterGlobalServices))){
            this.clusterGlobalServices = null;
        }
    }

    /**
     * Creates the Routing Table clustered global services cache
     *
     * @throws CacheExistException
     *           -- cluster global services exception when cache exist
     * @throws CacheConfigException
     *           -- cluster global services exception during cache config
     * @throws CacheListenerAddException
     *           -- cluster global services exception during adding of listener
     */

    void createRoutingTableCache() throws CacheExistException, CacheConfigException,
            CacheListenerAddException {
        // TBD: HOW DO WE DECIDE ON PROPERTIES OF THE CACHE i.e. what duration it
        // should be caching?

        // let us check here if the cache already exists -- if so don't create
        if (!clusterGlobalServices.existCache(ROUTING_TABLE_GLOBAL_CACHE)) {

            if (log.isDebugEnabled()) {
                log.debug("createRoutingTableCache: creating a new routing table cache "
                        + ROUTING_TABLE_GLOBAL_CACHE);
            }
            routingTableCache = clusterGlobalServices.createCache(ROUTING_TABLE_GLOBAL_CACHE,
                    EnumSet.of(IClusterServices.cacheMode.TRANSACTIONAL));
        } else {
            if (log.isDebugEnabled()) {
                log.debug("createRoutingTableCache: found existing routing table cache "
                        + ROUTING_TABLE_GLOBAL_CACHE);
            }
            routingTableCache = clusterGlobalServices.getCache(ROUTING_TABLE_GLOBAL_CACHE);
        }

    }

    /**
     * Function called by the dependency manager when all the required
     * dependencies are satisfied
     *
     */
    void init(Component c) {
        try {

            createRoutingTableCache();
        } catch (CacheExistException e) {
            throw new IllegalStateException("could not construct routing table cache");
        } catch (CacheConfigException e) {
            throw new IllegalStateException("could not construct routing table cache");
        } catch (CacheListenerAddException e) {
            throw new IllegalStateException("could not construct routing table cache");
        }
    }

    /**
     * Get routing table method is useful for unit testing <note>It has package
     * scope</note>
     */
    ConcurrentMap getRoutingTableCache() {
        return this.routingTableCache;
    }

    /**
     * Invoked when a new entry is available in the cache, the key is only
     * provided, the value will come as an entryUpdate invocation
     *
     * @param key
     *          Key for the entry just created
     * @param cacheName
     *          name of the cache for which update has been received
     * @param originLocal
     *          true if the event is generated from this node
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
     * @param key
     *          Key for the entry modified
     * @param new_value
     *          the new value the key will have
     * @param cacheName
     *          name of the cache for which update has been received
     * @param originLocal
     *          true if the event is generated from this node
     */
    @Override
    public void entryUpdated(I key, R new_value, String cacheName, boolean originLocal) {
        if (log.isDebugEnabled()) {
            log.debug("RoutingTableUpdates: entryUpdated  routeId = " + key + ",value = " + new_value
                    + " ,cacheName=" + cacheName + " originLocal="+originLocal);
        }
        if (!originLocal) {
            for (RouteChangeListener rcl : routeChangeListeners) {
                rcl.onRouteUpdated(key, new_value);
            }
        }
    }

    /**
     * Called anytime a given key is removed from the ConcurrentHashMap we are
     * listening to.
     *
     * @param key
     *          Key of the entry removed
     * @param cacheName
     *          name of the cache for which update has been received
     * @param originLocal
     *          true if the event is generated from this node
     */
    @Override
    public void entryDeleted(I key, String cacheName, boolean originLocal) {
        if (log.isDebugEnabled()) {
            log.debug("RoutingTableUpdates: entryUpdated  routeId = " + key + " local = " + originLocal
                    + " cacheName=" + cacheName + " originLocal="+originLocal);
        }
        if (!originLocal) {
            for (RouteChangeListener rcl : routeChangeListeners) {
                rcl.onRouteDeleted(key);
            }
        }
    }
}