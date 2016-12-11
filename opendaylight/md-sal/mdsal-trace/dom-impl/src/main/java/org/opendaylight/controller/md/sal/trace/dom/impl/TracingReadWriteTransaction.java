/*
 * Copyright (c) 2016 Red Hat, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.trace.dom.impl;


import com.google.common.base.Optional;
import com.google.common.util.concurrent.CheckedFuture;
import java.util.Objects;

import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.controller.md.sal.dom.api.DOMDataReadWriteTransaction;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;


class TracingReadWriteTransaction extends TracingWriteTransaction implements DOMDataReadWriteTransaction {

    private final DOMDataReadWriteTransaction delegate;

    TracingReadWriteTransaction(DOMDataReadWriteTransaction delegate, TracingBroker tracingBroker) {
        super(delegate, tracingBroker);
        this.delegate = Objects.requireNonNull(delegate);
    }

    @Override
    public CheckedFuture<Optional<NormalizedNode<?, ?>>, ReadFailedException> read(
                                                            LogicalDatastoreType store, YangInstanceIdentifier yiid) {
        return delegate.read(store, yiid);
    }

    @Override
    public CheckedFuture<Boolean, ReadFailedException> exists(LogicalDatastoreType store, YangInstanceIdentifier yiid) {
        return delegate.exists(store, yiid);
    }
}
