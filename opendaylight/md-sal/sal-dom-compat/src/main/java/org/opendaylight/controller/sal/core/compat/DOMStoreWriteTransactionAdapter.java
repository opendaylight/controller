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
import org.opendaylight.controller.sal.core.spi.data.DOMStoreThreePhaseCommitCohort;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreWriteTransaction;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;

public class DOMStoreWriteTransactionAdapter extends ForwardingObject implements DOMStoreWriteTransaction {
    private final org.opendaylight.mdsal.dom.spi.store.DOMStoreWriteTransaction delegate;

    public DOMStoreWriteTransactionAdapter(
            final org.opendaylight.mdsal.dom.spi.store.DOMStoreWriteTransaction delegate) {
        this.delegate = requireNonNull(delegate);
    }

    @Override
    protected org.opendaylight.mdsal.dom.spi.store.DOMStoreWriteTransaction delegate() {
        return delegate;
    }

    @Override
    public Object getIdentifier() {
        return delegate.getIdentifier();
    }

    @Override
    public void close() {
        delegate.close();
    }

    @Override
    public void write(final YangInstanceIdentifier path, final NormalizedNode<?, ?> data) {
        delegate.write(path, data);
    }

    @Override
    public DOMStoreThreePhaseCommitCohort ready() {
        return new DOMStoreThreePhaseCommitCohortAdapter(delegate.ready());
    }

    @Override
    public void merge(final YangInstanceIdentifier path, final NormalizedNode<?, ?> data) {
        delegate.merge(path, data);
    }

    @Override
    public void delete(final YangInstanceIdentifier path) {
        delegate.delete(path);
    }
}