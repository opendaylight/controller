/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.md.sal.binding.impl;

import com.google.common.base.Predicate;
import com.google.common.base.Throwables;
import com.google.common.util.concurrent.SettableFuture;
import java.net.URI;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import javax.annotation.concurrent.GuardedBy;
import org.opendaylight.yangtools.sal.binding.generator.util.BindingRuntimeContext;
import org.opendaylight.yangtools.yang.binding.Augmentation;

class FutureSchema implements AutoCloseable {

    @GuardedBy(value="postponedOperations")
    private final Set<FutureSchemaPredicate> postponedOperations = new LinkedHashSet<>();
    private final long duration;
    private final TimeUnit unit;
    private final boolean waitEnabled;
    private volatile BindingRuntimeContext runtimeContext;

    protected FutureSchema(final long time, final TimeUnit unit, final boolean waitEnabled) {
        this.duration = time;
        this.unit = unit;
        this.waitEnabled = waitEnabled;
    }

    BindingRuntimeContext runtimeContext() {
        BindingRuntimeContext localRuntimeContext = runtimeContext;
        if(localRuntimeContext != null) {
            return localRuntimeContext;
        }

        if(waitForSchema(Collections.emptyList())) {
            return runtimeContext;
        }

        throw new IllegalStateException("No SchemaContext is available");
    }

    void onRuntimeContextUpdated(final BindingRuntimeContext context) {
        synchronized(postponedOperations) {
            runtimeContext = context;
            for (final FutureSchemaPredicate op : postponedOperations) {
                op.unlockIfPossible(context);
            }
        }
    }

    long getDuration() {
        return duration;
    }

    TimeUnit getUnit() {
        return unit;
    }

    @Override
    public void close() {
        synchronized(postponedOperations) {
            for (final FutureSchemaPredicate op : postponedOperations) {
                op.cancel();
            }
        }
    }

    private static boolean isSchemaAvailable(final Class<?> clz, final BindingRuntimeContext context) {
        final Object schema;
        if (Augmentation.class.isAssignableFrom(clz)) {
            schema = context.getAugmentationDefinition(clz);
        } else {
            schema = context.getSchemaDefinition(clz);
        }
        return schema != null;
    }

    boolean waitForSchema(final URI namespace, final Date revision) {
        return addPostponedOpAndWait(new FutureSchemaPredicate() {
            @Override
            public boolean apply(final BindingRuntimeContext input) {
                return input.getSchemaContext().findModuleByNamespaceAndRevision(namespace, revision) != null;
            }
        });
    }

    boolean waitForSchema(final Collection<Class<?>> bindingClasses) {
        return addPostponedOpAndWait(new FutureSchemaPredicate() {
            @Override
            public boolean apply(final BindingRuntimeContext context) {
                for (final Class<?> clz : bindingClasses) {
                    if (!isSchemaAvailable(clz, context)) {
                        return false;
                    }
                }
                return true;
            }
        });
    }

    private boolean addPostponedOpAndWait(FutureSchemaPredicate postponedOp) {
        if(!waitEnabled) {
            return false;
        }

        BindingRuntimeContext localRuntimeContext = runtimeContext;
        synchronized(postponedOperations) {
            postponedOperations.add(postponedOp);

            // If the runtimeContext changed, this op may now be satisfied so check it.
            if(localRuntimeContext != runtimeContext) {
                postponedOp.unlockIfPossible(runtimeContext);
            }
        }

        return postponedOp.waitForSchema();
    }

    private abstract class FutureSchemaPredicate implements Predicate<BindingRuntimeContext> {

        final boolean waitForSchema() {
            try {
                schemaPromise.get(duration, unit);
                return true;
            } catch (final InterruptedException | ExecutionException e) {
                throw Throwables.propagate(e);
            } catch (final TimeoutException e) {
                return false;
            } finally {
                synchronized(postponedOperations) {
                    postponedOperations.remove(this);
                }
            }
        }

        final void unlockIfPossible(final BindingRuntimeContext context) {
            if (!schemaPromise.isDone() && apply(context)) {
                schemaPromise.set(null);
            }
        }

        final void cancel() {
            schemaPromise.cancel(true);
        }

        private final SettableFuture<?> schemaPromise = SettableFuture.create();
    }

}
