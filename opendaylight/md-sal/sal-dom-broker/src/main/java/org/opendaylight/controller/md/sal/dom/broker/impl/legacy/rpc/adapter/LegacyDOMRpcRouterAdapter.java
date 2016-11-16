/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.dom.broker.impl.legacy.rpc.adapter;

import com.google.common.base.Function;
import com.google.common.collect.ImmutableSet;
import com.google.common.util.concurrent.CheckedFuture;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import java.util.Collection;
import java.util.Set;
import org.opendaylight.controller.md.sal.dom.api.DOMRpcAvailabilityListener;
import org.opendaylight.controller.md.sal.dom.api.DOMRpcException;
import org.opendaylight.controller.md.sal.dom.api.DOMRpcIdentifier;
import org.opendaylight.controller.md.sal.dom.api.DOMRpcImplementation;
import org.opendaylight.controller.md.sal.dom.api.DOMRpcImplementationNotAvailableException;
import org.opendaylight.controller.md.sal.dom.api.DOMRpcImplementationRegistration;
import org.opendaylight.controller.md.sal.dom.api.DOMRpcProviderService;
import org.opendaylight.controller.md.sal.dom.api.DOMRpcResult;
import org.opendaylight.controller.md.sal.dom.api.DOMRpcService;
import org.opendaylight.controller.md.sal.dom.spi.AbstractDOMRpcImplementationRegistration;
import org.opendaylight.controller.sal.core.api.model.SchemaService;
import org.opendaylight.mdsal.dom.broker.DOMRpcRouter;
import org.opendaylight.yangtools.concepts.AbstractListenerRegistration;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.common.RpcError;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.opendaylight.yangtools.yang.model.api.SchemaContextListener;
import org.opendaylight.yangtools.yang.model.api.SchemaPath;

public class LegacyDOMRpcRouterAdapter
        implements AutoCloseable, DOMRpcService, DOMRpcProviderService, SchemaContextListener {

    private final DOMRpcRouter delegator;

    private LegacyDOMRpcRouterAdapter(DOMRpcRouter delegateRouter) {
        this.delegator = delegateRouter;
    }

    public static LegacyDOMRpcRouterAdapter newInstance(SchemaService schemaService) {
        DOMRpcRouter delegateRouter = new DOMRpcRouter();
        final LegacyDOMRpcRouterAdapter rpcRouter = new LegacyDOMRpcRouterAdapter(delegateRouter);
        schemaService.registerSchemaContextListener(rpcRouter);
        return rpcRouter;
    }

    @Override
    public <T extends DOMRpcImplementation> DOMRpcImplementationRegistration<T> registerRpcImplementation(
            T implementation, DOMRpcIdentifier... rpcs) {
        return registerRpcImplementation(implementation, ImmutableSet.copyOf(rpcs));
    }

    @Override
    public <T extends DOMRpcImplementation> DOMRpcImplementationRegistration<T> registerRpcImplementation(
            T implementation, Set<DOMRpcIdentifier> rpcs) {

        DOMRpcImplementationAdapter<T> implAdapter = new DOMRpcImplementationAdapter<>(implementation, rpcs);

        org.opendaylight.mdsal.dom.api.DOMRpcImplementationRegistration<DOMRpcImplementationAdapter<T>> reg = delegator
                .registerRpcImplementation(implAdapter, implAdapter.getRpcs());

        return new AbstractDOMRpcImplementationRegistration<T>(implementation) {
            @Override
            protected void removeRegistration() {
                reg.close();
            }
        };
    }

    @Override
    public CheckedFuture<DOMRpcResult, DOMRpcException> invokeRpc(SchemaPath type, NormalizedNode<?, ?> input) {
        
        CheckedFuture<org.opendaylight.mdsal.dom.api.DOMRpcResult, org.opendaylight.mdsal.dom.api.DOMRpcException> delegFuture = delegator
                .invokeRpc(type, input);

        ListenableFuture<DOMRpcResult> transformFuture = Futures.transform(delegFuture,
                (Function<? super org.opendaylight.mdsal.dom.api.DOMRpcResult, DOMRpcResult>) result -> new DOMRpcResult() {

                    @Override
                    public NormalizedNode<?, ?> getResult() {
                        return result.getResult();
                    }

                    @Override
                    public Collection<RpcError> getErrors() {
                        return result.getErrors();
                    }
                });


        return Futures.makeChecked(transformFuture, result -> {
            org.opendaylight.mdsal.dom.api.DOMRpcException exception = (org.opendaylight.mdsal.dom.api.DOMRpcException) result
                    .getCause();
            return new DOMRpcImplementationNotAvailableException(exception.getMessage());
        });
    }

    @Override
    public <T extends DOMRpcAvailabilityListener> ListenerRegistration<T> registerRpcListener(T listener) {

        DOMRpcListenerAdapter<T> listAdapter = new DOMRpcListenerAdapter<>(listener);
        ListenerRegistration<DOMRpcListenerAdapter<T>> listReg = delegator.registerRpcListener(listAdapter);
        
        return new AbstractListenerRegistration<T>(listener) {
            @Override
            protected void removeRegistration() {
                listReg.close();
            }
        };
    }

    @Override
    public synchronized void onGlobalContextUpdated(SchemaContext context) {
        this.delegator.onGlobalContextUpdated(context);
    }

    @Override
    public void close() throws Exception {
        delegator.close();
    }
}
