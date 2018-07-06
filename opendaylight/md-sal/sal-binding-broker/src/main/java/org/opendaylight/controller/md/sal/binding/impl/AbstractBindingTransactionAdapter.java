/*
 * Copyright (c) 2018 Inocybe Technologies and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.binding.impl;

import static java.util.Objects.requireNonNull;

import com.google.common.base.Optional;
import com.google.common.util.concurrent.CheckedFuture;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.mdsal.binding.api.ReadTransaction;
import org.opendaylight.mdsal.common.api.AsyncTransaction;
import org.opendaylight.mdsal.common.api.MappingCheckedFuture;
import org.opendaylight.yangtools.concepts.Delegator;
import org.opendaylight.yangtools.concepts.Identifiable;
import org.opendaylight.yangtools.util.concurrent.ExceptionMapper;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

/**
 * Base class for an md-sal transaction adapter.
 *
 * @author Thomas Pantelis
 */
abstract class AbstractBindingTransactionAdapter<T extends AsyncTransaction<?, ?>>
        implements Delegator<T>, Identifiable<Object> {
    private static final ExceptionMapper<ReadFailedException> READ_EX_MAPPER =
            new ExceptionMapper<ReadFailedException>("read", ReadFailedException.class) {
        @Override
        protected ReadFailedException newWithCause(String message, Throwable cause) {
            if (cause instanceof org.opendaylight.mdsal.common.api.ReadFailedException) {
                return new ReadFailedException(cause.getMessage(), cause.getCause());
            }

            return new ReadFailedException(message, cause);
        }
    };

    private final T delegate;

    AbstractBindingTransactionAdapter(T delegate) {
        this.delegate = requireNonNull(delegate);
    }

    @Override
    public Object getIdentifier() {
        return delegate.getIdentifier();
    }

    @Override
    public T getDelegate() {
        return delegate;
    }

    static <D extends DataObject> CheckedFuture<Optional<D>, ReadFailedException> read(
            final ReadTransaction readTx, final LogicalDatastoreType store, final InstanceIdentifier<D> path) {
        return MappingCheckedFuture.create(readTx.read(convert(store), path), READ_EX_MAPPER);
    }

    static org.opendaylight.mdsal.common.api.LogicalDatastoreType convert(LogicalDatastoreType from) {
        return org.opendaylight.mdsal.common.api.LogicalDatastoreType.valueOf(from.name());
    }
}
