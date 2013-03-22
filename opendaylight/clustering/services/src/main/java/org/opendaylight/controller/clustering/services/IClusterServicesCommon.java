
/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

/**
 * @file   IClusterServicesCommon.java
 *
 * @brief  : Set of services and application will expect from the
 * clustering services provider. This interface is going to be the
 * base for per-container and Global services and so the container
 * parameter is omitted but who uses knows about it
 *
 * Contract between the applications and the clustering service
 * providers. Common version
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
 * @deprecated for internal use
 * Set of services and application will expect from the
 * clustering services provider. This interface is going to be the
 * base for per-container and Global services and so the container
 * parameter is omitted but who uses knows about it
 *
 */
public interface IClusterServicesCommon {
    /**
     * Method that will create a new named cache. The data
     * structure if already present will cause an exception to be
     * thrown to the caller.
     *
     * @param cacheName Name of the ConcurrentHashMap to create
     * @param cMode Mode of the cache that need to be retrieved. This
     * is a set such that more than one property can be provided, of
     * course contrasting requirements will not be accepted and in
     * that case an exception is thrown
     *
     * @return ConcurrentHashMap to be used to modify the data structure
     */
    ConcurrentMap<?, ?> createCache(String cacheName,
            Set<IClusterServices.cacheMode> cMode) throws CacheExistException,
            CacheConfigException;

    /**
     * Method that will retrieve and return the handle to modify a
     * data structire distributed via clustering services. The
     * datastructure shall already have been created else a null
     * reference will be returned.
     *
     * @param cacheName Name of the ConcurrentHashMap to retrieve
     *
     * @return ConcurrentHashMap to be used to modify the data structure
     */
    ConcurrentMap<?, ?> getCache(String cacheName);

    /**
     * Destroy a cachename given cachename, if doesn't exist
     * the function does nothing. If the datastructure exists, the
     * whole cluster will destroy the instance
     *
     * @param cacheName Name of the ConcurrentHashMap to destroy
     */
    void destroyCache(String cacheName);

    /**
     * Function to test the existance of a cache with a given name already
     *
     * @param cacheName Name of the ConcurrentHashMap to destroy
     *
     * @return true if exists already, false otherwise
     */
    boolean existCache(String cacheName);

    /**
     * Return the list of all teh caches registered in the context of
     * the called
     *
     *
     * @return The set of names, expressed as strings
     */
    Set<String> getCacheList();

    /**
     * Return a list of properties that caracterize the cache
     *
     * @param cacheName Name of the cache
     *
     * @return The list of properties related to the cache
     */
    Properties getCacheProperties(String cacheName);

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
     *
     * Get the InetAddress of the coordinator controller in the cluster
     *
     * @return Address of the coordinator controller
     */
    InetAddress getCoordinatorAddress();

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
     * Function that is used to know if the node on which is called is
     * the cluster coordinator. The API is useful in scenario where
     * the same logic is not worthed to be replicated on multiple
     * nodes in the cluster and one can cook it up for all the
     * others. In this scenario running the logic on the coordinator
     * make sense, this of course implies logics that are not heavy
     * and don't need to be scaled out linearly with the size of the
     * cluster.
     *
     * @return true if the node on which the API is called is the
     * coordinator for the cluster
     */
    boolean amICoordinator();
}
