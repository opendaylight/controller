/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.binding.impl;

import static java.util.Objects.requireNonNull;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.CheckedFuture;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.MoreExecutors;
import org.opendaylight.controller.md.sal.common.api.MappingCheckedFuture;
import org.opendaylight.controller.md.sal.common.api.data.AsyncTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.controller.md.sal.dom.api.DOMDataReadTransaction;
import org.opendaylight.yangtools.concepts.Delegator;
import org.opendaylight.yangtools.concepts.Identifiable;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;

@Deprecated
abstract class AbstractForwardedTransaction<T extends AsyncTransaction<YangInstanceIdentifier, NormalizedNode<?, ?>>>
        implements Delegator<T>, Identifiable<Object> {

    private final T delegate;
    private final BindingToNormalizedNodeCodec codec;

    AbstractForwardedTransaction(final T delegateTx, final BindingToNormalizedNodeCodec codec) {
        this.delegate = requireNonNull(delegateTx, "Delegate must not be null");
        this.codec = requireNonNull(codec, "Codec must not be null");
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
    protected final <S extends AsyncTransaction<YangInstanceIdentifier, NormalizedNode<?, ?>>>
            S getDelegateChecked(final Class<S> txType) {
        Preconditions.checkState(txType.isInstance(delegate));
        return (S) delegate;
    }

    protected final BindingToNormalizedNodeCodec getCodec() {
        return codec;
    }

    protected final <D extends DataObject> CheckedFuture<Optional<D>, ReadFailedException> doRead(
            final DOMDataReadTransaction readTx, final LogicalDatastoreType store,
            final InstanceIdentifier<D> path) {
        Preconditions.checkArgument(!path.isWildcarded(), "Invalid read of wildcarded path %s", path);

        return MappingCheckedFuture.create(
            Futures.transform(readTx.read(store, codec.toYangInstanceIdentifierBlocking(path)),
                result -> Optional.fromJavaUtil(codec.deserializeFunction(path).apply(result.toJavaUtil())),
                MoreExecutors.directExecutor()),
            ReadFailedException.MAPPER);
    }

    protected final CheckedFuture<Boolean, ReadFailedException> doExists(
            final DOMDataReadTransaction readTx, final LogicalDatastoreType store,
            final InstanceIdentifier<?> path) {
        Preconditions.checkArgument(!path.isWildcarded(), "Invalid read of wildcarded path %s", path);

        return MappingCheckedFuture.create(readTx.exists(store, codec.toYangInstanceIdentifierBlocking(path)),
            ReadFailedException.MAPPER);
    }
}
