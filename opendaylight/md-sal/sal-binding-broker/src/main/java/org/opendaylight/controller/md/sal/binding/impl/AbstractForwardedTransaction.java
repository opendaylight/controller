/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.binding.impl;

import org.opendaylight.controller.md.sal.common.api.data.AsyncTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.dom.api.DOMDataReadTransaction;
import org.opendaylight.yangtools.concepts.Delegator;
import org.opendaylight.yangtools.concepts.Identifiable;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.data.api.InstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;


abstract class AbstractForwardedTransaction<T extends AsyncTransaction<InstanceIdentifier, NormalizedNode<?, ?>>>
        implements Delegator<T>, Identifiable<Object> {

    private final T delegate;
    private final BindingToNormalizedNodeCodec codec;

    public AbstractForwardedTransaction(final T delegateTx, final BindingToNormalizedNodeCodec codec) {
        this.delegate = Preconditions.checkNotNull(delegateTx, "Delegate must not be null");
        this.codec = Preconditions.checkNotNull(codec, "Codec must not be null");
    }


    @Override
    public final  Object getIdentifier() {
        return delegate.getIdentifier();
    }

    @Override
    public final  T getDelegate() {
        return delegate;
    }

    @SuppressWarnings("unchecked")
    protected final <S extends AsyncTransaction<InstanceIdentifier, NormalizedNode<?, ?>>> S getDelegateChecked(final Class<S> txType) {
        Preconditions.checkState(txType.isInstance(delegate));
        return (S) delegate;
    }

    protected final BindingToNormalizedNodeCodec getCodec() {
        return codec;
    }

    protected final <T extends DataObject> ListenableFuture<Optional<T>> doRead(final DOMDataReadTransaction readTx,
            final LogicalDatastoreType store, final org.opendaylight.yangtools.yang.binding.InstanceIdentifier<T> path) {
        return Futures.transform(readTx.read(store, codec.toNormalized(path)), codec.deserializeFunction(path));
    }
}
