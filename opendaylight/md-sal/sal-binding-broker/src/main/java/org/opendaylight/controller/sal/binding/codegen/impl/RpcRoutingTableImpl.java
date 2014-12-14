/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.binding.codegen.impl;

import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.opendaylight.controller.md.sal.common.api.routing.RouteChangeListener;
import org.opendaylight.controller.md.sal.common.api.routing.RouteChangePublisher;
import org.opendaylight.controller.md.sal.common.impl.routing.RoutingUtils;
import org.opendaylight.controller.sal.binding.api.rpc.RpcRoutingTable;
import org.opendaylight.yangtools.concepts.AbstractObjectRegistration;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.concepts.Mutable;
import org.opendaylight.yangtools.yang.binding.BaseIdentity;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.binding.RpcService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class RpcRoutingTableImpl<C extends BaseIdentity, S extends RpcService> implements
        Mutable, //
        RpcRoutingTable<C, S>, //
        RouteChangePublisher<Class<? extends BaseIdentity>, InstanceIdentifier<?>> {

    private static final Logger LOGGER = LoggerFactory.getLogger(RpcRoutingTableImpl.class);
    private final String routerName;
    private final Class<S> serviceType;

    private final Class<C> contextType;
    private final ConcurrentMap<InstanceIdentifier<?>, S> routes;
    private final Map<InstanceIdentifier<?>, S> unmodifiableRoutes;

    private RouteChangeListener<Class<? extends BaseIdentity>, InstanceIdentifier<?>> listener;
    private S defaultRoute;

    public RpcRoutingTableImpl(final String routerName,final Class<C> contextType, final Class<S> serviceType) {
        super();
        this.routerName = routerName;
        this.serviceType = serviceType;
        this.contextType = contextType;
        this.routes = new ConcurrentHashMap<>();
        this.unmodifiableRoutes = Collections.unmodifiableMap(routes);
    }

    @Override
    public void setDefaultRoute(final S target) {
        defaultRoute = target;
    }

    @Override
    public S getDefaultRoute() {
        return defaultRoute;
    }

    @Override
    public <L extends RouteChangeListener<Class<? extends BaseIdentity>, InstanceIdentifier<?>>> ListenerRegistration<L> registerRouteChangeListener(
            final L listener) {
        return new SingletonListenerRegistration<L>(listener);
    }

    @Override
    public Class<C> getIdentifier() {
        return contextType;
    }

    @Override
    @SuppressWarnings("unchecked")
    public void updateRoute(final InstanceIdentifier<?> path, final S service) {
        S previous = this.routes.put(path, service);

        LOGGER.debug("Route {} updated to {} in routing table {}",path,service,this);
        @SuppressWarnings("rawtypes")
        RouteChangeListener listenerCapture = listener;
        if (previous == null && listenerCapture != null) {
            listenerCapture.onRouteChange(RoutingUtils.announcementChange(contextType, path));
        }
    }


    @Override
    @SuppressWarnings("unchecked")
    public void removeRoute(final InstanceIdentifier<?> path) {
        S previous = this.routes.remove(path);
        LOGGER.debug("Route {} to {} removed in routing table {}",path,previous,this);
        @SuppressWarnings("rawtypes")
        RouteChangeListener listenerCapture = listener;
        if (previous != null && listenerCapture != null) {
            listenerCapture.onRouteChange(RoutingUtils.removalChange(contextType, path));
        }
    }

    void removeRoute(final InstanceIdentifier<?> path, final S service) {
        @SuppressWarnings("rawtypes")
        RouteChangeListener listenerCapture = listener;
        if (routes.remove(path, service) && listenerCapture != null) {
            LOGGER.debug("Route {} to {} removed in routing table {}",path,service,this);
            listenerCapture.onRouteChange(RoutingUtils.removalChange(contextType, path));
        }
    }

    @Override
    public S getRoute(final InstanceIdentifier<?> nodeInstance) {
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

    void removeAllReferences(final S service) {
        // FIXME: replace this via properly-synchronized BiMap (or something)
        final Iterator<S> it = routes.values().iterator();
        while (it.hasNext()) {
            final S s = it.next();
            if (service.equals(s)) {
                it.remove();
            }
        }
    }

    @Override
    public String toString() {
        return "RpcRoutingTableImpl [router=" + routerName + ", service=" + serviceType.getSimpleName() + ", context="
                + contextType.getSimpleName() + "]";
    }

    private class SingletonListenerRegistration<L extends RouteChangeListener<Class<? extends BaseIdentity>, InstanceIdentifier<?>>> extends
            AbstractObjectRegistration<L>
            implements ListenerRegistration<L> {

        public SingletonListenerRegistration(final L instance) {
            super(instance);
            listener = instance;
        }

        @Override
        protected void removeRegistration() {
            listener = null;
        }
    }
}
