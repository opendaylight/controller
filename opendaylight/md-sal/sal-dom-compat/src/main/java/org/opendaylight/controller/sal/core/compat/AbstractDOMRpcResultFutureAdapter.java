/*
 * Copyright (c) 2018 Inocybe Technologies and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.core.compat;

import com.google.common.util.concurrent.AbstractFuture;
import com.google.common.util.concurrent.ListenableFuture;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.opendaylight.mdsal.dom.api.DOMRpcException;
import org.opendaylight.mdsal.dom.api.DOMRpcResult;
import org.opendaylight.yangtools.util.concurrent.ExceptionMapper;

/**
 * Base for a DOMRpcResult future adapter.
 *
 * @author Thomas Pantelis
 */
@SuppressWarnings("checkstyle:ClassTypeParameterName")
public abstract class AbstractDOMRpcResultFutureAdapter<T extends DOMRpcResult, F extends DOMRpcResult,
        D extends ListenableFuture<F>, E extends DOMRpcException> extends AbstractFuture<T> {
    private final D delegate;
    private final ExceptionMapper<E> exMapper;
    private volatile Optional<T> result;

    AbstractDOMRpcResultFutureAdapter(D delegate, ExceptionMapper<E> exMapper) {
        this.delegate = delegate;
        this.exMapper = exMapper;
    }

    protected abstract T transform(F fromResult);

    public D delegate() {
        return delegate;
    }

    @Override
    public void addListener(Runnable listener, Executor executor) {
        delegate.addListener(listener, executor);
    }

    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        return delegate.cancel(mayInterruptIfRunning);
    }

    @Override
    public boolean isCancelled() {
        return delegate.isCancelled();
    }

    @Override
    public boolean isDone() {
        return delegate.isDone();
    }

    @Override
    public T get() throws InterruptedException, ExecutionException {
        if (result != null) {
            return result.orElse(null);
        }

        try {
            return transformIfNecessary(delegate.get());
        } catch (ExecutionException e) {
            throw new ExecutionException(e.getMessage(), exMapper.apply(e));
        }
    }

    @Override
    public T get(final long timeout, final TimeUnit unit) throws InterruptedException, ExecutionException,
            TimeoutException {
        if (result != null) {
            return result.orElse(null);
        }

        try {
            return transformIfNecessary(delegate.get(timeout, unit));
        } catch (ExecutionException e) {
            throw new ExecutionException(e.getMessage(), exMapper.apply(e));
        }
    }

    private synchronized T transformIfNecessary(F delegateResult) {
        if (result == null) {
            if (delegateResult == null) {
                result = Optional.empty();
            } else {
                result = Optional.of(transform(delegateResult));
            }
        }

        return result.orElse(null);
    }
}
