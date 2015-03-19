/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.binding.impl;

import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.controller.md.sal.dom.api.DOMDataReadOnlyTransaction;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

import com.google.common.base.Optional;
import com.google.common.util.concurrent.CheckedFuture;

class BindingDOMReadTransactionAdapter extends AbstractForwardedTransaction<DOMDataReadOnlyTransaction> implements
        ReadOnlyTransaction {

    protected BindingDOMReadTransactionAdapter(final DOMDataReadOnlyTransaction delegate,
            final BindingToNormalizedNodeCodec codec) {
        super(delegate, codec);
    }

    @Override
    public <T extends DataObject> CheckedFuture<Optional<T>,ReadFailedException> read(
            final LogicalDatastoreType store, final InstanceIdentifier<T> path) {
        return doRead(getDelegate(),store, path);
    }

    @Override
    public void close() {
        getDelegate().close();
    }

}