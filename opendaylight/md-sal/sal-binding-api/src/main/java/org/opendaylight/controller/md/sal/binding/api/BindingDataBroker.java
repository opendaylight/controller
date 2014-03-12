/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.binding.api;

import org.opendaylight.controller.md.sal.common.api.data.AsyncDataBroker;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public interface BindingDataBroker extends AsyncDataBroker<InstanceIdentifier<?>, DataObject, BindingDataChangeListener>{
    @Override
    BindingDataReadTransaction newReadOnlyTransaction();

    @Override
    BindingDataReadWriteTransaction newReadWriteTransaction();

    @Override
    BindingDataWriteTransaction newWriteOnlyTransaction();

    @Override
    ListenerRegistration<BindingDataChangeListener> registerDataChangeListener(LogicalDatastoreType store,
            InstanceIdentifier<?> path, BindingDataChangeListener listener, DataChangeScope triggeringScope);
}
