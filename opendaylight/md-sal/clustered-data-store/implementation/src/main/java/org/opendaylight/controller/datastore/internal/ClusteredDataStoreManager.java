
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
import org.opendaylight.controller.clustering.services.CacheExistException;
import org.opendaylight.controller.clustering.services.IClusterGlobalServices;
import org.opendaylight.controller.datastore.ClusteredDataStore;
import org.opendaylight.controller.md.sal.common.api.data.DataModification;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class ClusteredDataStoreManager implements ClusteredDataStore {

    private ClusteredDataStoreImpl clusteredDataStore = null;
    private IClusterGlobalServices clusterGlobalServices = null;

    @Override
    public DataCommitTransaction<InstanceIdentifier<? extends Object>, Object> requestCommit(DataModification<InstanceIdentifier<? extends Object>, Object> modification) {
        Preconditions.checkState(clusteredDataStore != null, "clusteredDataStore cannot be null");
        return clusteredDataStore.requestCommit(modification);
    }

    @Override
    public Object readOperationalData(InstanceIdentifier<? extends Object> path) {
        Preconditions.checkState(clusteredDataStore != null, "clusteredDataStore cannot be null");
        return clusteredDataStore.readOperationalData(path);
    }

    @Override
    public Object readConfigurationData(InstanceIdentifier<? extends Object> path) {
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
        } catch (CacheExistException e) {
            throw new IllegalStateException("could not construct clusteredDataStore");
        } catch (CacheConfigException e) {
            throw new IllegalStateException("could not construct clusteredDataStore");
        }
    }
    protected ClusteredDataStoreImpl createClusteredDataStore(Component c) throws CacheExistException,CacheConfigException{
    	return  new ClusteredDataStoreImpl(clusterGlobalServices);
    }
}
