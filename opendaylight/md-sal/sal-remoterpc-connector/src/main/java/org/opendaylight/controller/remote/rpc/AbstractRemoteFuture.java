/*
 * Copyright (c) 2019 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.remote.rpc;

import static java.util.Objects.requireNonNull;

import com.google.common.util.concurrent.AbstractFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

abstract class AbstractRemoteFuture<T, O, E extends Exception> extends AbstractFuture<O> {
    private static final Logger LOG = LoggerFactory.getLogger(AbstractRemoteFuture.class);

    private final @NonNull T type;

    AbstractRemoteFuture(final @NonNull T type, final CompletionStage<Object> requestFuture) {
        this.type = requireNonNull(type);
        requestFuture.whenComplete((reply, error) -> {
            if (error != null) {
                failNow(error);
                return;
            }

            final var result = processReply(reply);
            if (result != null) {
                LOG.debug("Received response for operation {}: result is {}", type, result);
                set(result);
            } else {
                failNow(new IllegalStateException("Incorrect reply type " + reply + " from Akka"));
            }
        });
    }

    @Override
    public final O get() throws InterruptedException, ExecutionException {
        try {
            return super.get();
        } catch (ExecutionException e) {
            throw mapException(e);
        }
    }

    @Override
    public final O get(final long timeout, final TimeUnit unit)
            throws InterruptedException, ExecutionException, TimeoutException {
        try {
            return super.get(timeout, unit);
        } catch (final ExecutionException e) {
            throw mapException(e);
        }
    }

    @Override
    protected final boolean set(final O value) {
        final boolean ret = super.set(value);
        if (ret) {
            LOG.debug("Future {} for action {} successfully completed", this, type);
        }
        return ret;
    }

    final void failNow(final Throwable error) {
        LOG.debug("Failing future {} for operation {}", this, type, error);
        setException(error);
    }

    abstract @Nullable O processReply(Object reply);

    abstract @NonNull Class<E> exceptionClass();

    abstract @NonNull E wrapCause(Throwable cause);

    private ExecutionException mapException(final ExecutionException ex) {
        final Throwable cause = ex.getCause();
        return exceptionClass().isInstance(cause) ? ex : new ExecutionException(ex.getMessage(), wrapCause(cause));
    }
}
