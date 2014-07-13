/*
 * Copyright (c) 2014 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.sal.common.util.concurrent;

import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import com.google.common.base.Function;
import com.google.common.util.concurrent.AbstractCheckedFuture;
import com.google.common.util.concurrent.ListenableFuture;

/**
 * An implementation of CheckedFuture that provides similar behavior for the <code>get</code> methods
 * that the <code>checkedGet</code> methods provide.
 * <p>
 * For {@link CancellationException} and {@link InterruptedException}, the specified exception mapper
 * is invoked to translate them to the checked exception type.
 * <p>
 * For {@link ExecutionException}, the mapper is invoked to translate the cause to the checked exception
 * and a new ExecutionException is thrown with the translated cause.
 *
 * @author Thomas Pantelis
 *
 * @param <V> The result type returned by this Future's get method
 * @param <X> The checked exception type
 */
public class MappingCheckedFuture<V, X extends Exception> extends AbstractCheckedFuture<V, X> {

    private final Function<Exception, X> mapper;

    private MappingCheckedFuture( ListenableFuture<V> delegate, Function<Exception, X> mapper ) {
        super( delegate );
        this.mapper = mapper;
    }

    public static <V, X extends Exception> MappingCheckedFuture<V, X> create(
            ListenableFuture<V> delegate, Function<Exception, X> mapper ) {
        return new MappingCheckedFuture<V, X>( delegate, mapper );
    }

    @Override
    protected X mapException( Exception e ) {
        return mapper.apply( e );
    }

    private ExecutionException wrapInExecutionException( String message, final Exception e ) {
        return new ExecutionException( message, mapException( e ) );
    }

    @Override
    public V get() throws InterruptedException, ExecutionException {
        try {
            return super.get();
        } catch( InterruptedException e ) {
            Thread.currentThread().interrupt();
            throw wrapInExecutionException( "Operation was interrupted", e );
        } catch( CancellationException e ) {
            throw wrapInExecutionException( "Operation was cancelled", e );
        } catch( ExecutionException e ) {
            throw wrapInExecutionException( e.getMessage(), e );
        }
    }

    @Override
    public V get( long timeout, TimeUnit unit )
            throws InterruptedException, ExecutionException, TimeoutException {
        try {
            return super.get( timeout, unit );
        } catch( InterruptedException e ) {
            Thread.currentThread().interrupt();
            throw wrapInExecutionException( "Operation was interrupted", e );
        } catch( CancellationException e ) {
            throw wrapInExecutionException( "Operation was cancelled", e );
        } catch( ExecutionException e ) {
            throw wrapInExecutionException( e.getMessage(), e );
        }
    }
}
