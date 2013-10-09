/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.binding.api.data;

import java.util.EventListener;

import org.opendaylight.controller.md.sal.common.api.TransactionStatus;
import org.opendaylight.controller.md.sal.common.api.data.DataModification;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.concepts.Registration;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public interface DataModificationTransaction extends DataModification<InstanceIdentifier<? extends DataObject>, DataObject> {

    ListenerRegistration<DataTransactionListener> registerListener(DataTransactionListener listener);


    //FIXME: After 0.6 Release of YANG-Binding
    //public <T extends DataObject> T readOperationalData(InstanceIdentifier<T> path);
    //public <T extends DataObject> T readConfigurationData(InstanceIdentifier<T> path);

    public interface DataTransactionListener extends EventListener {
        void onStatusUpdated(DataModificationTransaction transaction,TransactionStatus status);
    }
}
