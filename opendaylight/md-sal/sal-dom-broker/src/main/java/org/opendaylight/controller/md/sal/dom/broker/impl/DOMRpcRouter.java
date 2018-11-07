/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.dom.broker.impl;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableSet;
import com.google.common.util.concurrent.CheckedFuture;
import com.google.common.util.concurrent.FluentFuture;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import org.opendaylight.controller.md.sal.dom.api.DOMRpcAvailabilityListener;
import org.opendaylight.controller.md.sal.dom.api.DOMRpcException;
import org.opendaylight.controller.md.sal.dom.api.DOMRpcIdentifier;
import org.opendaylight.controller.md.sal.dom.api.DOMRpcImplementation;
import org.opendaylight.controller.md.sal.dom.api.DOMRpcImplementationRegistration;
import org.opendaylight.controller.md.sal.dom.api.DOMRpcProviderService;
import org.opendaylight.controller.md.sal.dom.api.DOMRpcResult;
import org.opendaylight.controller.md.sal.dom.api.DOMRpcService;
import org.opendaylight.controller.md.sal.dom.spi.AbstractDOMRpcImplementationRegistration;
import org.opendaylight.controller.sal.core.compat.DOMRpcServiceAdapter;
import org.opendaylight.controller.sal.core.compat.LegacyDOMRpcResultFutureAdapter;
import org.opendaylight.controller.sal.core.compat.MdsalDOMRpcResultFutureAdapter;
import org.opendaylight.controller.sal.core.compat.RpcAvailabilityListenerAdapter;
import org.opendaylight.yangtools.concepts.AbstractListenerRegistration;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.opendaylight.yangtools.yang.model.api.SchemaContextListener;
import org.opendaylight.yangtools.yang.model.api.SchemaPath;

public final class DOMRpcRouter implements AutoCloseable, DOMRpcService, DOMRpcProviderService, SchemaContextListener {
    // This mapping is used to translate mdsal DOMRpcImplementations to their corresponding legacy
    // DOMRpcImplementations registered thru this interface when invoking a DOMRpcAvailabilityListener.
    private final Map<org.opendaylight.mdsal.dom.api.DOMRpcImplementation, DOMRpcImplementation> implMapping =
            Collections.synchronizedMap(new WeakHashMap<>());

    private final org.opendaylight.mdsal.dom.api.DOMRpcService delegateRpcService;
    private final org.opendaylight.mdsal.dom.api.DOMRpcProviderService delegateRpcProviderService;

    // Note - this is only used for backward compatibility for UTs that use the empty constructor which creates
    // a local mdsal DOMRpcRouter that needs to be updated with the SchemaContext. In production, the mdsal API
    // services are passed via the constructor and are set up externally with the SchemaContext.
    private final SchemaContextListener delegateSchemaContextListener;

    @VisibleForTesting
    public DOMRpcRouter() {
        org.opendaylight.mdsal.dom.broker.DOMRpcRouter delegate = new org.opendaylight.mdsal.dom.broker.DOMRpcRouter();
        this.delegateRpcService = delegate.getRpcService();
        this.delegateRpcProviderService = delegate.getRpcProviderService();
        this.delegateSchemaContextListener = delegate;
    }

    public DOMRpcRouter(final org.opendaylight.mdsal.dom.api.DOMRpcService delegateRpcService,
            final org.opendaylight.mdsal.dom.api.DOMRpcProviderService delegateRpcProviderService) {
        this.delegateRpcService = delegateRpcService;
        this.delegateRpcProviderService = delegateRpcProviderService;
        this.delegateSchemaContextListener = null;
    }

    @Override
    public <T extends DOMRpcImplementation> DOMRpcImplementationRegistration<T> registerRpcImplementation(
            final T implementation, final DOMRpcIdentifier... rpcs) {
        return registerRpcImplementation(implementation, ImmutableSet.copyOf(rpcs));
    }

    @Override
    public synchronized <T extends DOMRpcImplementation> DOMRpcImplementationRegistration<T> registerRpcImplementation(
            final T implementation, final Set<DOMRpcIdentifier> rpcs) {
        org.opendaylight.mdsal.dom.api.DOMRpcImplementation delegateImpl =
            new org.opendaylight.mdsal.dom.api.DOMRpcImplementation() {
                @Override
                public FluentFuture<org.opendaylight.mdsal.dom.api.DOMRpcResult> invokeRpc(
                        final org.opendaylight.mdsal.dom.api.DOMRpcIdentifier rpc, final NormalizedNode<?, ?> input) {
                    return new MdsalDOMRpcResultFutureAdapter(implementation.invokeRpc(DOMRpcIdentifier.fromMdsal(rpc),
                        input));
                }


                @Override
                public long invocationCost() {
                    return implementation.invocationCost();
                }
            };

        implMapping.put(delegateImpl, implementation);

        final org.opendaylight.mdsal.dom.api.DOMRpcImplementationRegistration
            <org.opendaylight.mdsal.dom.api.DOMRpcImplementation> reg = delegateRpcProviderService
                .registerRpcImplementation(delegateImpl, DOMRpcServiceAdapter.convert(rpcs));

        return new AbstractDOMRpcImplementationRegistration<T>(implementation) {
            @Override
            protected void removeRegistration() {
                reg.close();
                implMapping.remove(delegateImpl);
            }
        };
    }

    @Override
    public CheckedFuture<DOMRpcResult, DOMRpcException> invokeRpc(final SchemaPath type,
                                                                  final NormalizedNode<?, ?> input) {
        final FluentFuture<org.opendaylight.mdsal.dom.api.DOMRpcResult> future =
                delegateRpcService.invokeRpc(type, input);
        return future instanceof MdsalDOMRpcResultFutureAdapter ? ((MdsalDOMRpcResultFutureAdapter)future).delegate()
                : new LegacyDOMRpcResultFutureAdapter(future);
    }

    @Override
    public synchronized <T extends DOMRpcAvailabilityListener> ListenerRegistration<T> registerRpcListener(
            final T listener) {
        final ListenerRegistration<org.opendaylight.mdsal.dom.api.DOMRpcAvailabilityListener> reg =
            delegateRpcService.registerRpcListener(new RpcAvailabilityListenerAdapter<T>(listener) {
                @Override
                public boolean acceptsImplementation(final org.opendaylight.mdsal.dom.api.DOMRpcImplementation impl) {
                    // If the DOMRpcImplementation wasn't registered thru this interface then the mapping won't be
                    // present - in this we can't call the listener so just assume acceptance which is the default
                    // behavior. This should be fine since a legacy listener would not be aware of implementation types
                    // registered via the new mdsal API.
                    final DOMRpcImplementation legacyImpl = implMapping.get(impl);
                    return legacyImpl != null ? delegate().acceptsImplementation(legacyImpl) : true;
                }
            });

        return new AbstractListenerRegistration<T>(listener) {
            @Override
            protected void removeRegistration() {
                reg.close();
            }
        };
    }

    @Override
    public void close() {
    }

    @Override
    @VisibleForTesting
    public void onGlobalContextUpdated(final SchemaContext context) {
        if (delegateSchemaContextListener != null) {
            delegateSchemaContextListener.onGlobalContextUpdated(context);
        }
    }
}
