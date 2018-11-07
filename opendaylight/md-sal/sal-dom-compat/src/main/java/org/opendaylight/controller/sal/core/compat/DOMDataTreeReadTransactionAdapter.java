/*
 * Copyright (c) 2018 Pantheon Technologies, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.core.compat;

import static java.util.Objects.requireNonNull;

import com.google.common.collect.ForwardingObject;
import com.google.common.util.concurrent.FluentFuture;
import java.util.Optional;
import org.opendaylight.controller.md.sal.dom.api.DOMDataReadOnlyTransaction;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.mdsal.dom.api.DOMDataTreeReadTransaction;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;

public class DOMDataTreeReadTransactionAdapter extends ForwardingObject implements DOMDataTreeReadTransaction {
    private final DOMDataReadOnlyTransaction delegate;

    public DOMDataTreeReadTransactionAdapter(final DOMDataReadOnlyTransaction delegate) {
        this.delegate = requireNonNull(delegate);
    }

    @Override
    public Object getIdentifier() {
        return delegate().getIdentifier();
    }

    @Override
    public FluentFuture<Optional<NormalizedNode<?, ?>>> read(final LogicalDatastoreType store,
            final YangInstanceIdentifier path) {
        return TransactionUtils.read(delegate(), store, path);
    }

    @Override
    public FluentFuture<Boolean> exists(final LogicalDatastoreType store, final YangInstanceIdentifier path) {
        return TransactionUtils.exists(delegate(), store, path);
    }

    @Override
    public void close() {
        delegate().close();
    }

    @Override
    protected DOMDataReadOnlyTransaction delegate() {
        return delegate;
    }
}
