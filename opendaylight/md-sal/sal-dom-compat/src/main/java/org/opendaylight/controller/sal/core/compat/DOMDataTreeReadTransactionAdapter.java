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
        return FluentFuture.from(delegate.read(fromMdsal(store), path)).transform(opt -> opt.toJavaUtil(),
            MoreExecutors.directExecutor());
    }

    @Override
    public FluentFuture<Boolean> exists(final LogicalDatastoreType store, final YangInstanceIdentifier path) {
        return FluentFuture.from(delegate.exists(fromMdsal(store), path));
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
