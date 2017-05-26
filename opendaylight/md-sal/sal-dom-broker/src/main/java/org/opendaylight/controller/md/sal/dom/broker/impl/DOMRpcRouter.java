/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.dom.broker.impl;

import com.google.common.base.Preconditions;
import com.google.common.base.Verify;
import com.google.common.collect.Collections2;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.MapDifference;
import com.google.common.collect.MapDifference.ValueDifference;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.common.util.concurrent.CheckedFuture;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import javax.annotation.concurrent.GuardedBy;
import org.opendaylight.controller.md.sal.dom.api.DOMRpcAvailabilityListener;
import org.opendaylight.controller.md.sal.dom.api.DOMRpcException;
import org.opendaylight.controller.md.sal.dom.api.DOMRpcIdentifier;
import org.opendaylight.controller.md.sal.dom.api.DOMRpcImplementation;
import org.opendaylight.controller.md.sal.dom.api.DOMRpcImplementationRegistration;
import org.opendaylight.controller.md.sal.dom.api.DOMRpcProviderService;
import org.opendaylight.controller.md.sal.dom.api.DOMRpcResult;
import org.opendaylight.controller.md.sal.dom.api.DOMRpcService;
import org.opendaylight.controller.md.sal.dom.spi.AbstractDOMRpcImplementationRegistration;
import org.opendaylight.controller.sal.core.api.model.SchemaService;
import org.opendaylight.yangtools.concepts.AbstractListenerRegistration;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.opendaylight.yangtools.yang.model.api.SchemaContextListener;
import org.opendaylight.yangtools.yang.model.api.SchemaPath;

public final class DOMRpcRouter implements AutoCloseable, DOMRpcService, DOMRpcProviderService, SchemaContextListener {
    private static final ThreadFactory THREAD_FACTORY = new ThreadFactoryBuilder()
            .setNameFormat("DOMRpcRouter-listener-%s").setDaemon(true).build();

    private final ExecutorService listenerNotifier = Executors.newSingleThreadExecutor(THREAD_FACTORY);

    @GuardedBy("this")
    private Collection<Registration<?>> listeners = Collections.emptyList();

    private volatile DOMRpcRoutingTable routingTable = DOMRpcRoutingTable.EMPTY;

    public static DOMRpcRouter newInstance(final SchemaService schemaService) {
        final DOMRpcRouter rpcRouter = new DOMRpcRouter();
        schemaService.registerSchemaContextListener(rpcRouter);
        return rpcRouter;
    }

    @Override
    public <T extends DOMRpcImplementation> DOMRpcImplementationRegistration<T> registerRpcImplementation(
            final T implementation, final DOMRpcIdentifier... rpcs) {
        return registerRpcImplementation(implementation, ImmutableSet.copyOf(rpcs));
    }

    private synchronized void removeRpcImplementation(final DOMRpcImplementation implementation,
            final Set<DOMRpcIdentifier> rpcs) {
        final DOMRpcRoutingTable oldTable = routingTable;
        final DOMRpcRoutingTable newTable = oldTable.remove(implementation, rpcs);
        routingTable = newTable;

        listenerNotifier.execute(() -> notifyRemoved(newTable, implementation));
    }

    @Override
    public synchronized <T extends DOMRpcImplementation> DOMRpcImplementationRegistration<T> registerRpcImplementation(
            final T implementation, final Set<DOMRpcIdentifier> rpcs) {
        final DOMRpcRoutingTable oldTable = routingTable;
        final DOMRpcRoutingTable newTable = oldTable.add(implementation, rpcs);
        routingTable = newTable;

        listenerNotifier.execute(() -> notifyAdded(newTable, implementation));

        return new AbstractDOMRpcImplementationRegistration<T>(implementation) {
            @Override
            protected void removeRegistration() {
                removeRpcImplementation(getInstance(), rpcs);
            }
        };
    }

    @Override
    public CheckedFuture<DOMRpcResult, DOMRpcException> invokeRpc(final SchemaPath type,
            final NormalizedNode<?, ?> input) {
        return routingTable.invokeRpc(type, input);
    }

    private synchronized void removeListener(final ListenerRegistration<? extends DOMRpcAvailabilityListener> reg) {
        listeners = ImmutableList.copyOf(Collections2.filter(listeners, i -> !reg.equals(i)));
    }

    private synchronized void notifyAdded(final DOMRpcRoutingTable newTable, final DOMRpcImplementation impl) {
        for (Registration<?> l : listeners) {
            l.addRpc(newTable, impl);
        }
    }

    private synchronized void notifyRemoved(final DOMRpcRoutingTable newTable, final DOMRpcImplementation impl) {
        for (Registration<?> l : listeners) {
            l.removeRpc(newTable, impl);
        }
    }

    @Override
    public synchronized <T extends DOMRpcAvailabilityListener> ListenerRegistration<T> registerRpcListener(
            final T listener) {
        final Registration<T> ret = new Registration<>(this, listener, routingTable.getRpcs(listener));
        final Builder<Registration<?>> b = ImmutableList.builder();
        b.addAll(listeners);
        b.add(ret);
        listeners = b.build();

        listenerNotifier.execute(() -> ret.initialTable());
        return ret;
    }

    @Override
    public synchronized void onGlobalContextUpdated(final SchemaContext context) {
        final DOMRpcRoutingTable oldTable = routingTable;
        final DOMRpcRoutingTable newTable = oldTable.setSchemaContext(context);
        routingTable = newTable;
    }

    @Override
    public void close() {
        listenerNotifier.shutdown();
    }

    private static final class Registration<T extends DOMRpcAvailabilityListener>
        extends AbstractListenerRegistration<T> {

        private final DOMRpcRouter router;

        private Map<SchemaPath, Set<YangInstanceIdentifier>> prevRpcs;

        Registration(final DOMRpcRouter router, final T listener,
                final Map<SchemaPath, Set<YangInstanceIdentifier>> rpcs) {
            super(Preconditions.checkNotNull(listener));
            this.router = Preconditions.checkNotNull(router);
            this.prevRpcs = Preconditions.checkNotNull(rpcs);
        }

        @Override
        protected void removeRegistration() {
            router.removeListener(this);
        }

        void initialTable() {
            final Collection<DOMRpcIdentifier> added = new ArrayList<>();
            for (Entry<SchemaPath, Set<YangInstanceIdentifier>> e : prevRpcs.entrySet()) {
                added.addAll(Collections2.transform(e.getValue(), i -> DOMRpcIdentifier.create(e.getKey(), i)));
            }

            if (!added.isEmpty()) {
                final T l = getInstance();
                l.onRpcAvailable(added);
            }
        }

        void addRpc(final DOMRpcRoutingTable newTable, final DOMRpcImplementation impl) {
            final T l = getInstance();
            if (!l.acceptsImplementation(impl)) {
                return;
            }

            final Map<SchemaPath, Set<YangInstanceIdentifier>> rpcs = Verify.verifyNotNull(newTable.getRpcs(l));
            final MapDifference<SchemaPath, Set<YangInstanceIdentifier>> diff = Maps.difference(prevRpcs, rpcs);

            final Collection<DOMRpcIdentifier> added = new ArrayList<>();
            for (Entry<SchemaPath, Set<YangInstanceIdentifier>> e : diff.entriesOnlyOnRight().entrySet()) {
                added.addAll(Collections2.transform(e.getValue(), i -> DOMRpcIdentifier.create(e.getKey(), i)));
            }
            for (Entry<SchemaPath, ValueDifference<Set<YangInstanceIdentifier>>> e : diff.entriesDiffering().entrySet()) {
                for (YangInstanceIdentifier i : Sets.difference(e.getValue().rightValue(), e.getValue().leftValue())) {
                    added.add(DOMRpcIdentifier.create(e.getKey(), i));
                }
            }

            prevRpcs = rpcs;
            if (!added.isEmpty()) {
                l.onRpcAvailable(added);
            }
        }

        void removeRpc(final DOMRpcRoutingTable newTable, final DOMRpcImplementation impl) {
            final T l = getInstance();
            if (!l.acceptsImplementation(impl)) {
                return;
            }

            final Map<SchemaPath, Set<YangInstanceIdentifier>> rpcs = Verify.verifyNotNull(newTable.getRpcs(l));
            final MapDifference<SchemaPath, Set<YangInstanceIdentifier>> diff = Maps.difference(prevRpcs, rpcs);

            final Collection<DOMRpcIdentifier> removed = new ArrayList<>();
            for (Entry<SchemaPath, Set<YangInstanceIdentifier>> e : diff.entriesOnlyOnLeft().entrySet()) {
                removed.addAll(Collections2.transform(e.getValue(), i -> DOMRpcIdentifier.create(e.getKey(), i)));
            }
            for (Entry<SchemaPath, ValueDifference<Set<YangInstanceIdentifier>>> e : diff.entriesDiffering().entrySet()) {
                for (YangInstanceIdentifier i : Sets.difference(e.getValue().leftValue(), e.getValue().rightValue())) {
                    removed.add(DOMRpcIdentifier.create(e.getKey(), i));
                }
            }

            prevRpcs = rpcs;
            if (!removed.isEmpty()) {
                l.onRpcUnavailable(removed);
            }
        }
    }
}
