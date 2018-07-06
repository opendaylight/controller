/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.binding.impl;

import com.google.common.base.Optional;
import com.google.common.util.concurrent.CheckedFuture;
import org.opendaylight.controller.md.sal.binding.api.ReadWriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

class BindingReadWriteTransactionAdapter
        extends BindingWriteTransactionAdapter<org.opendaylight.mdsal.binding.api.ReadWriteTransaction>
        implements ReadWriteTransaction {

    BindingReadWriteTransactionAdapter(
            org.opendaylight.mdsal.binding.api.ReadWriteTransaction delegate) {
        super(delegate);
    }

    @Override
    public <T extends DataObject> CheckedFuture<Optional<T>,ReadFailedException> read(
            final LogicalDatastoreType store, final InstanceIdentifier<T> path) {
        return read(getDelegate(), store, path);
    }
}
