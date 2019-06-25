/*
 * Copyright (c) 2019 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.remote.rpc;

import static java.util.Objects.requireNonNull;

import akka.dispatch.OnComplete;
import com.google.common.util.concurrent.AbstractFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.yangtools.yang.model.api.SchemaPath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.concurrent.ExecutionContext;
import scala.concurrent.Future;

abstract class AbstractRemoteFuture<T> extends AbstractFuture<T> {
    private static final Logger LOG = LoggerFactory.getLogger(AbstractRemoteFuture.class);

    private final @NonNull SchemaPath type;

    AbstractRemoteFuture(final @NonNull SchemaPath type) {
        this.type = requireNonNull(type);
    }

    @Override
    public final T get() throws InterruptedException, ExecutionException {
        try {
            return super.get();
        } catch (ExecutionException e) {
            throw mapException(e);
        }
    }

    @Override
    public final T get(final long timeout, final TimeUnit unit)
            throws InterruptedException, ExecutionException, TimeoutException {
        try {
            return super.get(timeout, unit);
        } catch (final ExecutionException e) {
            throw mapException(e);
        }
    }

    final void completeWith(final Future<Object> future) {
        future.onComplete(newFutureUpdater(), ExecutionContext.Implicits$.MODULE$.global());
    }

    final void failNow(final Throwable error) {
        LOG.debug("Failing future {} for operation {}", this, type, error);
        setException(error);
    }

    abstract ExecutionException mapException(ExecutionException ex);

    abstract AbstractFutureUpdater newFutureUpdater();

    abstract class AbstractFutureUpdater extends OnComplete<Object> {
        @Override
        public final void onComplete(final Throwable error, final Object reply) {
            if (error == null) {
                if (!onComplete(type, reply)) {
                    failNow(new IllegalStateException("Incorrect reply type " + reply + " from Akka"));
                }
            } else {
                failNow(error);
            }
        }

        abstract boolean onComplete(@NonNull SchemaPath type, Object reply);
    }
}
