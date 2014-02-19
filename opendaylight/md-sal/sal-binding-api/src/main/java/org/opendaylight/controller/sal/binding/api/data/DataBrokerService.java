/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.binding.api.data;

import org.opendaylight.controller.md.sal.common.api.data.DataChangePublisher;
import org.opendaylight.controller.md.sal.common.api.data.DataModificationTransactionFactory;
import org.opendaylight.controller.md.sal.common.api.data.DataReader;
import org.opendaylight.controller.sal.binding.api.BindingAwareService;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

/**
 * DataBrokerService provides unified access to the data stores available in the
 * system.
 *
 *
 * @see DataProviderService
 */
public interface DataBrokerService extends //
        BindingAwareService, //
        DataModificationTransactionFactory<InstanceIdentifier<? extends DataObject>, DataObject>, //
        DataReader<InstanceIdentifier<? extends DataObject>, DataObject>, //
        DataChangePublisher<InstanceIdentifier<? extends DataObject>, DataObject, DataChangeListener> {
    /**
     * Creates a data modification transaction.
     *
     * @return new blank data modification transaction.
     */
    @Override
	DataModificationTransaction beginTransaction();

    /**
     * Reads data subtree from configurational store.
     * (Store which is populated by consumer, which is usually used to
     * inject state into providers. E.g. Flow configuration)-
     *
     */
    @Override
    public DataObject readConfigurationData(InstanceIdentifier<? extends DataObject> path);

    /**
     * Reads data subtree from operational store.
     * (Store which is populated by providers, which is usually used to
     * capture state of providers. E.g. Topology)
     *
     */
    @Override
    public DataObject readOperationalData(InstanceIdentifier<? extends DataObject> path);

    /**
     * Register a data change listener for particular subtree.
     *
     * Callback is invoked each time data in subtree changes.
     *
     */
    @Override
    public ListenerRegistration<DataChangeListener> registerDataChangeListener(
            InstanceIdentifier<? extends DataObject> path, DataChangeListener listener);
}
