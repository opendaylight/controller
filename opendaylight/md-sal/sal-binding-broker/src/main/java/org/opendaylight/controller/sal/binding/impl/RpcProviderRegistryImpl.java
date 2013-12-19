package org.opendaylight.controller.sal.binding.impl;

import java.util.Map;
import java.util.Map.Entry;
import java.util.HashMap;
import java.util.Set;
import java.util.WeakHashMap;

import javax.swing.tree.ExpandVetoException;

import org.opendaylight.controller.md.sal.common.api.routing.RouteChange;
import org.opendaylight.controller.md.sal.common.api.routing.RouteChangeListener;
import org.opendaylight.controller.md.sal.common.api.routing.RouteChangePublisher;
import org.opendaylight.controller.md.sal.common.impl.routing.RoutingUtils;
import org.opendaylight.controller.sal.binding.api.RpcProviderRegistry;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker.RoutedRpcRegistration;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker.RpcRegistration;
import org.opendaylight.controller.sal.binding.codegen.RuntimeCodeGenerator;
import org.opendaylight.controller.sal.binding.codegen.RuntimeCodeHelper;
import org.opendaylight.controller.sal.binding.codegen.impl.SingletonHolder;
import org.opendaylight.controller.sal.binding.spi.RpcContextIdentifier;
import org.opendaylight.controller.sal.binding.spi.RpcRouter;
import org.opendaylight.yangtools.concepts.AbstractObjectRegistration;
import org.opendaylight.yangtools.concepts.Identifiable;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.concepts.util.ListenerRegistry;
import org.opendaylight.yangtools.yang.binding.BaseIdentity;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.binding.RpcService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.google.common.base.Preconditions.*;

public class RpcProviderRegistryImpl implements //
        RpcProviderRegistry, //
        RouteChangePublisher<RpcContextIdentifier, InstanceIdentifier<?>> {

    private RuntimeCodeGenerator rpcFactory = SingletonHolder.RPC_GENERATOR_IMPL;

    private final Map<Class<? extends RpcService>, RpcService> publicProxies = new WeakHashMap<>();
    private final Map<Class<? extends RpcService>, RpcRouter<?>> rpcRouters = new WeakHashMap<>();
    private final ListenerRegistry<RouteChangeListener<RpcContextIdentifier, InstanceIdentifier<?>>> routeChangeListeners = ListenerRegistry
            .create();

    private final static Logger LOG = LoggerFactory.getLogger(RpcProviderRegistryImpl.class);
    
    private final String name;

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
        LOG.debug("Registering {} as global implementation of {} in {}",implementation,type.getSimpleName(),this);
        RuntimeCodeHelper.setDelegate(publicProxy, implementation);
        return new RpcProxyRegistration<T>(type, implementation, this);
    }

    @SuppressWarnings("unchecked")
    @Override
    public final <T extends RpcService> T getRpcService(Class<T> type) {

        @SuppressWarnings("unchecked")
        T potentialProxy = (T) publicProxies.get(type);
        if (potentialProxy != null) {
            return potentialProxy;
        }
        synchronized(this) {
            /**
             * Potential proxy could be instantiated by other thread while we were
             * waiting for the lock.
             */
            
            potentialProxy = (T) publicProxies.get(type);
            if (potentialProxy != null) {
                return (T) potentialProxy;
            }
            T proxy = rpcFactory.getDirectProxyFor(type);
            LOG.debug("Created {} as public proxy for {} in {}",proxy,type.getSimpleName(),this);
            publicProxies.put(type, proxy);
            return proxy;
        }
    }

    private <T extends RpcService> RpcRouter<T> getRpcRouter(Class<T> type) {
        RpcRouter<?> potentialRouter = rpcRouters.get(type);
        if (potentialRouter != null) {
            return (RpcRouter<T>) potentialRouter;
        }
        synchronized(this) {
            /**
             * Potential Router could be instantiated by other thread while we were
             * waiting for the lock.
             */
            potentialRouter = rpcRouters.get(type); 
            if (potentialRouter != null) {
                return (RpcRouter<T>) potentialRouter;
            }
            RpcRouter<T> router = rpcFactory.getRouterFor(type,name);
            router.registerRouteChangeListener(new RouteChangeForwarder(type));
            LOG.debug("Registering router {} as global implementation of {} in {}",router,type.getSimpleName(),this);
            RuntimeCodeHelper.setDelegate(getRpcService(type), router.getInvocationProxy());
            rpcRouters.put(type, router);
            return router;
        }
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
