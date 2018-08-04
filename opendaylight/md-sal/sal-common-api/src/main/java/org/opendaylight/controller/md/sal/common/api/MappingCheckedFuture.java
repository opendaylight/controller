/*
 * Copyright (c) 2014 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.common.api;

import static java.util.Objects.requireNonNull;

import com.google.common.util.concurrent.AbstractCheckedFuture;
import com.google.common.util.concurrent.ListenableFuture;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Function;
import javax.annotation.Nonnull;

/**
 * An implementation of CheckedFuture that provides similar behavior for the <code>get</code> methods
 * that the <code>checkedGet</code> methods provide.
 *
 * <p>For {@link CancellationException} and {@link InterruptedException}, the specified exception mapper
 * is invoked to translate them to the checked exception type.
 *
 * <p>For {@link ExecutionException}, the mapper is invoked to translate the cause to the checked exception
 * and a new ExecutionException is thrown with the translated cause.
 *
 * @author Thomas Pantelis
 *
 * @param <V> The result type returned by this Future's get method
 * @param <X> The checked exception type
 */
public final class MappingCheckedFuture<V, X extends Exception> extends AbstractCheckedFuture<V, X> {
    private final Function<Exception, X> mapper;

    private MappingCheckedFuture(final ListenableFuture<V> delegate, final Function<Exception, X> mapper) {
        super(delegate);
        this.mapper = requireNonNull(mapper);
    }

    /**
     * Creates a new <code>MappingCheckedFuture</code> that wraps the given {@link ListenableFuture}
     * delegate.
     *
     * @param delegate the {@link ListenableFuture} to wrap
     * @param mapper the mapping {@link Function} used to translate exceptions from the delegate
     * @return a new <code>MappingCheckedFuture</code>
     */
    public static <V, X extends Exception> MappingCheckedFuture<V, X> create(
            final ListenableFuture<V> delegate, final Function<Exception, X> mapper) {
        return new MappingCheckedFuture<>(delegate, mapper);
    }

    @Override
    @SuppressWarnings("checkstyle:parameterName")
    protected X mapException(@Nonnull final Exception e) {
        return mapper.apply(e);
    }

    private ExecutionException wrapInExecutionException(final String message, final Exception ex) {
        return new ExecutionException(message, mapException(ex));
    }

    @Override
    public V get() throws InterruptedException, ExecutionException {
        try {
            return super.get();
        } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
            throw wrapInExecutionException("Operation was interrupted", e);
        } catch (final CancellationException e) {
            throw wrapInExecutionException("Operation was cancelled", e);
        } catch (final ExecutionException e) {
            throw wrapInExecutionException(e.getMessage(), e);
        }
    }

    @Override
    public V get(final long timeout, @Nonnull final TimeUnit unit)
            throws InterruptedException, ExecutionException, TimeoutException {
        try {
            return super.get(timeout, unit);
        } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
            throw wrapInExecutionException("Operation was interrupted", e);
        } catch (final CancellationException e) {
            throw wrapInExecutionException("Operation was cancelled", e);
        } catch (final ExecutionException e) {
            throw wrapInExecutionException(e.getMessage(), e);
        }
    }
}
