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
import org.opendaylight.controller.sal.binding.api.BindingAwareProvider;
import org.opendaylight.controller.sal.common.DataStoreIdentifier;
import org.opendaylight.yangtools.concepts.Registration;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

/**
 * DataProviderService is common access point for {@link BindingAwareProvider} providers
 * to access data trees described by the YANG model.
 *
 */
public interface DataProviderService extends DataBrokerService, DataProvisionService<InstanceIdentifier<? extends DataObject>, DataObject> {

    
    /**
     * Registers a data reader for particular subtree of overal YANG data tree.
     * 
     * Registered data reader is called if anyone tries to read data from 
     * paths which are nested to provided path.
     * 
     * @param path Subpath which is handled by registered data reader
     * @param reader Instance of reader which 
     * @return Registration object for reader. Invoking {@link Registration#close()} will unregister reader.
     */
    Registration<DataReader<InstanceIdentifier<? extends DataObject>,DataObject>> registerDataReader(InstanceIdentifier<? extends DataObject> path,DataReader<InstanceIdentifier<? extends DataObject>,DataObject> reader);
}
