/*
 * Copyright (c) 2017 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.binding.api;

import com.google.common.base.Optional;
import com.google.common.collect.ForwardingObject;
import com.google.common.util.concurrent.CheckedFuture;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

/**
 * Utility {@link ReadOnlyTransaction} implementation which forwards all interface method
 * invocation to a delegate instance.
 */
@SuppressWarnings("deprecation") // due to CheckedFuture
public class ForwardingReadOnlyTransaction extends ForwardingObject implements ReadOnlyTransaction {

    private final ReadOnlyTransaction delegate;

    protected ForwardingReadOnlyTransaction(ReadOnlyTransaction delegate) {
        super();
        this.delegate = delegate;
    }

    @Override
    protected ReadTransaction delegate() {
        return delegate;
    }

    @Override
    public <T extends DataObject> CheckedFuture<Optional<T>, ReadFailedException> read(LogicalDatastoreType store,
            InstanceIdentifier<T> path) {
        return delegate.read(store, path);
    }

    @Override
    public Object getIdentifier() {
        return delegate.getIdentifier();
    }

    @Override
    public void close() {
        delegate.close();
    }
}
