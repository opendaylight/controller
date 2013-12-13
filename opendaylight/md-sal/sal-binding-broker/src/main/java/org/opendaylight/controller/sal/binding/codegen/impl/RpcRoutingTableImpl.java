package org.opendaylight.controller.sal.binding.codegen.impl;

import org.opendaylight.controller.sal.binding.spi.RpcRoutingTable;
import org.opendaylight.yangtools.yang.binding.BaseIdentity;
import org.opendaylight.yangtools.yang.binding.RpcService;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.controller.md.sal.common.api.routing.RouteChangePublisher;
import org.opendaylight.controller.md.sal.common.api.routing.RouteChangeListener;
import org.opendaylight.controller.md.sal.common.impl.routing.RoutingUtils;
import org.opendaylight.yangtools.concepts.AbstractObjectRegistration;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.concepts.Mutable;

class RpcRoutingTableImpl<C extends BaseIdentity, S extends RpcService> //
implements //
        Mutable, //
        RpcRoutingTable<C, S>, //
        RouteChangePublisher<Class<? extends BaseIdentity>, InstanceIdentifier<?>> {

    private final Class<C> identifier;
    private final ConcurrentMap<InstanceIdentifier<?>, S> routes;
    private final Map<InstanceIdentifier<?>, S> unmodifiableRoutes;

    private RouteChangeListener<Class<? extends BaseIdentity>, InstanceIdentifier<?>> listener;
    private S defaultRoute;

    public RpcRoutingTableImpl(Class<C> identifier) {
        super();
        this.identifier = identifier;
        this.routes = new ConcurrentHashMap<>();
        this.unmodifiableRoutes = Collections.unmodifiableMap(routes);
    }

    @Override
    public void setDefaultRoute(S target) {
        defaultRoute = target;
    }

    @Override
    public S getDefaultRoute() {
        return defaultRoute;
    }

    @Override
    public <L extends RouteChangeListener<Class<? extends BaseIdentity>, InstanceIdentifier<?>>> ListenerRegistration<L> registerRouteChangeListener(
            L listener) {
        return (ListenerRegistration<L>) new SingletonListenerRegistration<L>(listener);
    }
        
    @Override
    public Class<C> getIdentifier() {
        return identifier;
    }

    @Override
    @SuppressWarnings("unchecked")
    public void updateRoute(InstanceIdentifier<?> path, S service) {
        S previous = this.routes.put(path, service);
        @SuppressWarnings("rawtypes")
        RouteChangeListener listenerCapture = listener;
        if (previous == null && listenerCapture != null) {
            listenerCapture.onRouteChange(RoutingUtils.announcementChange(identifier, path));
        }
    }

    
    @Override
    @SuppressWarnings("unchecked")
    public void removeRoute(InstanceIdentifier<?> path) {
        S previous = this.routes.remove(path);
        @SuppressWarnings("rawtypes")
        RouteChangeListener listenerCapture = listener;
        if (previous != null && listenerCapture != null) {
            listenerCapture.onRouteChange(RoutingUtils.removalChange(identifier, path));
        }
    }
    
    public void removeRoute(InstanceIdentifier<?> path, S service) {
        @SuppressWarnings("rawtypes")
        RouteChangeListener listenerCapture = listener;
        if (routes.remove(path, service) && listenerCapture != null) {
            listenerCapture.onRouteChange(RoutingUtils.removalChange(identifier, path));
        }
    }

    @Override
    public S getRoute(InstanceIdentifier<?> nodeInstance) {
        S route = routes.get(nodeInstance);
        if (route != null) {
            return route;
        }
        return getDefaultRoute();
    }

    @Override
    public Map<InstanceIdentifier<?>, S> getRoutes() {
        return unmodifiableRoutes;
    }
    
    protected void removeAllReferences(S service) {
        
    }

    private class SingletonListenerRegistration<L extends RouteChangeListener<Class<? extends BaseIdentity>, InstanceIdentifier<?>>> extends
            AbstractObjectRegistration<L>
            implements ListenerRegistration<L> {

        public SingletonListenerRegistration(L instance) {
            super(instance);
            listener = instance;
        }

        @Override
        protected void removeRegistration() {
            listener = null;
        }
    }
}