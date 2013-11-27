/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.datastore.internal;

import java.util.Hashtable;

import com.google.common.base.Preconditions;

import org.opendaylight.controller.clustering.services.CacheConfigException;
import org.opendaylight.controller.clustering.services.IClusterGlobalServices;
import org.opendaylight.controller.datastore.ClusteredDataStore;
import org.opendaylight.controller.md.sal.common.api.data.DataModification;
import org.opendaylight.yangtools.yang.data.api.CompositeNode;
import org.opendaylight.yangtools.yang.data.api.InstanceIdentifier;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

public class ClusteredDataStoreManager implements //
        ClusteredDataStore, //
        ServiceTrackerCustomizer<IClusterGlobalServices, IClusterGlobalServices>, //
        AutoCloseable {

    private ClusteredDataStore clusteredDataStore = null;
    private IClusterGlobalServices clusterGlobalServices = null;
    private BundleContext context;

    private ServiceReference<IClusterGlobalServices> firstClusterGlobalReference;
    private ServiceTracker<IClusterGlobalServices, IClusterGlobalServices> clusterTracker;

    @Override
    public DataCommitTransaction<InstanceIdentifier, CompositeNode> requestCommit(
            DataModification<InstanceIdentifier, CompositeNode> modification) {
        Preconditions.checkState(clusteredDataStore != null, "clusteredDataStore cannot be null");
        return clusteredDataStore.requestCommit(modification);
    }

    @Override
    public CompositeNode readOperationalData(InstanceIdentifier path) {
        Preconditions.checkState(clusteredDataStore != null, "clusteredDataStore cannot be null");
        return clusteredDataStore.readOperationalData(path);
    }

    @Override
    public CompositeNode readConfigurationData(InstanceIdentifier path) {
        Preconditions.checkState(clusteredDataStore != null, "clusteredDataStore cannot be null");
        return clusteredDataStore.readConfigurationData(path);
    }

    public Iterable<InstanceIdentifier> getStoredConfigurationPaths() {
        Preconditions.checkState(clusteredDataStore != null, "clusteredDataStore cannot be null");
        return clusteredDataStore.getStoredConfigurationPaths();
    }

    public Iterable<InstanceIdentifier> getStoredOperationalPaths() {
        Preconditions.checkState(clusteredDataStore != null, "clusteredDataStore cannot be null");
        return clusteredDataStore.getStoredOperationalPaths();
    }

    public boolean containsConfigurationPath(InstanceIdentifier path) {
        Preconditions.checkState(clusteredDataStore != null, "clusteredDataStore cannot be null");
        return clusteredDataStore.containsConfigurationPath(path);
    }

    public boolean containsOperationalPath(InstanceIdentifier path) {
        Preconditions.checkState(clusteredDataStore != null, "clusteredDataStore cannot be null");
        return clusteredDataStore.containsOperationalPath(path);
    }

    public void setClusterGlobalServices(IClusterGlobalServices clusterGlobalServices) {
        this.clusterGlobalServices = clusterGlobalServices;
        try {
            // Adding creation of the clustered data store in its own method
            // to make the method unit testable
            clusteredDataStore = createClusteredDataStore();
        } catch (CacheConfigException e) {
            throw new IllegalStateException("could not construct clusteredDataStore");
        }
    }

    @Override
    public IClusterGlobalServices addingService(ServiceReference<IClusterGlobalServices> reference) {
        if (clusterGlobalServices == null) {
            setClusterGlobalServices(context.getService(reference));
            return clusterGlobalServices;
        }
        return null;
    }

    @Override
    public void modifiedService(ServiceReference<IClusterGlobalServices> reference, IClusterGlobalServices service) {

    }

    @Override
    public void removedService(ServiceReference<IClusterGlobalServices> reference, IClusterGlobalServices service) {
        if (clusterGlobalServices == service) {
            clusterGlobalServices = null;
            clusteredDataStore = null;
        }
    }

    public BundleContext getContext() {
        return context;
    }

    public void setContext(BundleContext context) {
        this.context = context;
    }
    
    
    /**
     * Function called by the dependency manager when all the required
     * dependencies are satisfied
     * 
     */
    public void start() {
        if (context != null) {
            clusterTracker = new ServiceTracker<>(context, IClusterGlobalServices.class, this);
            clusterTracker.open();
            
            context.registerService(ClusteredDataStore.class, this, new Hashtable<String,Object>());
        }
    }

    protected ClusteredDataStore createClusteredDataStore() throws CacheConfigException {
        return new ClusteredDataStoreImpl(clusterGlobalServices);
    }

    @Override
    public void close() throws Exception {
        clusterTracker.close();
    }
}
