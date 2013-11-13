
/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */



package org.opendaylight.controller.datastore.internal;

import com.google.common.base.Preconditions;
import org.apache.felix.dm.Component;
import org.opendaylight.controller.clustering.services.CacheConfigException;
import org.opendaylight.controller.clustering.services.IClusterGlobalServices;
import org.opendaylight.controller.datastore.ClusteredDataStore;
import org.opendaylight.controller.md.sal.common.api.data.DataModification;
import org.opendaylight.yangtools.yang.data.api.CompositeNode;
import org.opendaylight.yangtools.yang.data.api.InstanceIdentifier;

public class ClusteredDataStoreManager implements ClusteredDataStore {

    private ClusteredDataStore clusteredDataStore = null;
    private IClusterGlobalServices clusterGlobalServices = null;

    @Override
    public DataCommitTransaction<InstanceIdentifier, CompositeNode> requestCommit(DataModification<InstanceIdentifier, CompositeNode> modification) {
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


    public void setClusterGlobalServices(IClusterGlobalServices clusterGlobalServices){
        this.clusterGlobalServices = clusterGlobalServices;
    }

    public void unsetClusterGlobalServices(IClusterGlobalServices clusterGlobalServices){
        this.clusterGlobalServices = null;
        this.clusteredDataStore = null;
    }


    /**
     * Function called by the dependency manager when all the required
     * dependencies are satisfied
     *
     */
    void init(Component c) {
        try {
        	//Adding creation of the clustered data store in its own method to make the method unit testable
            clusteredDataStore = createClusteredDataStore(c);
        } catch (CacheConfigException e) {
            throw new IllegalStateException("could not construct clusteredDataStore");
        }
    }
    protected ClusteredDataStore createClusteredDataStore(Component c) throws CacheConfigException{
    	return  new ClusteredDataStoreImpl(clusterGlobalServices);
    }
}
