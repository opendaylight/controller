/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.binding.api.data;


import org.opendaylight.controller.md.sal.common.api.data.DataProvisionService;
import org.opendaylight.controller.md.sal.common.api.data.DataReader;
import org.opendaylight.controller.sal.common.DataStoreIdentifier;
import org.opendaylight.yangtools.concepts.Registration;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;


public interface DataProviderService extends DataBrokerService, DataProvisionService<InstanceIdentifier<? extends DataObject>, DataObject> {

    
    Registration<DataReader<InstanceIdentifier<? extends DataObject>,DataObject>> registerDataReader(InstanceIdentifier<? extends DataObject> path,DataReader<InstanceIdentifier<? extends DataObject>,DataObject> reader);
}
