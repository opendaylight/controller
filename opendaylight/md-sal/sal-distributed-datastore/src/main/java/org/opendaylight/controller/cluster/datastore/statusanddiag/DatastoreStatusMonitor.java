/*
 * Copyright (c) 2017 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.statusanddiag;

import org.opendaylight.infrautils.diagstatus.MBeanUtils;
import org.opendaylight.infrautils.diagstatus.ServiceDescriptor;
import org.opendaylight.infrautils.diagstatus.ServiceState;
import org.opendaylight.infrautils.diagstatus.ServiceStatusProvider;

public class DatastoreStatusMonitor implements ServiceStatusProvider {

    private static final String DATASTORE_SERVICE_NAME = "DATASTORE";

    @Override
    public ServiceDescriptor getServiceDescriptor() {
        ServiceState dataStoreServiceState = ServiceState.ERROR;
        String statusDesc;
        Object operSyncStatusValue = MBeanUtils.readMBeanAttribute("org.opendaylight.controller:type=" +
                "DistributedOperationalDatastore,Category=ShardManager,name=shard-manager-operational",
                "SyncStatus");
        Object configSyncStatusValue = MBeanUtils.readMBeanAttribute("org.opendaylight.controller:type=" +
                        "DistributedConfigDatastore,Category=ShardManager,name=shard-manager-config",
                "SyncStatus");
        if (operSyncStatusValue != null && configSyncStatusValue != null) {
            if ((boolean) operSyncStatusValue && (boolean) configSyncStatusValue) {
                dataStoreServiceState =  ServiceState.OPERATIONAL;
                statusDesc = dataStoreServiceState.name();
            } else {
                statusDesc = "datastore out of sync";
            }
        } else {
            statusDesc = "not able to read datastore sync status";
        }
        return new ServiceDescriptor(DATASTORE_SERVICE_NAME, dataStoreServiceState, statusDesc);
    }
}