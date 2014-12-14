/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.binding.codegen.impl;

import static org.opendaylight.controller.sal.binding.codegen.RuntimeCodeHelper.setRoutingTable;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import javax.annotation.concurrent.GuardedBy;
import org.opendaylight.controller.md.sal.common.api.routing.RouteChange;
import org.opendaylight.controller.md.sal.common.api.routing.RouteChangeListener;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker.RoutedRpcRegistration;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker.RpcRegistration;
import org.opendaylight.controller.sal.binding.api.rpc.RpcRouter;
import org.opendaylight.controller.sal.binding.api.rpc.RpcRoutingTable;
import org.opendaylight.controller.sal.binding.codegen.RuntimeCodeHelper;
import org.opendaylight.yangtools.concepts.AbstractObjectRegistration;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.util.ListenerRegistry;
import org.opendaylight.yangtools.yang.binding.BaseIdentity;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.binding.RpcService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RpcRouterCodegenInstance<T extends RpcService> implements //
RpcRouter<T>, RouteChangeListener<Class<? extends BaseIdentity>, InstanceIdentifier<?>> {

    private static final Logger LOG = LoggerFactory.getLogger(RpcRouterCodegenInstance.class);

    private final Class<T> serviceType;

    private final T invocationProxy;

    private final Set<Class<? extends BaseIdentity>> contexts;

    private final ListenerRegistry<RouteChangeListener<Class<? extends BaseIdentity>, InstanceIdentifier<?>>> listeners;

    private final Map<Class<? extends BaseIdentity>, RpcRoutingTableImpl<? extends BaseIdentity, T>> routingTables;

    @SuppressWarnings("unchecked")
    public RpcRouterCodegenInstance(final String name,final Class<T> type, final T routerImpl, final Iterable<Class<? extends BaseIdentity>> contexts) {
        this.listeners = ListenerRegistry.create();
        this.serviceType = type;
        this.invocationProxy = routerImpl;
        this.contexts = ImmutableSet.copyOf(contexts);
        Map<Class<? extends BaseIdentity>, RpcRoutingTableImpl<? extends BaseIdentity, T>> mutableRoutingTables = new HashMap<>();
        for (Class<? extends BaseIdentity> ctx : contexts) {
            RpcRoutingTableImpl<? extends BaseIdentity, T> table = new RpcRoutingTableImpl<>(name,ctx,type);

            @SuppressWarnings("rawtypes")
            Map invokerView = table.getRoutes();

            setRoutingTable(invocationProxy, ctx, invokerView);
            mutableRoutingTables.put(ctx, table);
            table.registerRouteChangeListener(this);
        }
        this.routingTables = ImmutableMap.copyOf(mutableRoutingTables);
    }

    @Override
    public Class<T> getServiceType() {
        return serviceType;
    }

    @Override
    public T getInvocationProxy() {
        return invocationProxy;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <C extends BaseIdentity> RpcRoutingTable<C, T> getRoutingTable(final Class<C> routeContext) {
        return (RpcRoutingTable<C, T>) routingTables.get(routeContext);
    }

    @Override
    public T getDefaultService() {
        return RuntimeCodeHelper.getDelegate(invocationProxy);
    }

    @Override
    public Set<Class<? extends BaseIdentity>> getContexts() {
        return contexts;
    }

    @Override
    public <L extends RouteChangeListener<Class<? extends BaseIdentity>, InstanceIdentifier<?>>> ListenerRegistration<L> registerRouteChangeListener(
            final L listener) {
        return listeners.registerWithType(listener);
    }

    @Override
    public void onRouteChange(final RouteChange<Class<? extends BaseIdentity>, InstanceIdentifier<?>> change) {
        for (ListenerRegistration<RouteChangeListener<Class<? extends BaseIdentity>, InstanceIdentifier<?>>> listener : listeners) {
            try {
                listener.getInstance().onRouteChange(change);
            } catch (Exception e) {
                LOG.error("Error occured during invoker listener {}", listener.getInstance(), e);
            }
        }
    }

    @Override
    public T getService(final Class<? extends BaseIdentity> context, final InstanceIdentifier<?> path) {
        return routingTables.get(context).getRoute(path);
    }

    @Override
    public RoutedRpcRegistration<T> addRoutedRpcImplementation(final T service) {
        return new RoutedRpcRegistrationImpl(service);
    }

    public void removeDefaultImplementation(final T instance) {
        RpcService current = RuntimeCodeHelper.getDelegate(invocationProxy);
        if(instance == current) {
            RuntimeCodeHelper.setDelegate(invocationProxy, null);
        }
    }

    @Override
    public RpcRegistration<T> registerDefaultService(final T service) {
        RuntimeCodeHelper.setDelegate(invocationProxy, service);
        return new DefaultRpcImplementationRegistration(service);
    }

    private final class RoutedRpcRegistrationImpl extends AbstractObjectRegistration<T> implements RoutedRpcRegistration<T> {
        /*
         * FIXME: retaining this collection is not completely efficient. We really should be storing
         *        a reference to this registration, as a particular listener may be registered multiple
         *        times -- and then this goes kaboom in various aspects.
         */
        @GuardedBy("this")
        private final Collection<Class<? extends BaseIdentity>> contexts = new ArrayList<>(1);

        public RoutedRpcRegistrationImpl(final T instance) {
            super(instance);
        }

        @Override
        public Class<T> getServiceType() {
            return serviceType;
        }

        @Override
        public synchronized void registerPath(final Class<? extends BaseIdentity> context, final InstanceIdentifier<?> path) {
            if (isClosed()) {
                LOG.debug("Closed registration of {} ignoring new path {}", getInstance(), path);
                return;
            }

            routingTables.get(context).updateRoute(path, getInstance());
            contexts.add(context);
        }

        @Override
        public synchronized void unregisterPath(final Class<? extends BaseIdentity> context, final InstanceIdentifier<?> path) {
            if (isClosed()) {
                LOG.debug("Closed unregistration of {} ignoring new path {}", getInstance(), path);
                return;
            }

            routingTables.get(context).removeRoute(path, getInstance());
            contexts.remove(context);
        }

        @Deprecated
        @Override
        public void registerInstance(final Class<? extends BaseIdentity> context, final InstanceIdentifier<?> instance) {
            registerPath(context, instance);
        }

        @Deprecated
        @Override
        public void unregisterInstance(final Class<? extends BaseIdentity> context, final InstanceIdentifier<?> instance) {
            unregisterPath(context, instance);
        }

        @Override
        protected synchronized void removeRegistration() {
            for (Class<? extends BaseIdentity> ctx : contexts) {
                routingTables.get(ctx).removeAllReferences(getInstance());
            }
            contexts.clear();
        }
    }

    private final class DefaultRpcImplementationRegistration extends AbstractObjectRegistration<T> implements RpcRegistration<T> {


        protected DefaultRpcImplementationRegistration(final T instance) {
            super(instance);
        }

        @Override
        protected void removeRegistration() {
            removeDefaultImplementation(this.getInstance());
        }

        @Override
        public Class<T> getServiceType() {
            return serviceType;
        }
    }


}
