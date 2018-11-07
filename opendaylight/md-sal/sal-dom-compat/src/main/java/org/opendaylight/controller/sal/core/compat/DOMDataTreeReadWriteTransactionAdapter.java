/*
 * Copyright (c) 2018 Pantheon Technologies, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.core.compat;

import static java.util.Objects.requireNonNull;
import static org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType.fromMdsal;

import com.google.common.collect.ForwardingObject;
import com.google.common.util.concurrent.FluentFuture;
import com.google.common.util.concurrent.MoreExecutors;
import java.util.Optional;
import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.controller.md.sal.dom.api.DOMDataReadWriteTransaction;
import org.opendaylight.mdsal.common.api.CommitInfo;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.mdsal.dom.api.DOMDataTreeReadWriteTransaction;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;

public class DOMDataTreeReadWriteTransactionAdapter extends ForwardingObject
        implements DOMDataTreeReadWriteTransaction {
    private final DOMDataReadWriteTransaction delegate;

    public DOMDataTreeReadWriteTransactionAdapter(final DOMDataReadWriteTransaction delegate) {
        this.delegate = requireNonNull(delegate);
    }

    @Override
    public Object getIdentifier() {
        return delegate().getIdentifier();
    }

    @Override
    public FluentFuture<Optional<NormalizedNode<?, ?>>> read(final LogicalDatastoreType store,
            final YangInstanceIdentifier path) {
        return FluentFuture.from(delegate.read(fromMdsal(store), path)).transform(opt -> opt.toJavaUtil(),
            MoreExecutors.directExecutor());
    }

    @Override
    public FluentFuture<Boolean> exists(final LogicalDatastoreType store, final YangInstanceIdentifier path) {
        return FluentFuture.from(delegate.exists(fromMdsal(store), path));
    }

    @Override
    public void close() {
        cancel();
    }

    @Override
    public void put(final LogicalDatastoreType store, final YangInstanceIdentifier path,
            final NormalizedNode<?, ?> data) {
        delegate().put(fromMdsal(store), path, data);
    }

    @Override
    public void merge(final LogicalDatastoreType store, final YangInstanceIdentifier path,
            final NormalizedNode<?, ?> data) {
        delegate().merge(fromMdsal(store), path, data);
    }

    @Override
    public void delete(final LogicalDatastoreType store, final YangInstanceIdentifier path) {
        delegate().delete(fromMdsal(store), path);
    }

    @Override
    public @NonNull FluentFuture<? extends @NonNull CommitInfo> commit() {
        return delegate().commit();
    }

    @Override
    public boolean cancel() {
        return delegate().cancel();
    }

    @Override
    protected DOMDataReadWriteTransaction delegate() {
        return delegate;
    }
}
