
/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

/**
 * @file   IClusterServices.java
 *
 * @brief  : Set of services and application will expect from the
 * clustering services provider
 *
 * Contract between the applications and the clustering service
 * providers.
 */

package org.opendaylight.controller.clustering.services;

import java.net.InetAddress;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;

import javax.transaction.HeuristicMixedException;
import javax.transaction.HeuristicRollbackException;
import javax.transaction.NotSupportedException;
import javax.transaction.RollbackException;
import javax.transaction.SystemException;
import javax.transaction.Transaction;

/**
 * Set of services and application will expect from the
 * clustering services provider
 *
 */
public interface IClusterServices {

    /**
     * Enumeration of the several modality with which a
     * ConcurrentHashMap cache can be requested to the clustering
     * services. The property that can be requested can be multiple.
     *
     */
    public enum cacheMode {
        /**
         * Set for a cache that supports transaction that implies that
         * is a transaction is open on the current thread the data
         * will not immediately be reflected in the cache but will be
         * staged till commit or rollback. If the transaction if NOT
         * open the data will immediately go in the cache without
         * staging.
         */
        TRANSACTIONAL,
        /**
         * Set on a cache that doesn't want to support
         * transaction, so irrespective of the fact that we are in
         * the middle of a transaction or no data will be
         * immediately committed in the cache.
         *
         */
        NON_TRANSACTIONAL;
    }

    /**
     * Enumeration of the several properties that a cache can carry
     *
     */
    public enum cacheProps {
        /**
         * The property returned describe the caracteristics of the
         * transaction setup for the cache it was retrieved.
         */
        TRANSACTION_PROP,
        /**
         * The property returned report the clustering
         * caracteristics of the cache for which property was
         * queried.
         */
        CLUSTERING_PROP,
        /**
         * The property returned reports the locking
         * caracteristics of the cache for which the property was
         * queried
         */
        LOCKING_PROP;
    }

    /**
     * Method that will create a new named cache per-container. The data
     * structure if already present will cause an exception to be
     * thrown to the caller.
     *
     * @param containerName Container to which the datastructure is associated
     * @param cacheName Name of the ConcurrentHashMap to create
     * @param cMode Mode of the cache that need to be retrieved. This
     * is a set such that more than one property can be provided, of
     * course contrasting requirements will not be accepted and in
     * that case an exception is thrown
     *
     * @return ConcurrentHashMap to be used to modify the data structure
     */
    ConcurrentMap<?, ?> createCache(String containerName, String cacheName,
            Set<cacheMode> cMode) throws CacheExistException,
            CacheConfigException;

    /**
     * Method that will retrieve and return the handle to modify a
     * data structire distributed via clustering services. The
     * datastructure shall already have been created else a null
     * reference will be returned.
     *
     * @param containerName Container to which the datastructure is associated
     * @param cacheName Name of the ConcurrentHashMap to retrieve
     *
     * @return ConcurrentHashMap to be used to modify the data structure
     */
    ConcurrentMap<?, ?> getCache(String containerName, String cacheName);

    /**
     * Destroy a cachename given containerName/cachename, if doesn't exist
     * the function does nothing. If the datastructure exists, the
     * whole cluster will destroy the instance
     *
     * @param containerName Container to which the datastructure is associated
     * @param cacheName Name of the ConcurrentHashMap to destroy
     */
    void destroyCache(String containerName, String cacheName);

    /**
     * Function to test the existance of a cache with a given name already
     *
     * @param containerName Container to which the datastructure is associated
     * @param cacheName Name of the ConcurrentHashMap to destroy
     *
     * @return true if exists already, false otherwise
     */
    boolean existCache(String containerName, String cacheName);

    /**
     * Return the list of all teh caches registered with a container
     *
     * @param containerName Container for which we want to list all the caches registered
     *
     * @return The set of names, expressed as strings
     */
    Set<String> getCacheList(String containerName);

    /**
     * Return a list of properties that caracterize the cache
     *
     * @param containerName Name of the container where data structure resides
     * @param cacheName Name of the cache
     *
     * @return The list of properties related to the cache
     */
    Properties getCacheProperties(String containerName, String cacheName);

    /**
     * Register an update handler for a given containerName/cacheName
     * shared data structure. Multiple listeners are possible.
     *
     * @param containerName Container to which the datastructure is associated
     * @param cacheName Name of the ConcurrentHashMap for which we
     * want to register the listener
     * @param u Interface to invoke when the updates are received
     */
    void addListener(String containerName, String cacheName, IGetUpdates<?, ?> u)
            throws CacheListenerAddException;

    /**
     * Return a set of interfaces that are interesteed to listen to
     * updates coming for a given datastructure shared via clustering
     * services.
     *
     * @param containerName Container to which the datastructure is associated
     * @param cacheName Name of the ConcurrentHashMap for which we
     * want to retrieve the listener
     */
    Set<IGetUpdates<?, ?>> getListeners(String containerName, String cacheName);

    /**
     * UN-Register an update handler for a given containerName/cacheName
     * shared data structure. Multiple listeners are possible.
     *
     * @param containerName Container to which the datastructure is associated
     * @param cacheName Name of the ConcurrentHashMap for which we
     * want to un-register the listener
     * @param u Interface to un-register
     */
    void removeListener(String containerName, String cacheName,
            IGetUpdates<?, ?> u);

    /**
     * Begin a transaction covering with all the data structures/HW
     * updates. One transaction per-thread can be opened at the
     * most, that means if multiple thread are available, multiple
     * transactions can be outstanding.
     *
     */
    void tbegin() throws NotSupportedException, SystemException;

    /**
     * Commit a transaction covering all the data structures/HW updates.
     */
    void tcommit() throws RollbackException, HeuristicMixedException,
            HeuristicRollbackException, java.lang.SecurityException,
            java.lang.IllegalStateException, SystemException;

    /**
     * Rollback a transaction covering all the data structures/HW updates
     */
    void trollback() throws java.lang.IllegalStateException,
            java.lang.SecurityException, SystemException;

    /**
     * Return the javax.transaction.Transaction associated with this thread
     *
     *
     * @return Return the current transaction associated with this thread
     */
    Transaction tgetTransaction() throws SystemException;

    /**
     * @deprecated
     * Function that says if we are standby in the 1-1 redundancy with
     * active/standby model. The API is not encouraged hence is
     * deprecated. It is supposed to be used as a stop-gap till the
     * active-standby goal is achieved. The only guys that are
     * supposed to use are:
     * - southbound layer, should not listen on the OF port if standby
     * - jetty configuration, on standby jetty should redirect calls
     * to the active.
     *
     * @return true if the role is the one of standby, else false
     */
    boolean amIStandby();

    /**
     * @deprecated
     * Get the InetAddress of the active controller for the
     * active-standby case, where the standby controller has to
     * redirect the HTTP requests received from applications layer
     *
     * @return Address of the active controller
     */
    InetAddress getActiveAddress();

    /**
     * Get the InetAddress of the all the controllers that make up this
     * Cluster
     *
     * @return List of InetAddress'es of all the controllers
     */
    List<InetAddress> getClusteredControllers();

    /**
     * Get the InetAddress of this Controller as seen by the Cluster Manager
     *
     * @return InetAddress of this Controller as seen by the Cluster Manager.
     */
    InetAddress getMyAddress();

    /**
     * @deprecated
     * Register a listener to the event of ChangeRole, raised every
     * time there is a change in the role of active or standby.
     *
     * @param i Interface that will be called when the Role Change happens
     */
    void listenRoleChange(IListenRoleChange i)
            throws ListenRoleChangeAddException;

    /**
     * @deprecated
     * UN-Register a listener to the event of ChangeRole, raised every
     * time there is a change in the role of active or standby.
     *
     * @param i Interface that will be called when the Role Change happens
     */
    void unlistenRoleChange(IListenRoleChange i);
}
