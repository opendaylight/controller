/*
 * Copyright (c) 2014 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.sal.common.util;

import java.util.concurrent.ExecutionException;

import com.google.common.base.Function;
import com.google.common.base.Preconditions;

/**
 * Utility exception mapper which translates an Exception to a specified type of Exception.
 *
 * This mapper is intended to be used with {@link com.google.common.util.concurrent.Futures#makeChecked(com.google.common.util.concurrent.ListenableFuture, Function)}
 * <ul>
 * <li>if exception is the specified type or one of its subclasses, it returns original exception.
 * <li>if exception is {@link ExecutionException} and the cause is of the specified type, it returns the cause
 * <li>otherwise returns an instance of the specified exception type with original exception as the cause.
 * </ul>
 *
 * @author Thomas Pantelis
 *
 * @param <X> the exception type
 */
public abstract class ExceptionMapper<X extends Exception> implements Function<Exception, X> {

    private final String opName;
    private final Class<X> exceptionType;

    public ExceptionMapper(final String opName, final Class<X> exceptionType ) {
        this.opName = Preconditions.checkNotNull( opName );
        this.exceptionType = Preconditions.checkNotNull( exceptionType );
    }

    protected abstract X newWithCause( String message, Throwable cause );

    @SuppressWarnings("unchecked")
    @Override
    public X apply( final Exception e ) {

        // If exception is of the specified type,return it.
        if( exceptionType.isAssignableFrom( e.getClass() ) ) {
            return (X) e;
        }

        // If exception is ExecutionException whose cause is of the specified
        // type, return the cause.
        if( e instanceof ExecutionException && e.getCause() != null ) {
            if( exceptionType.isAssignableFrom( e.getCause().getClass() ) ) {
                return (X) e.getCause();
            } else {
                return newWithCause( opName + " failed", e.getCause() );
            }
        }

        // Otherwise return an instance of the specified type with the original
        // cause.

        if( e instanceof InterruptedException ) {
            return newWithCause( opName + " was interupted.", e );
        }

        return newWithCause( opName + " failed", e );
    }
}
