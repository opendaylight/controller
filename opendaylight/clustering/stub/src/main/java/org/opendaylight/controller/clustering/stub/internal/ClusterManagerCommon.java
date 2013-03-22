
/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.clustering.stub.internal;

import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;
import java.net.UnknownHostException;
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
import javax.transaction.TransactionManager;

import org.opendaylight.controller.clustering.services.CacheConfigException;
import org.opendaylight.controller.clustering.services.CacheExistException;
import org.opendaylight.controller.clustering.services.CacheListenerAddException;
import org.opendaylight.controller.clustering.services.ICacheUpdateAware;
import org.opendaylight.controller.clustering.services.IClusterServices;
import org.opendaylight.controller.clustering.services.IClusterServicesCommon;
import org.opendaylight.controller.clustering.services.ICoordinatorChangeAware;
import org.opendaylight.controller.clustering.services.IListenRoleChange;
import org.opendaylight.controller.clustering.services.ListenRoleChangeAddException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Dictionary;
import java.util.Collections;
import java.util.HashSet;
import org.apache.felix.dm.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

abstract public class ClusterManagerCommon implements IClusterServicesCommon {
    protected String containerName = "";
    protected static final Logger logger = LoggerFactory
            .getLogger(ClusterManagerCommon.class);
    private InetAddress loopbackAddress;
    private ConcurrentMap<String, ConcurrentMap<?, ?>> caches = new ConcurrentHashMap<String, ConcurrentMap<?, ?>>();

    protected ClusterManagerCommon() throws UnknownHostException {
        loopbackAddress = InetAddress.getByName("127.0.0.1");
    }

    /**
     * Function called by the dependency manager when all the required
     * dependencies are satisfied
     *
     */
    void init(Component c) {
        Dictionary props = c.getServiceProperties();
        if (props != null) {
            this.containerName = (String) props.get("containerName");
            logger.debug("Running containerName:" + this.containerName);
        } else {
            // In the Global instance case the containerName is empty
            this.containerName = "";
        }
    }

    /**
     * Function called by the dependency manager when any of the required
     * dependencies are going away
     *
     */
    void destroy() {
        // Clear the caches, will restart on the new life
        this.caches.clear();
    }

    @Override
    public ConcurrentMap<?, ?> createCache(String cacheName,
            Set<IClusterServices.cacheMode> cMode) throws CacheExistException,
            CacheConfigException {
        ConcurrentMap<?, ?> res = this.caches.get(cacheName);
        if (res == null) {
            res = new ConcurrentHashMap();
            this.caches.put(cacheName, res);
            return res;
        }
        throw new CacheExistException();
    }

    @Override
    public ConcurrentMap<?, ?> getCache(String cacheName) {
        return this.caches.get(cacheName);
    }

    @Override
    public void destroyCache(String cacheName) {
        this.caches.remove(cacheName);
    }

    @Override
    public boolean existCache(String cacheName) {
        return (this.caches.get(cacheName) != null);
    }

    @Override
    public Set<String> getCacheList() {
        return this.caches.keySet();
    }

    @Override
    public Properties getCacheProperties(String cacheName) {
        return null;
    }

    @Override
    public void tbegin() throws NotSupportedException, SystemException {
    }

    @Override
    public void tcommit() throws RollbackException, HeuristicMixedException,
            HeuristicRollbackException, java.lang.SecurityException,
            java.lang.IllegalStateException, SystemException {
    }

    @Override
    public void trollback() throws java.lang.IllegalStateException,
            java.lang.SecurityException, SystemException {
    }

    @Override
    public Transaction tgetTransaction() throws SystemException {
        return null;
    }

    @Override
    public List<InetAddress> getClusteredControllers() {
        List<InetAddress> res = new ArrayList<InetAddress>();
        res.add(loopbackAddress);
        return res;
    }

    @Override
    public InetAddress getMyAddress() {
        return loopbackAddress;
    }

    @Override
    public InetAddress getCoordinatorAddress() {
        return loopbackAddress;
    }

    @Override
    public boolean amICoordinator() {
        return true;
    }
}
