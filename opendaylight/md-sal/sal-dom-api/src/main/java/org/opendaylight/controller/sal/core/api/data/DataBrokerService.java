/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.core.api.data;

import org.opendaylight.controller.md.sal.common.api.data.DataChangePublisher;
import org.opendaylight.controller.md.sal.common.api.data.DataModificationTransactionFactory;
import org.opendaylight.controller.md.sal.common.api.data.DataReader;
import org.opendaylight.controller.sal.core.api.BrokerService;
import org.opendaylight.yangtools.yang.data.api.CompositeNode;
import org.opendaylight.yangtools.yang.data.api.InstanceIdentifier;


/**
 * DataBrokerService provides unified access to the data stores available in the
 * system.
 * 
 * 
 * @see DataProviderService
 * 
 */
public interface DataBrokerService extends 
    BrokerService, //
    DataReader<InstanceIdentifier, CompositeNode>, //
    DataModificationTransactionFactory<InstanceIdentifier, CompositeNode>, //
    DataChangePublisher<InstanceIdentifier, CompositeNode, DataChangeListener> {


    @Override
    public CompositeNode readConfigurationData(InstanceIdentifier path);

    @Override
    public CompositeNode readOperationalData(InstanceIdentifier path);

    DataModificationTransaction beginTransaction();
}
