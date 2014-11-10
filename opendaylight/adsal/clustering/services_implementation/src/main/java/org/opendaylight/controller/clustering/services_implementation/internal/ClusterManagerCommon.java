
/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.clustering.services_implementation.internal;

import java.net.InetAddress;
import java.util.Collections;
import java.util.Dictionary;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;

import javax.transaction.HeuristicMixedException;
import javax.transaction.HeuristicRollbackException;
import javax.transaction.NotSupportedException;
import javax.transaction.RollbackException;
import javax.transaction.SystemException;
import javax.transaction.Transaction;

import org.apache.felix.dm.Component;
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

public abstract class ClusterManagerCommon implements IClusterServicesCommon {
    protected String containerName = null;
    protected IClusterServices clusterService = null;
    protected static final Logger logger = LoggerFactory
            .getLogger(ClusterManagerCommon.class);
    private ConcurrentMap<String, GetUpdatesContainer> cacheUpdateAware =
        new ConcurrentHashMap<String, GetUpdatesContainer>();
    private Set<ICoordinatorChangeAware> coordinatorChangeAware = Collections
            .synchronizedSet(new HashSet<ICoordinatorChangeAware>());
    private ListenCoordinatorChange coordinatorChangeListener = null;

    /**
     * Class needed to listen to the role changes from the cluster
     * manager and to pass it along to the other components that
     * export the interface ICoordinatorChangeAware
     */
    class ListenCoordinatorChange implements IListenRoleChange {
        @Override
        public void newActiveAvailable() {
            if (coordinatorChangeAware != null) {
                // Make sure to look the set while walking it
                synchronized (coordinatorChangeAware) {
                    for (ICoordinatorChangeAware s : coordinatorChangeAware) {
                        // Now walk every instance and signal that the
                        // coordinator has changed
                        s.coordinatorChanged();
                    }
                }
            }
        }
    }

    void setCoordinatorChangeAware(ICoordinatorChangeAware s) {
        if (this.coordinatorChangeAware != null) {
            this.coordinatorChangeAware.add(s);
        }
    }

    void unsetCoordinatorChangeAware(ICoordinatorChangeAware s) {
        if (this.coordinatorChangeAware != null) {
            this.coordinatorChangeAware.remove(s);
        }
    }

    void setCacheUpdateAware(Map props, ICacheUpdateAware s) {
        logger.trace("CacheUpdateAware being set on container:{}",
                     this.containerName);
        if (this.cacheUpdateAware != null) {
            Set<String> caches = (Set<String>)props.get("cachenames");
            if (caches != null) {
                logger.trace("cachenames provided below:");
                for (String cache : caches) {
                    if (this.cacheUpdateAware.get(cache) != null) {
                        logger.error("cachename:{} on container:{} has already a listener", cache, this.containerName);
                    } else {
                        GetUpdatesContainer<?, ?> up = new GetUpdatesContainer(s, this.containerName, cache);
                        if (up != null) {
                            try {
                                this.clusterService.addListener(this.containerName,
                                                                cache, up);
                                this.cacheUpdateAware.put(cache, up);
                                logger.trace("cachename:{} on container:{} has " +
                                             "been registered", cache,
                                             this.containerName);
                            } catch (CacheListenerAddException exc) {
                                logger.debug("Cache {} didn't exist when {} tried to register to its updates", cache, s);
                                // Do nothing, the important is that
                                // we don't register the listener in
                                // the shadow, and we are not doing
                                // that.
                            }
                        }
                    }
                }
            }
        }
    }

    void unsetCacheUpdateAware(Map props, ICacheUpdateAware s) {
        logger.trace("CacheUpdateAware being unset on container:{}",
                     this.containerName);
        if (this.cacheUpdateAware != null) {
            Set<String> caches = (Set<String>)props.get("cachenames");
            if (caches != null) {
                logger.trace("cachenames provided below:");
                GetUpdatesContainer<?, ?> up = null;
                for (String cache : caches) {
                    up = this.cacheUpdateAware.get(cache);
                    if (up != null) {
                        this.cacheUpdateAware.remove(cache);
                        this.clusterService.removeListener(this.containerName,
                                                           cache, up);
                    }
                }
            }
        }
    }

    public void setClusterService(IClusterServices s) {
        this.clusterService = s;
    }

    public void unsetClusterServices(IClusterServices s) {
        if (this.clusterService == s) {
            this.clusterService = null;
        }
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
            logger.debug("Running containerName: {}", this.containerName);
        } else {
            // In the Global instance case the containerName is empty
            this.containerName = "";
        }
        if (this.clusterService != null) {
            this.coordinatorChangeListener = new ListenCoordinatorChange();
            try {
                this.clusterService
                        .listenRoleChange(this.coordinatorChangeListener);
                logger.debug("Coordinator change handler registered");
            } catch (ListenRoleChangeAddException ex) {
                logger.error("Could not register coordinator change");
            }
        }
    }

    /**
     * Function called by the dependency manager when any of the required
     * dependencies are going away
     *
     */
    void destroy() {
        if (this.clusterService != null
                && this.coordinatorChangeListener != null) {
            this.clusterService
                    .unlistenRoleChange(this.coordinatorChangeListener);
            this.coordinatorChangeListener = null;
            logger.debug("Coordinator change handler UNregistered");
        }
    }

    @Override
    public ConcurrentMap<?, ?> createCache(String cacheName,
            Set<IClusterServices.cacheMode> cMode) throws CacheExistException,
            CacheConfigException {
        if (this.clusterService != null) {
            return this.clusterService.createCache(this.containerName,
                    cacheName, cMode);
        } else {
            return null;
        }
    }

    @Override
    public ConcurrentMap<?, ?> getCache(String cacheName) {
        if (this.clusterService != null) {
            return this.clusterService.getCache(this.containerName, cacheName);
        } else {
            return null;
        }
    }

    @Override
    public void destroyCache(String cacheName) {
        if (this.clusterService != null) {
            this.clusterService.destroyCache(this.containerName, cacheName);
        }
    }

    @Override
    public boolean existCache(String cacheName) {
        if (this.clusterService != null) {
            return this.clusterService
                    .existCache(this.containerName, cacheName);
        } else {
            return false;
        }
    }

    @Override
    public Set<String> getCacheList() {
        if (this.clusterService != null) {
            return this.clusterService.getCacheList(this.containerName);
        } else {
            return null;
        }
    }

    @Override
    public Properties getCacheProperties(String cacheName) {
        if (this.clusterService != null) {
            return this.clusterService.getCacheProperties(this.containerName,
                    cacheName);
        } else {
            return null;
        }
    }

    @Override
    public void tbegin() throws NotSupportedException, SystemException {
        if (this.clusterService != null) {
            this.clusterService.tbegin();
        } else {
            throw new IllegalStateException();
        }
    }

    @Override
    public void tbegin(long timeout, TimeUnit unit) throws NotSupportedException, SystemException {
        if (this.clusterService != null) {
            this.clusterService.tbegin(timeout, unit);
        } else {
            throw new IllegalStateException();
        }
    }

    @Override
    public void tcommit() throws RollbackException, HeuristicMixedException,
            HeuristicRollbackException, java.lang.SecurityException,
            java.lang.IllegalStateException, SystemException {
        if (this.clusterService != null) {
            this.clusterService.tcommit();
        } else {
            throw new IllegalStateException();
        }
    }

    @Override
    public void trollback() throws java.lang.IllegalStateException,
            java.lang.SecurityException, SystemException {
        if (this.clusterService != null) {
            this.clusterService.trollback();
        } else {
            throw new IllegalStateException();
        }
    }

    @Override
    public Transaction tgetTransaction() throws SystemException {
        if (this.clusterService != null) {
            return this.clusterService.tgetTransaction();
        } else {
            return null;
        }
    }

    @Override
    public List<InetAddress> getClusteredControllers() {
        if (this.clusterService != null) {
            return this.clusterService.getClusteredControllers();
        } else {
            return null;
        }
    }

    @Override
    public InetAddress getMyAddress() {
        if (this.clusterService != null) {
            return this.clusterService.getMyAddress();
        } else {
            return null;
        }
    }

    @Override
    public InetAddress getCoordinatorAddress() {
        if (this.clusterService != null) {
            return this.clusterService.getActiveAddress();
        } else {
            return null;
        }
    }

    @Override
    public boolean amICoordinator() {
        if (this.clusterService != null) {
            return (!this.clusterService.amIStandby());
        } else {
            return false;
        }
    }
}
