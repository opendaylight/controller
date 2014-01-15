package org.opendaylight.controller.sal.binding.codegen.impl;

import org.opendaylight.yangtools.yang.binding.RpcService;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker.RoutedRpcRegistration;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker.RpcRegistration;
import org.opendaylight.controller.sal.binding.api.rpc.RpcRouter;
import org.opendaylight.controller.sal.binding.api.rpc.RpcRoutingTable;
import org.opendaylight.yangtools.yang.binding.BaseIdentity;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

import static org.opendaylight.controller.sal.binding.codegen.RuntimeCodeHelper.*;

import java.util.Map;
import java.util.Set;
import java.util.HashMap;

import org.opendaylight.yangtools.yang.binding.DataContainer;
import org.opendaylight.yangtools.yang.binding.RpcImplementation;
import org.opendaylight.controller.md.sal.common.api.routing.MutableRoutingTable;
import org.opendaylight.controller.md.sal.common.api.routing.RouteChange;
import org.opendaylight.controller.md.sal.common.api.routing.RouteChangeListener;
import org.opendaylight.yangtools.concepts.AbstractObjectRegistration;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.concepts.util.ListenerRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;

public class RpcRouterCodegenInstance<T extends RpcService> implements //
        RpcRouter<T>, RouteChangeListener<Class<? extends BaseIdentity>, InstanceIdentifier<?>> {

    private static final Logger LOG = LoggerFactory.getLogger(RpcRouterCodegenInstance.class);

    private T defaultService;

    private final Class<T> serviceType;

    private final T invocationProxy;

    private final Set<Class<? extends BaseIdentity>> contexts;

    private final ListenerRegistry<RouteChangeListener<Class<? extends BaseIdentity>, InstanceIdentifier<?>>> listeners;

    private final Map<Class<? extends BaseIdentity>, RpcRoutingTableImpl<? extends BaseIdentity, T>> routingTables;

    private final String name;

    @SuppressWarnings("unchecked")
    public RpcRouterCodegenInstance(String name,Class<T> type, T routerImpl, Set<Class<? extends BaseIdentity>> contexts,
            Set<Class<? extends DataContainer>> inputs) {
        this.name = name;
        this.listeners = ListenerRegistry.create();
        this.serviceType = type;
        this.invocationProxy = routerImpl;
        this.contexts = ImmutableSet.copyOf(contexts);
        Map<Class<? extends BaseIdentity>, RpcRoutingTableImpl<? extends BaseIdentity, T>> mutableRoutingTables = new HashMap<>();
        for (Class<? extends BaseIdentity> ctx : contexts) {
            RpcRoutingTableImpl<? extends BaseIdentity, T> table = new RpcRoutingTableImpl<>(name,ctx,type);
            
            @SuppressWarnings("rawtypes")
            Map invokerView = table.getRoutes();
            
            setRoutingTable((RpcService) invocationProxy, ctx, invokerView);
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
    public <C extends BaseIdentity> RpcRoutingTable<C, T> getRoutingTable(Class<C> routeContext) {
        return (RpcRoutingTable<C, T>) routingTables.get(routeContext);
    }

    @Override
    public T getDefaultService() {
        return defaultService;
    }

    @Override
    public Set<Class<? extends BaseIdentity>> getContexts() {
        return contexts;
    }

    @Override
    public <L extends RouteChangeListener<Class<? extends BaseIdentity>, InstanceIdentifier<?>>> ListenerRegistration<L> registerRouteChangeListener(
            L listener) {
        return listeners.registerWithType(listener);
    }

    @Override
    public void onRouteChange(RouteChange<Class<? extends BaseIdentity>, InstanceIdentifier<?>> change) {
        for (ListenerRegistration<RouteChangeListener<Class<? extends BaseIdentity>, InstanceIdentifier<?>>> listener : listeners) {
            try {
                listener.getInstance().onRouteChange(change);
            } catch (Exception e) {
                LOG.error("Error occured during invoker listener {}", listener.getInstance(), e);
            }
        }
    }

    @Override
    public T getService(Class<? extends BaseIdentity> context, InstanceIdentifier<?> path) {
        return routingTables.get(context).getRoute(path);
    }

    @Override
    public RoutedRpcRegistration<T> addRoutedRpcImplementation(T service) {
        return new RoutedRpcRegistrationImpl(service);
    }

    @Override
    public RpcRegistration<T> registerDefaultService(T service) {
        // TODO Auto-generated method stub
        return null;
    }

    private class RoutedRpcRegistrationImpl extends AbstractObjectRegistration<T> implements RoutedRpcRegistration<T> {

        public RoutedRpcRegistrationImpl(T instance) {
            super(instance);
        }

        @Override
        public Class<T> getServiceType() {
            return serviceType;
        }

        @Override
        public void registerPath(Class<? extends BaseIdentity> context, InstanceIdentifier<?> path) {
            routingTables.get(context).updateRoute(path, getInstance());
        }

        @Override
        public void unregisterPath(Class<? extends BaseIdentity> context, InstanceIdentifier<?> path) {
            routingTables.get(context).removeRoute(path, getInstance());
        }

        @Override
        public void registerInstance(Class<? extends BaseIdentity> context, InstanceIdentifier<?> instance) {
            registerPath(context, instance);
        }

        @Override
        public void unregisterInstance(Class<? extends BaseIdentity> context, InstanceIdentifier<?> instance) {
            unregisterPath(context, instance);
        }

        @Override
        protected void removeRegistration() {

        }
    }
}
