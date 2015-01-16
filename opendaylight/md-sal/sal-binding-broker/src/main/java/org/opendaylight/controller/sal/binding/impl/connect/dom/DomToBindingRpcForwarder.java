package org.opendaylight.controller.sal.binding.impl.connect.dom;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;
import com.google.common.base.Function;
import com.google.common.collect.Collections2;
import com.google.common.collect.ImmutableSet;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import java.lang.ref.WeakReference;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.Callable;
import org.opendaylight.controller.sal.binding.api.RpcProviderRegistry;
import org.opendaylight.controller.sal.binding.api.rpc.RpcRouter;
import org.opendaylight.controller.sal.binding.impl.RpcProviderRegistryImpl;
import org.opendaylight.controller.sal.core.api.Broker.RoutedRpcRegistration;
import org.opendaylight.controller.sal.core.api.RpcImplementation;
import org.opendaylight.controller.sal.core.api.RpcProvisionRegistry;
import org.opendaylight.yangtools.concepts.CompositeObjectRegistration;
import org.opendaylight.yangtools.concepts.ObjectRegistration;
import org.opendaylight.yangtools.util.ClassLoaderUtils;
import org.opendaylight.yangtools.yang.binding.BaseIdentity;
import org.opendaylight.yangtools.yang.binding.BindingMapping;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.binding.RpcService;
import org.opendaylight.yangtools.yang.binding.util.BindingReflections;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.data.api.CompositeNode;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.impl.codec.BindingIndependentMappingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class DomToBindingRpcForwarder implements RpcImplementation, InvocationHandler {

    private final Logger LOG = LoggerFactory.getLogger(DomToBindingRpcForwarder.class);

    private final Set<QName> supportedRpcs;
    private final WeakReference<Class<? extends RpcService>> rpcServiceType;
    private Set<RoutedRpcRegistration> registrations;
    private final Map<QName, RpcInvocationStrategy> strategiesByQName = new HashMap<>();
    private final WeakHashMap<Method, RpcInvocationStrategy> strategiesByMethod = new WeakHashMap<>();
    private final RpcService proxy;
    private ObjectRegistration<?> forwarderRegistration;
    private boolean registrationInProgress = false;

    private final RpcProvisionRegistry biRpcRegistry;
    private final RpcProviderRegistry baRpcRegistry;
    private final RpcProviderRegistryImpl baRpcRegistryImpl;

    private final Function<InstanceIdentifier<?>, YangInstanceIdentifier> toDOMInstanceIdentifier;

    private final static Method EQUALS_METHOD;

    static {
        try {
            EQUALS_METHOD = Object.class.getMethod("equals", Object.class);
        } catch (NoSuchMethodException | SecurityException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    public DomToBindingRpcForwarder(final Class<? extends RpcService> service, final BindingIndependentMappingService mappingService,
        final RpcProvisionRegistry biRpcRegistry, final RpcProviderRegistry baRpcRegistry, final RpcProviderRegistryImpl registryImpl) {
        this.rpcServiceType = new WeakReference<Class<? extends RpcService>>(service);
        this.supportedRpcs = mappingService.getRpcQNamesFor(service);

        this.toDOMInstanceIdentifier = new Function<InstanceIdentifier<?>, YangInstanceIdentifier>() {
            @Override
            public YangInstanceIdentifier apply(final InstanceIdentifier<?> input) {
                return mappingService.toDataDom(input);
            }
        };

        this.biRpcRegistry = biRpcRegistry;
        this.baRpcRegistry = baRpcRegistry;
        this.baRpcRegistryImpl = registryImpl;

        Class<?> cls = rpcServiceType.get();
        ClassLoader clsLoader = cls.getClassLoader();
        proxy =(RpcService) Proxy.newProxyInstance(clsLoader, new Class<?>[] { cls }, this);
        createStrategies(mappingService);
    }

    /**
     * Constructor for Routed RPC Forwarder.
     *
     * @param service
     * @param context
     * @param registryImpl
     */
    public DomToBindingRpcForwarder(final Class<? extends RpcService> service,
        final Class<? extends BaseIdentity> context, final BindingIndependentMappingService mappingService,
        final RpcProvisionRegistry biRpcRegistry, final RpcProviderRegistry baRpcRegistry, final RpcProviderRegistryImpl registryImpl) {
        this(service, mappingService, biRpcRegistry, baRpcRegistry,registryImpl);

        final ImmutableSet.Builder<RoutedRpcRegistration> registrationsBuilder = ImmutableSet.builder();
        try {
            for (QName rpc : supportedRpcs) {
                registrationsBuilder.add(biRpcRegistry.addRoutedRpcImplementation(rpc, this));
            }
            createDefaultDomForwarder();
        } catch (Exception e) {
            LOG.error("Could not forward Rpcs of type {}", service.getName(), e);
        }
        registrations = registrationsBuilder.build();
    }



    private void createStrategies(final BindingIndependentMappingService mappingService) {
        try {
            for (QName rpc : supportedRpcs) {
                RpcInvocationStrategy strategy = createInvocationStrategy(rpc, rpcServiceType.get(), mappingService);
                strategiesByMethod.put(strategy.targetMethod, strategy);
                strategiesByQName.put(rpc, strategy);
            }
        } catch (Exception e) {
            LOG.error("Could not forward Rpcs of type {}", rpcServiceType.get(), e);
        }

    }

    /**
     * Registers RPC Forwarder to DOM Broker,
     * this means Binding Aware Broker has implementation of RPC
     * which is registered to it.
     *
     * If RPC Forwarder was previously registered to DOM Broker
     * or to Bidning Broker this method is noop to prevent
     * creating forwarding loop.
     *
     */
    public void registerToDOMBroker() {
        if(!registrationInProgress && forwarderRegistration == null) {
            registrationInProgress = true;
            CompositeObjectRegistration.CompositeObjectRegistrationBuilder<DomToBindingRpcForwarder> builder = CompositeObjectRegistration.builderFor(this);
            try {
                for (QName rpc : supportedRpcs) {
                    builder.add(biRpcRegistry.addRpcImplementation(rpc, this));
                }
            } catch (Exception e) {
                LOG.error("Could not forward Rpcs of type {}", rpcServiceType.get(), e);
            }
            this.forwarderRegistration = builder.build();
            registrationInProgress = false;
        }
    }


    public void registerPaths(final Class<? extends BaseIdentity> context,
        final Class<? extends RpcService> service, final Set<InstanceIdentifier<?>> set) {
        QName ctx = BindingReflections.findQName(context);
        for (YangInstanceIdentifier path : Collections2.transform(set, toDOMInstanceIdentifier)) {
            for (RoutedRpcRegistration reg : registrations) {
                reg.registerPath(ctx, path);
            }
        }
    }

    @Override
    public Object invoke(final Object proxy, final Method method, final Object[] args) throws Throwable {
        if (EQUALS_METHOD.equals(method)) {
            return false;
        }
        RpcInvocationStrategy strategy = strategiesByMethod.get(method);
        checkState(strategy != null);
        checkArgument(args.length <= 2);
        if (args.length == 1) {
            checkArgument(args[0] instanceof DataObject);
            return strategy.forwardToDomBroker((DataObject) args[0]);
        }
        return strategy.forwardToDomBroker(null);
    }

    public void removePaths(final Class<? extends BaseIdentity> context, final Class<? extends RpcService> service,
        final Set<InstanceIdentifier<?>> set) {
        QName ctx = BindingReflections.findQName(context);
        for (YangInstanceIdentifier path : Collections2.transform(set, toDOMInstanceIdentifier)) {
            for (RoutedRpcRegistration reg : registrations) {
                reg.unregisterPath(ctx, path);
            }
        }
    }

    @Override
    public Set<QName> getSupportedRpcs() {
        return supportedRpcs;
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public void createDefaultDomForwarder() {
        if (baRpcRegistryImpl != null) {
            Class<?> cls = rpcServiceType.get();
            ClassLoader clsLoader = cls.getClassLoader();
            RpcService proxy = (RpcService) Proxy.newProxyInstance(clsLoader, new Class<?>[] { cls }, this);

            RpcRouter rpcRouter = baRpcRegistryImpl.getRpcRouter(rpcServiceType.get());
            rpcRouter.registerDefaultService(proxy);
        }
    }

    @Override
    public ListenableFuture<RpcResult<CompositeNode>> invokeRpc(final QName rpc, final CompositeNode domInput) {
        checkArgument(rpc != null);
        checkArgument(domInput != null);

        Class<? extends RpcService> rpcType = rpcServiceType.get();
        checkState(rpcType != null);
        RpcService rpcService = baRpcRegistry.getRpcService(rpcType);
        checkState(rpcService != null);
        CompositeNode domUnwrappedInput = domInput.getFirstCompositeByName(QName.create(rpc, "input"));

        try {
            return Futures.immediateFuture(resolveInvocationStrategy(rpc).invokeOn(rpcService, domUnwrappedInput));
        } catch (Exception e) {
            return Futures.immediateFailedFuture(e);
        }
    }

    private RpcInvocationStrategy resolveInvocationStrategy(final QName rpc) {
        return strategiesByQName.get(rpc);
    }

    private RpcInvocationStrategy createInvocationStrategy(final QName rpc,
        final Class<? extends RpcService> rpcType, final BindingIndependentMappingService mappingService) throws Exception {
        return ClassLoaderUtils.withClassLoader(rpcType.getClassLoader(), new Callable<RpcInvocationStrategy>() {
            @Override
            public RpcInvocationStrategy call() throws Exception {
                String methodName = BindingMapping.getMethodName(rpc);
                Method targetMethod = null;
                for (Method possibleMethod : rpcType.getMethods()) {
                    if (possibleMethod.getName().equals(methodName)
                        && BindingReflections.isRpcMethod(possibleMethod)) {
                        targetMethod = possibleMethod;
                        break;
                    }
                }
                checkState(targetMethod != null, "Rpc method not found");
                return new RpcInvocationStrategy(rpc, targetMethod, mappingService, biRpcRegistry);
            }

        });
    }

    /**
     * Registers RPC Forwarder to Binding Broker,
     * this means DOM Broekr has implementation of RPC
     * which is registered to it.
     *
     * If RPC Forwarder was previously registered to DOM Broker
     * or to Bidning Broker this method is noop to prevent
     * creating forwarding loop.
     *
     */
    public void registerToBindingBroker() {
        if(!registrationInProgress && forwarderRegistration == null) {
            try {
                registrationInProgress = true;
                this.forwarderRegistration = baRpcRegistry.addRpcImplementation((Class)rpcServiceType.get(), proxy);
            } catch (Exception e) {
                LOG.error("Unable to forward RPCs for {}",rpcServiceType.get(),e);
            } finally {
                registrationInProgress = false;
            }
        }
    }
}
