/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.dom.broker.impl;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;
import com.google.common.collect.ImmutableSet;
import com.google.common.util.concurrent.CheckedFuture;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
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
import org.opendaylight.yangtools.concepts.AbstractListenerRegistration;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.opendaylight.yangtools.yang.model.api.SchemaContextListener;
import org.opendaylight.yangtools.yang.model.api.SchemaPath;

public final class DOMRpcRouter implements AutoCloseable, DOMRpcService, DOMRpcProviderService, SchemaContextListener {
    private static final ThreadFactory THREAD_FACTORY = new ThreadFactoryBuilder().setNameFormat("DOMRpcRouter-listener-%s").setDaemon(true).build();
    private final ExecutorService listenerNotifier = Executors.newSingleThreadExecutor(THREAD_FACTORY);
    @GuardedBy("this")
    private Collection<ListenerRegistration<? extends DOMRpcAvailabilityListener>> listeners = Collections.emptyList();
    private volatile DOMRpcRoutingTable routingTable = DOMRpcRoutingTable.EMPTY;

    @Override
    public <T extends DOMRpcImplementation> DOMRpcImplementationRegistration<T> registerRpcImplementation(final T implementation, final DOMRpcIdentifier... rpcs) {
        return registerRpcImplementation(implementation, ImmutableSet.copyOf(rpcs));
    }

    private static Collection<DOMRpcIdentifier> notPresentRpcs(final DOMRpcRoutingTable table, final Collection<DOMRpcIdentifier> candidates) {
        return ImmutableSet.copyOf(Collections2.filter(candidates, new Predicate<DOMRpcIdentifier>() {
            @Override
            public boolean apply(final DOMRpcIdentifier input) {
                return !table.contains(input);
            }
        }));
    }

    private synchronized void removeRpcImplementation(final DOMRpcImplementation implementation, final Set<DOMRpcIdentifier> rpcs) {
        final DOMRpcRoutingTable oldTable = routingTable;
        final DOMRpcRoutingTable newTable = oldTable.remove(implementation, rpcs);

        final Collection<DOMRpcIdentifier> removedRpcs = notPresentRpcs(newTable, rpcs);
        routingTable = newTable;
        if(!removedRpcs.isEmpty()) {
            final Collection<ListenerRegistration<? extends DOMRpcAvailabilityListener>> capturedListeners = listeners;
            listenerNotifier.execute(new Runnable() {
                @Override
                public void run() {
                    for (final ListenerRegistration<? extends DOMRpcAvailabilityListener> l : capturedListeners) {
                        // Need to ensure removed listeners do not get notified
                        synchronized (DOMRpcRouter.this) {
                            if (listeners.contains(l)) {
                                l.getInstance().onRpcUnavailable(removedRpcs);
                            }
                        }
                    }
                }
            });
        }
    }

    @Override
    public synchronized <T extends DOMRpcImplementation> DOMRpcImplementationRegistration<T> registerRpcImplementation(final T implementation, final Set<DOMRpcIdentifier> rpcs) {
        final DOMRpcRoutingTable oldTable = routingTable;
        final DOMRpcRoutingTable newTable = oldTable.add(implementation, rpcs);

        final Collection<DOMRpcIdentifier> addedRpcs = notPresentRpcs(oldTable, rpcs);
        routingTable = newTable;

        if(!addedRpcs.isEmpty()) {
            final Collection<ListenerRegistration<? extends DOMRpcAvailabilityListener>> capturedListeners = listeners;
            listenerNotifier.execute(new Runnable() {
                @Override
                public void run() {
                    for (final ListenerRegistration<? extends DOMRpcAvailabilityListener> l : capturedListeners) {
                        // Need to ensure removed listeners do not get notified
                        synchronized (DOMRpcRouter.this) {
                            if (listeners.contains(l)) {
                                l.getInstance().onRpcAvailable(addedRpcs);
                            }
                        }
                    }
                }
            });
        }

        return new AbstractDOMRpcImplementationRegistration<T>(implementation) {
            @Override
            protected void removeRegistration() {
                removeRpcImplementation(getInstance(), rpcs);
            }
        };
    }

    @Override
    public CheckedFuture<DOMRpcResult, DOMRpcException> invokeRpc(final SchemaPath type, final NormalizedNode<?, ?> input) {
        return routingTable.invokeRpc(type, input);
    }

    private synchronized void removeListener(final ListenerRegistration<? extends DOMRpcAvailabilityListener> reg) {
        listeners = ImmutableList.copyOf(Collections2.filter(listeners, new Predicate<Object>() {
            @Override
            public boolean apply(final Object input) {
                return !reg.equals(input);
            }
        }));
    }

    @Override
    public synchronized <T extends DOMRpcAvailabilityListener> ListenerRegistration<T> registerRpcListener(final T listener) {
        final ListenerRegistration<T> ret = new AbstractListenerRegistration<T>(listener) {
            @Override
            protected void removeRegistration() {
                removeListener(this);
            }
        };

        final Builder<ListenerRegistration<? extends DOMRpcAvailabilityListener>> b = ImmutableList.builder();
        b.addAll(listeners);
        b.add(ret);
        listeners = b.build();
        final Map<SchemaPath, Set<YangInstanceIdentifier>> capturedRpcs = routingTable.getRpcs();

        listenerNotifier.execute(new Runnable() {
            @Override
            public void run() {
                for (final Entry<SchemaPath, Set<YangInstanceIdentifier>> e : capturedRpcs.entrySet()) {
                    listener.onRpcAvailable(Collections2.transform(e.getValue(), new Function<YangInstanceIdentifier, DOMRpcIdentifier>() {
                        @Override
                        public DOMRpcIdentifier apply(final YangInstanceIdentifier input) {
                            return DOMRpcIdentifier.create(e.getKey(), input);
                        }
                    }));
                }
            }
        });

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

}
