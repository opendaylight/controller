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
import com.google.common.util.concurrent.CheckedFuture;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreReadTransaction;
import org.opendaylight.mdsal.common.api.MappingCheckedFuture;
import org.opendaylight.yangtools.util.concurrent.ExceptionMapper;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;

public class DOMStoreReadTransactionAdapter<T extends org.opendaylight.mdsal.dom.spi.store.DOMStoreReadTransaction>
        extends ForwardingObject implements DOMStoreReadTransaction {
    public static final ExceptionMapper<ReadFailedException> READ_EX_MAPPER =
            new ExceptionMapper<ReadFailedException>("read", ReadFailedException.class) {
        @Override
        protected ReadFailedException newWithCause(final String message, final Throwable cause) {
            return cause instanceof org.opendaylight.mdsal.common.api.ReadFailedException
                    ? new ReadFailedException(cause.getMessage(), cause.getCause())
                            : new ReadFailedException(message, cause);
        }
    };

    private final T delegate;

    public DOMStoreReadTransactionAdapter(final T delegate) {
        this.delegate = requireNonNull(delegate);
    }

    @Override
    protected T delegate() {
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
    public CheckedFuture<com.google.common.base.Optional<NormalizedNode<?, ?>>, ReadFailedException> read(
            final YangInstanceIdentifier path) {
        return MappingCheckedFuture.create(delegate.read(path), READ_EX_MAPPER);
    }

    @Override
    public CheckedFuture<Boolean, ReadFailedException> exists(final YangInstanceIdentifier path) {
        return MappingCheckedFuture.create(delegate.exists(path), READ_EX_MAPPER);
    }
}
