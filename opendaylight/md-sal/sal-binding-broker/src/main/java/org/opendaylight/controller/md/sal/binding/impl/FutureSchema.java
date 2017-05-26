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
import org.opendaylight.mdsal.binding.generator.util.BindingRuntimeContext;
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
        final BindingRuntimeContext localRuntimeContext = this.runtimeContext;
        if(localRuntimeContext != null) {
            return localRuntimeContext;
        }

        if(waitForSchema(Collections.emptyList())) {
            return this.runtimeContext;
        }

        throw new IllegalStateException("No SchemaContext is available");
    }

    void onRuntimeContextUpdated(final BindingRuntimeContext context) {
        synchronized(this.postponedOperations) {
            this.runtimeContext = context;
            for (final FutureSchemaPredicate op : this.postponedOperations) {
                op.unlockIfPossible(context);
            }
        }
    }

    long getDuration() {
        return this.duration;
    }

    TimeUnit getUnit() {
        return this.unit;
    }

    @Override
    public void close() {
        synchronized(this.postponedOperations) {
            for (final FutureSchemaPredicate op : this.postponedOperations) {
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

    private boolean addPostponedOpAndWait(final FutureSchemaPredicate postponedOp) {
        if(!this.waitEnabled) {
            return false;
        }

        final BindingRuntimeContext localRuntimeContext = this.runtimeContext;
        synchronized(this.postponedOperations) {
            this.postponedOperations.add(postponedOp);

            // If the runtimeContext changed, this op may now be satisfied so check it.
            if(localRuntimeContext != this.runtimeContext) {
                postponedOp.unlockIfPossible(this.runtimeContext);
            }
        }

        return postponedOp.waitForSchema();
    }

    private abstract class FutureSchemaPredicate implements Predicate<BindingRuntimeContext> {

        final boolean waitForSchema() {
            try {
                this.schemaPromise.get(FutureSchema.this.duration, FutureSchema.this.unit);
                return true;
            } catch (final InterruptedException | ExecutionException e) {
                throw Throwables.propagate(e);
            } catch (final TimeoutException e) {
                return false;
            } finally {
                synchronized(FutureSchema.this.postponedOperations) {
                    FutureSchema.this.postponedOperations.remove(this);
                }
            }
        }

        final void unlockIfPossible(final BindingRuntimeContext context) {
            if (!this.schemaPromise.isDone() && apply(context)) {
                this.schemaPromise.set(null);
            }
        }

        final void cancel() {
            this.schemaPromise.cancel(true);
        }

        private final SettableFuture<?> schemaPromise = SettableFuture.create();
    }

}
