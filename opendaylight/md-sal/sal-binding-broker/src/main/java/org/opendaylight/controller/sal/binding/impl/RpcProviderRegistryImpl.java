/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.binding.impl;

import static com.google.common.base.Preconditions.checkState;
import com.google.common.base.Preconditions;
import com.google.common.base.Throwables;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.util.concurrent.UncheckedExecutionException;
import java.util.EventListener;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;
import org.opendaylight.controller.md.sal.common.api.routing.RouteChange;
import org.opendaylight.controller.md.sal.common.api.routing.RouteChangeListener;
import org.opendaylight.controller.md.sal.common.api.routing.RouteChangePublisher;
import org.opendaylight.controller.md.sal.common.impl.routing.RoutingUtils;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker.RoutedRpcRegistration;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker.RpcRegistration;
import org.opendaylight.controller.sal.binding.api.RpcProviderRegistry;
import org.opendaylight.controller.sal.binding.api.rpc.RpcContextIdentifier;
import org.opendaylight.controller.sal.binding.api.rpc.RpcRouter;
import org.opendaylight.controller.sal.binding.codegen.RpcIsNotRoutedException;
import org.opendaylight.controller.sal.binding.codegen.RuntimeCodeGenerator;
import org.opendaylight.controller.sal.binding.codegen.RuntimeCodeHelper;
import org.opendaylight.controller.sal.binding.codegen.impl.SingletonHolder;
import org.opendaylight.yangtools.concepts.AbstractObjectRegistration;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.util.ListenerRegistry;
import org.opendaylight.yangtools.yang.binding.BaseIdentity;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.binding.RpcService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RpcProviderRegistryImpl implements RpcProviderRegistry, RouteChangePublisher<RpcContextIdentifier, InstanceIdentifier<?>> {

    private RuntimeCodeGenerator rpcFactory = SingletonHolder.RPC_GENERATOR_IMPL;

    // cache of proxy objects where each value in the map corresponds to a specific RpcService
    private final LoadingCache<Class<? extends RpcService>, RpcService> publicProxies = CacheBuilder.newBuilder().weakKeys().
            build(new CacheLoader<Class<? extends RpcService>, RpcService>() {
                @Override
                public RpcService load(final Class<? extends RpcService> type) {
                    final RpcService proxy = rpcFactory.getDirectProxyFor(type);
                    LOG.debug("Created {} as public proxy for {} in {}", proxy, type.getSimpleName(), this);
                    return proxy;
                }
            });

    private final Cache<Class<? extends RpcService>, RpcRouter<?>> rpcRouters = CacheBuilder.newBuilder().weakKeys()
            .build();

    private final ListenerRegistry<RouteChangeListener<RpcContextIdentifier, InstanceIdentifier<?>>> routeChangeListeners = ListenerRegistry
            .create();
    private final ListenerRegistry<RouterInstantiationListener> routerInstantiationListener = ListenerRegistry.create();

    private final static Logger LOG = LoggerFactory.getLogger(RpcProviderRegistryImpl.class);

    private final String name;

    private final ListenerRegistry<GlobalRpcRegistrationListener> globalRpcListeners = ListenerRegistry.create();

    public String getName() {
        return name;
    }

    public RpcProviderRegistryImpl(final String name) {
        super();
        this.name = name;
    }

    @Override
    public final <T extends RpcService> RoutedRpcRegistration<T> addRoutedRpcImplementation(final Class<T> type,
            final T implementation) throws IllegalStateException {
        return getRpcRouter(type).addRoutedRpcImplementation(implementation);
    }

    @Override
    public final <T extends RpcService> RpcRegistration<T> addRpcImplementation(final Class<T> type, final T implementation) {

        // FIXME: This should be well documented - addRpcImplementation for
        // routed RPCs
        try {
            // Note: If RPC is really global, expected count of registrations
            // of this method is really low.
            RpcRouter<T> potentialRouter = getRpcRouter(type);
            checkState(potentialRouter.getDefaultService() == null,
                        "Default service for routed RPC already registered.");
            return potentialRouter.registerDefaultService(implementation);
        } catch (RpcIsNotRoutedException e) {
            // NOOP - we could safely continue, since RPC is not routed
            // so we fallback to global routing.
            LOG.debug("RPC is not routed. Using global registration.",e);
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
    public final <T extends RpcService> T getRpcService(final Class<T> type) {
        return (T) publicProxies.getUnchecked(type);
    }


    public <T extends RpcService> RpcRouter<T> getRpcRouter(final Class<T> type) {
        try {
            final AtomicBoolean created = new AtomicBoolean(false);
            @SuppressWarnings( "unchecked")
            // LoadingCache is unsuitable for RpcRouter since we need to distinguish
            // first creation of RPC Router, so that is why
            // we are using normal cache with load API and shared AtomicBoolean
            // for this call, which will be set to true if router was created.
            RpcRouter<T> router = (RpcRouter<T>) rpcRouters.get(type,new Callable<RpcRouter<?>>() {

                @Override
                public org.opendaylight.controller.sal.binding.api.rpc.RpcRouter<?> call()  {
                    RpcRouter<?> router = rpcFactory.getRouterFor(type, name);
                    router.registerRouteChangeListener(new RouteChangeForwarder<T>(type));
                    LOG.debug("Registering router {} as global implementation of {} in {}", router, type.getSimpleName(), this);
                    RuntimeCodeHelper.setDelegate(getRpcService(type), router.getInvocationProxy());
                    created.set(true);
                    return router;
                }
            });
            if(created.get()) {
                notifyListenersRoutedCreated(router);
            }
            return router;
        } catch (ExecutionException | UncheckedExecutionException e) {
            // We rethrow Runtime Exceptions which were wrapped by
            // Execution Exceptions
            // otherwise we throw IllegalStateException with original
            Throwables.propagateIfPossible(e.getCause());
            throw new IllegalStateException("Could not load RPC Router for "+type.getName(),e);
        }
    }

    private void notifyGlobalRpcAdded(final Class<? extends RpcService> type) {
        for(ListenerRegistration<GlobalRpcRegistrationListener> listener : globalRpcListeners) {
            try {
                listener.getInstance().onGlobalRpcRegistered(type);
            } catch (Exception e) {
                LOG.error("Unhandled exception during invoking listener {}", e);
            }
        }

    }

    private void notifyListenersRoutedCreated(final RpcRouter<?> router) {

        for (ListenerRegistration<RouterInstantiationListener> listener : routerInstantiationListener) {
            try {
                listener.getInstance().onRpcRouterCreated(router);
            } catch (Exception e) {
                LOG.error("Unhandled exception during invoking listener {}", e);
            }
        }

    }

    public ListenerRegistration<RouterInstantiationListener> registerRouterInstantiationListener(
            final RouterInstantiationListener listener) {
        ListenerRegistration<RouterInstantiationListener> reg = routerInstantiationListener.register(listener);
        try {
            for (RpcRouter<?> router : rpcRouters.asMap().values()) {
                listener.onRpcRouterCreated(router);
            }
        } catch (Exception e) {
            LOG.error("Unhandled exception during invoking listener {}", e);
        }
        return reg;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <L extends RouteChangeListener<RpcContextIdentifier, InstanceIdentifier<?>>> ListenerRegistration<L> registerRouteChangeListener(
            final L listener) {
        return (ListenerRegistration<L>) routeChangeListeners.register(listener);
    }

    public RuntimeCodeGenerator getRpcFactory() {
        return rpcFactory;
    }

    public void setRpcFactory(final RuntimeCodeGenerator rpcFactory) {
        this.rpcFactory = rpcFactory;
    }

    public interface RouterInstantiationListener extends EventListener {
        void onRpcRouterCreated(RpcRouter<?> router);
    }

    public ListenerRegistration<GlobalRpcRegistrationListener> registerGlobalRpcRegistrationListener(final GlobalRpcRegistrationListener listener) {
        return globalRpcListeners.register(listener);
    }

    public interface GlobalRpcRegistrationListener extends EventListener {
        void onGlobalRpcRegistered(Class<? extends RpcService> cls);
        void onGlobalRpcUnregistered(Class<? extends RpcService> cls);

    }

    private final class RouteChangeForwarder<T extends RpcService> implements RouteChangeListener<Class<? extends BaseIdentity>, InstanceIdentifier<?>> {
        private final Class<T> type;

        RouteChangeForwarder(final Class<T> type) {
            this.type = type;
        }

        @Override
        public void onRouteChange(final RouteChange<Class<? extends BaseIdentity>, InstanceIdentifier<?>> change) {
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
                    LOG.error("Unhandled exception during invoking listener",listener.getInstance(),e);
                }
            }
        }
    }

    private static final class RpcProxyRegistration<T extends RpcService> extends AbstractObjectRegistration<T> implements RpcRegistration<T> {
        private final RpcProviderRegistryImpl registry;
        private final Class<T> serviceType;

        RpcProxyRegistration(final Class<T> type, final T service, final RpcProviderRegistryImpl registry) {
            super(service);
            this.registry =  Preconditions.checkNotNull(registry);
            this.serviceType = type;
        }

        @Override
        public Class<T> getServiceType() {
            return serviceType;
        }

        @Override
        protected void removeRegistration() {
            T publicProxy = registry.getRpcService(serviceType);
            RpcService currentDelegate = RuntimeCodeHelper.getDelegate(publicProxy);
            if (currentDelegate == getInstance()) {
                RuntimeCodeHelper.setDelegate(publicProxy, null);
            }
        }
    }
}
