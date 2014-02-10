/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.binding.impl;

import static com.google.common.base.Preconditions.checkState;

import java.util.EventListener;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.WeakHashMap;

import org.opendaylight.controller.md.sal.common.api.routing.RouteChange;
import org.opendaylight.controller.md.sal.common.api.routing.RouteChangeListener;
import org.opendaylight.controller.md.sal.common.api.routing.RouteChangePublisher;
import org.opendaylight.controller.md.sal.common.impl.routing.RoutingUtils;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker.RoutedRpcRegistration;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker.RpcRegistration;
import org.opendaylight.controller.sal.binding.api.RpcProviderRegistry;
import org.opendaylight.controller.sal.binding.api.rpc.RpcContextIdentifier;
import org.opendaylight.controller.sal.binding.api.rpc.RpcRouter;
import org.opendaylight.controller.sal.binding.codegen.RuntimeCodeGenerator;
import org.opendaylight.controller.sal.binding.codegen.RuntimeCodeHelper;
import org.opendaylight.controller.sal.binding.codegen.impl.SingletonHolder;
import org.opendaylight.yangtools.concepts.AbstractObjectRegistration;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.concepts.util.ListenerRegistry;
import org.opendaylight.yangtools.yang.binding.BaseIdentity;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.binding.RpcService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RpcProviderRegistryImpl implements //
        RpcProviderRegistry, //
        RouteChangePublisher<RpcContextIdentifier, InstanceIdentifier<?>> {

    private RuntimeCodeGenerator rpcFactory = SingletonHolder.RPC_GENERATOR_IMPL;

    private final Map<Class<? extends RpcService>, RpcService> publicProxies = new WeakHashMap<>();
    private final Map<Class<? extends RpcService>, RpcRouter<?>> rpcRouters = new WeakHashMap<>();
    private final ListenerRegistry<RouteChangeListener<RpcContextIdentifier, InstanceIdentifier<?>>> routeChangeListeners = ListenerRegistry
            .create();
    private final ListenerRegistry<RouterInstantiationListener> routerInstantiationListener = ListenerRegistry.create();

    private final static Logger LOG = LoggerFactory.getLogger(RpcProviderRegistryImpl.class);

    private final String name;

    private final ListenerRegistry<GlobalRpcRegistrationListener> globalRpcListeners = ListenerRegistry.create();

    public String getName() {
        return name;
    }

    public RpcProviderRegistryImpl(String name) {
        super();
        this.name = name;
    }

    @Override
    public final <T extends RpcService> RoutedRpcRegistration<T> addRoutedRpcImplementation(Class<T> type,
            T implementation) throws IllegalStateException {
        return getRpcRouter(type).addRoutedRpcImplementation(implementation);
    }

    @Override
    public final <T extends RpcService> RpcRegistration<T> addRpcImplementation(Class<T> type, T implementation)
            throws IllegalStateException {
        @SuppressWarnings("unchecked")
        RpcRouter<T> potentialRouter = (RpcRouter<T>) rpcRouters.get(type);
        if (potentialRouter != null) {
            checkState(potentialRouter.getDefaultService() == null,
                    "Default service for routed RPC already registered.");
            return potentialRouter.registerDefaultService(implementation);
        }
        T publicProxy = getRpcService(type);
        RpcService currentDelegate = RuntimeCodeHelper.getDelegate(publicProxy);
        checkState(currentDelegate == null, "Rpc service is already registered");
        LOG.debug("Registering {} as global implementation of {} in {}", implementation, type.getSimpleName(), this);
        RuntimeCodeHelper.setDelegate(publicProxy, implementation);
        notifyGlobalRpcAdded(type);
        return new RpcProxyRegistration<T>(type, implementation, this);
    }

    @SuppressWarnings("unchecked")
    @Override
    public final <T extends RpcService> T getRpcService(Class<T> type) {

        T potentialProxy = (T) publicProxies.get(type);
        if (potentialProxy != null) {
            return potentialProxy;
        }
        synchronized (this) {
            /**
             * Potential proxy could be instantiated by other thread while we
             * were waiting for the lock.
             */

            potentialProxy = (T) publicProxies.get(type);
            if (potentialProxy != null) {
                return potentialProxy;
            }
            T proxy = rpcFactory.getDirectProxyFor(type);
            LOG.debug("Created {} as public proxy for {} in {}", proxy, type.getSimpleName(), this);
            publicProxies.put(type, proxy);
            return proxy;
        }
    }

    @SuppressWarnings("unchecked")
    public <T extends RpcService> RpcRouter<T> getRpcRouter(Class<T> type) {
        RpcRouter<?> potentialRouter = rpcRouters.get(type);
        if (potentialRouter != null) {
            return (RpcRouter<T>) potentialRouter;
        }
        synchronized (this) {
            /**
             * Potential Router could be instantiated by other thread while we
             * were waiting for the lock.
             */
            potentialRouter = rpcRouters.get(type);
            if (potentialRouter != null) {
                return (RpcRouter<T>) potentialRouter;
            }
            RpcRouter<T> router = rpcFactory.getRouterFor(type, name);
            router.registerRouteChangeListener(new RouteChangeForwarder(type));
            LOG.debug("Registering router {} as global implementation of {} in {}", router, type.getSimpleName(), this);
            RuntimeCodeHelper.setDelegate(getRpcService(type), router.getInvocationProxy());
            rpcRouters.put(type, router);
            notifyListenersRoutedCreated(router);
            return router;
        }
    }

    private void notifyGlobalRpcAdded(Class<? extends RpcService> type) {
        for(ListenerRegistration<GlobalRpcRegistrationListener> listener : globalRpcListeners) {
            try {
                listener.getInstance().onGlobalRpcRegistered(type);
            } catch (Exception e) {
                LOG.error("Unhandled exception during invoking listener {}", e);
            }
        }

    }

    private void notifyListenersRoutedCreated(RpcRouter<?> router) {

        for (ListenerRegistration<RouterInstantiationListener> listener : routerInstantiationListener) {
            try {
                listener.getInstance().onRpcRouterCreated(router);
            } catch (Exception e) {
                LOG.error("Unhandled exception during invoking listener {}", e);
            }
        }

    }

    public ListenerRegistration<RouterInstantiationListener> registerRouterInstantiationListener(
            RouterInstantiationListener listener) {
        ListenerRegistration<RouterInstantiationListener> reg = routerInstantiationListener.register(listener);
        try {
            for (RpcRouter<?> router : rpcRouters.values()) {
                listener.onRpcRouterCreated(router);
            }
        } catch (Exception e) {
            LOG.error("Unhandled exception during invoking listener {}", e);
        }
        return reg;
    }

    @Override
    public <L extends RouteChangeListener<RpcContextIdentifier, InstanceIdentifier<?>>> ListenerRegistration<L> registerRouteChangeListener(
            L listener) {
        return (ListenerRegistration<L>) routeChangeListeners.register(listener);
    }

    public RuntimeCodeGenerator getRpcFactory() {
        return rpcFactory;
    }

    public void setRpcFactory(RuntimeCodeGenerator rpcFactory) {
        this.rpcFactory = rpcFactory;
    }

    public interface RouterInstantiationListener extends EventListener {
        void onRpcRouterCreated(RpcRouter<?> router);
    }

    public ListenerRegistration<GlobalRpcRegistrationListener> registerGlobalRpcRegistrationListener(GlobalRpcRegistrationListener listener) {
        return globalRpcListeners.register(listener);
    }

    public interface GlobalRpcRegistrationListener extends EventListener {
        void onGlobalRpcRegistered(Class<? extends RpcService> cls);
        void onGlobalRpcUnregistered(Class<? extends RpcService> cls);

    }

    private class RouteChangeForwarder<T extends RpcService> implements
            RouteChangeListener<Class<? extends BaseIdentity>, InstanceIdentifier<?>> {

        private final Class<T> type;

        public RouteChangeForwarder(Class<T> type) {
            this.type = type;
        }

        @Override
        public void onRouteChange(RouteChange<Class<? extends BaseIdentity>, InstanceIdentifier<?>> change) {
            Map<RpcContextIdentifier, Set<InstanceIdentifier<?>>> announcements = new HashMap<>();
            for (Entry<Class<? extends BaseIdentity>, Set<InstanceIdentifier<?>>> entry : change.getAnnouncements()
                    .entrySet()) {
                RpcContextIdentifier key = RpcContextIdentifier.contextFor(type, entry.getKey());
                announcements.put(key, entry.getValue());
            }
            Map<RpcContextIdentifier, Set<InstanceIdentifier<?>>> removals = new HashMap<>();
            for (Entry<Class<? extends BaseIdentity>, Set<InstanceIdentifier<?>>> entry : change.getRemovals()
                    .entrySet()) {
                RpcContextIdentifier key = RpcContextIdentifier.contextFor(type, entry.getKey());
                removals.put(key, entry.getValue());
            }
            RouteChange<RpcContextIdentifier, InstanceIdentifier<?>> toPublish = RoutingUtils
                    .<RpcContextIdentifier, InstanceIdentifier<?>> change(announcements, removals);
            for (ListenerRegistration<RouteChangeListener<RpcContextIdentifier, InstanceIdentifier<?>>> listener : routeChangeListeners) {
                try {
                    listener.getInstance().onRouteChange(toPublish);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public static class RpcProxyRegistration<T extends RpcService> extends AbstractObjectRegistration<T> implements
            RpcRegistration<T> {

        private final Class<T> serviceType;
        private RpcProviderRegistryImpl registry;

        public RpcProxyRegistration(Class<T> type, T service, RpcProviderRegistryImpl registry) {
            super(service);
            serviceType = type;
        }

        @Override
        public Class<T> getServiceType() {
            return serviceType;
        }

        @Override
        protected void removeRegistration() {
            if (registry != null) {
                T publicProxy = registry.getRpcService(serviceType);
                RpcService currentDelegate = RuntimeCodeHelper.getDelegate(publicProxy);
                if (currentDelegate == getInstance()) {
                    RuntimeCodeHelper.setDelegate(publicProxy, null);
                }
                registry = null;
            }
        }
    }
}
