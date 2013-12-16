package org.opendaylight.controller.sal.binding.impl.connect.dom;

import java.lang.ref.WeakReference;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;


import org.opendaylight.controller.md.sal.common.api.RegistrationListener;
import org.opendaylight.controller.md.sal.common.api.TransactionStatus;
import org.opendaylight.controller.md.sal.common.api.data.DataCommitHandler;
import org.opendaylight.controller.md.sal.common.api.data.DataCommitHandler.DataCommitTransaction;
import org.opendaylight.controller.md.sal.common.api.data.DataCommitHandlerRegistration;
import org.opendaylight.controller.md.sal.common.api.data.DataModification;
import org.opendaylight.controller.md.sal.common.api.routing.RouteChange;
import org.opendaylight.controller.md.sal.common.api.routing.RouteChangeListener;
import org.opendaylight.controller.sal.binding.api.RpcProviderRegistry;
import org.opendaylight.controller.sal.binding.api.data.DataProviderService;
import org.opendaylight.controller.sal.binding.api.data.RuntimeDataProvider;
import org.opendaylight.controller.sal.binding.impl.RpcProviderRegistryImpl;
import org.opendaylight.controller.sal.binding.spi.RpcContextIdentifier;
import org.opendaylight.controller.sal.binding.spi.RpcRouter;
import org.opendaylight.controller.sal.common.util.Rpcs;
import org.opendaylight.controller.sal.core.api.Provider;
import org.opendaylight.controller.sal.core.api.Broker.ProviderSession;
import org.opendaylight.controller.sal.core.api.RpcImplementation;
import org.opendaylight.controller.sal.core.api.RpcProvisionRegistry;
import org.opendaylight.controller.sal.core.api.data.DataModificationTransaction;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.concepts.Registration;
import org.opendaylight.yangtools.concepts.util.ClassLoaderUtils;
import org.opendaylight.yangtools.yang.binding.Augmentable;
import org.opendaylight.yangtools.yang.binding.Augmentation;
import org.opendaylight.yangtools.yang.binding.BaseIdentity;
import org.opendaylight.yangtools.yang.binding.BindingMapping;
import org.opendaylight.yangtools.yang.binding.DataContainer;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.binding.RpcInput;
import org.opendaylight.yangtools.yang.binding.RpcService;
import org.opendaylight.yangtools.yang.binding.util.BindingReflections;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.common.RpcError;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.data.api.CompositeNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableSet;

import static com.google.common.base.Preconditions.*;
import static org.opendaylight.yangtools.concepts.util.ClassLoaderUtils.*;

public class BindingIndependentConnector implements //
        RuntimeDataProvider, //
        Provider, //
        AutoCloseable {

    private final Logger LOG = LoggerFactory.getLogger(BindingIndependentConnector.class);

    private static final InstanceIdentifier<? extends DataObject> ROOT = InstanceIdentifier.builder().toInstance();

    private static final org.opendaylight.yangtools.yang.data.api.InstanceIdentifier ROOT_BI = org.opendaylight.yangtools.yang.data.api.InstanceIdentifier
            .builder().toInstance();

    private BindingIndependentMappingService mappingService;

    private org.opendaylight.controller.sal.core.api.data.DataProviderService biDataService;

    private DataProviderService baDataService;

    private ConcurrentMap<Object, BindingToDomTransaction> domOpenedTransactions = new ConcurrentHashMap<>();
    private ConcurrentMap<Object, DomToBindingTransaction> bindingOpenedTransactions = new ConcurrentHashMap<>();

    private BindingToDomCommitHandler bindingToDomCommitHandler = new BindingToDomCommitHandler();
    private DomToBindingCommitHandler domToBindingCommitHandler = new DomToBindingCommitHandler();

    private Registration<DataCommitHandler<InstanceIdentifier<? extends DataObject>, DataObject>> baCommitHandlerRegistration;

    private Registration<DataCommitHandler<org.opendaylight.yangtools.yang.data.api.InstanceIdentifier, CompositeNode>> biCommitHandlerRegistration;

    private RpcProvisionRegistry biRpcRegistry;
    private RpcProviderRegistryImpl baRpcRegistry;

    private ListenerRegistration<DomToBindingRpcForwardingManager> domToBindingRpcManager;
    // private ListenerRegistration<BindingToDomRpcForwardingManager>
    // bindingToDomRpcManager;

    private Function<InstanceIdentifier<?>, org.opendaylight.yangtools.yang.data.api.InstanceIdentifier> toDOMInstanceIdentifier = new Function<InstanceIdentifier<?>, org.opendaylight.yangtools.yang.data.api.InstanceIdentifier>() {

        @Override
        public org.opendaylight.yangtools.yang.data.api.InstanceIdentifier apply(InstanceIdentifier<?> input) {
            return mappingService.toDataDom(input);
        }

    };

    @Override
    public DataObject readOperationalData(InstanceIdentifier<? extends DataObject> path) {
        try {
            org.opendaylight.yangtools.yang.data.api.InstanceIdentifier biPath = mappingService.toDataDom(path);

            CompositeNode result = biDataService.readOperationalData(biPath);
            Class<? extends DataObject> targetType = path.getTargetType();

            if (Augmentation.class.isAssignableFrom(targetType)) {
                path = mappingService.fromDataDom(biPath);
                Class<? extends Augmentation<?>> augmentType = (Class<? extends Augmentation<?>>) targetType;
                DataObject parentTo = mappingService.dataObjectFromDataDom(path, result);
                if (parentTo instanceof Augmentable<?>) {
                    return (DataObject) ((Augmentable) parentTo).getAugmentation(augmentType);
                }

            }
            return mappingService.dataObjectFromDataDom(path, result);

        } catch (DeserializationException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public DataObject readConfigurationData(InstanceIdentifier<? extends DataObject> path) {
        try {
            org.opendaylight.yangtools.yang.data.api.InstanceIdentifier biPath = mappingService.toDataDom(path);
            CompositeNode result = biDataService.readConfigurationData(biPath);
            return mappingService.dataObjectFromDataDom(path, result);
        } catch (DeserializationException e) {
            throw new IllegalStateException(e);
        }
    }

    private DataModificationTransaction createBindingToDomTransaction(
            DataModification<InstanceIdentifier<? extends DataObject>, DataObject> source) {
        DataModificationTransaction target = biDataService.beginTransaction();
        for (Entry<InstanceIdentifier<? extends DataObject>, DataObject> entry : source.getUpdatedConfigurationData()
                .entrySet()) {
            Entry<org.opendaylight.yangtools.yang.data.api.InstanceIdentifier, CompositeNode> biEntry = mappingService
                    .toDataDom(entry);
            target.putConfigurationData(biEntry.getKey(), biEntry.getValue());
        }
        for (Entry<InstanceIdentifier<? extends DataObject>, DataObject> entry : source.getUpdatedOperationalData()
                .entrySet()) {
            Entry<org.opendaylight.yangtools.yang.data.api.InstanceIdentifier, CompositeNode> biEntry = mappingService
                    .toDataDom(entry);
            target.putOperationalData(biEntry.getKey(), biEntry.getValue());
        }
        for (InstanceIdentifier<? extends DataObject> entry : source.getRemovedConfigurationData()) {
            org.opendaylight.yangtools.yang.data.api.InstanceIdentifier biEntry = mappingService.toDataDom(entry);
            target.removeConfigurationData(biEntry);
        }
        for (InstanceIdentifier<? extends DataObject> entry : source.getRemovedOperationalData()) {
            org.opendaylight.yangtools.yang.data.api.InstanceIdentifier biEntry = mappingService.toDataDom(entry);
            target.removeOperationalData(biEntry);
        }
        return target;
    }

    private org.opendaylight.controller.sal.binding.api.data.DataModificationTransaction createDomToBindingTransaction(
            DataModification<org.opendaylight.yangtools.yang.data.api.InstanceIdentifier, CompositeNode> source) {
        org.opendaylight.controller.sal.binding.api.data.DataModificationTransaction target = baDataService
                .beginTransaction();
        for (Entry<org.opendaylight.yangtools.yang.data.api.InstanceIdentifier, CompositeNode> entry : source
                .getUpdatedConfigurationData().entrySet()) {
            try {
                InstanceIdentifier<?> baKey = mappingService.fromDataDom(entry.getKey());
                DataObject baData = mappingService.dataObjectFromDataDom(baKey, entry.getValue());
                target.putConfigurationData(baKey, baData);
            } catch (DeserializationException e) {
                LOG.error("Ommiting from BA transaction: {}.", entry.getKey(), e);
            }
        }
        for (Entry<org.opendaylight.yangtools.yang.data.api.InstanceIdentifier, CompositeNode> entry : source
                .getUpdatedOperationalData().entrySet()) {
            try {

                InstanceIdentifier<?> baKey = mappingService.fromDataDom(entry.getKey());
                DataObject baData = mappingService.dataObjectFromDataDom(baKey, entry.getValue());
                target.putOperationalData(baKey, baData);
            } catch (DeserializationException e) {
                LOG.error("Ommiting from BA transaction: {}.", entry.getKey(), e);
            }
        }
        for (org.opendaylight.yangtools.yang.data.api.InstanceIdentifier entry : source.getRemovedConfigurationData()) {
            try {

                InstanceIdentifier<?> baEntry = mappingService.fromDataDom(entry);
                target.removeConfigurationData(baEntry);
            } catch (DeserializationException e) {
                LOG.error("Ommiting from BA transaction: {}.", entry, e);
            }
        }
        for (org.opendaylight.yangtools.yang.data.api.InstanceIdentifier entry : source.getRemovedOperationalData()) {
            try {

                InstanceIdentifier<?> baEntry = mappingService.fromDataDom(entry);
                target.removeOperationalData(baEntry);
            } catch (DeserializationException e) {
                LOG.error("Ommiting from BA transaction: {}.", entry, e);
            }
        }
        return target;
    }

    public org.opendaylight.controller.sal.core.api.data.DataProviderService getBiDataService() {
        return biDataService;
    }

    public void setBiDataService(org.opendaylight.controller.sal.core.api.data.DataProviderService biDataService) {
        this.biDataService = biDataService;
    }

    public DataProviderService getBaDataService() {
        return baDataService;
    }

    public void setBaDataService(DataProviderService baDataService) {
        this.baDataService = baDataService;
    }

    public RpcProviderRegistry getRpcRegistry() {
        return baRpcRegistry;
    }

    public void setRpcRegistry(RpcProviderRegistryImpl rpcRegistry) {
        this.baRpcRegistry = rpcRegistry;
    }

    public void start() {
        baDataService.registerDataReader(ROOT, this);
        baCommitHandlerRegistration = baDataService.registerCommitHandler(ROOT, bindingToDomCommitHandler);
        biCommitHandlerRegistration = biDataService.registerCommitHandler(ROOT_BI, domToBindingCommitHandler);
        baDataService.registerCommitHandlerListener(domToBindingCommitHandler);

        if (baRpcRegistry != null && biRpcRegistry != null) {
            domToBindingRpcManager = baRpcRegistry.registerRouteChangeListener(new DomToBindingRpcForwardingManager());

        }
    }

    public void setMappingService(BindingIndependentMappingService mappingService) {
        this.mappingService = mappingService;
    }

    @Override
    public Collection<ProviderFunctionality> getProviderFunctionality() {
        return Collections.emptyList();
    }

    @Override
    public void onSessionInitiated(ProviderSession session) {
        setBiDataService(session.getService(org.opendaylight.controller.sal.core.api.data.DataProviderService.class));
        start();
    }

    public <T extends RpcService> void onRpcRouterCreated(Class<T> serviceType, RpcRouter<T> router) {

    }

    public void setDomRpcRegistry(RpcProvisionRegistry registry) {
        biRpcRegistry = registry;
    }

    @Override
    public void close() throws Exception {
        if (baCommitHandlerRegistration != null) {
            baCommitHandlerRegistration.close();
        }
        if (biCommitHandlerRegistration != null) {
            biCommitHandlerRegistration.close();
        }

    }

    private class DomToBindingTransaction implements
            DataCommitTransaction<org.opendaylight.yangtools.yang.data.api.InstanceIdentifier, CompositeNode> {

        private final org.opendaylight.controller.sal.binding.api.data.DataModificationTransaction backing;
        private final DataModification<org.opendaylight.yangtools.yang.data.api.InstanceIdentifier, CompositeNode> modification;

        public DomToBindingTransaction(
                org.opendaylight.controller.sal.binding.api.data.DataModificationTransaction backing,
                DataModification<org.opendaylight.yangtools.yang.data.api.InstanceIdentifier, CompositeNode> modification) {
            super();
            this.backing = backing;
            this.modification = modification;
            bindingOpenedTransactions.put(backing.getIdentifier(), this);
        }

        @Override
        public DataModification<org.opendaylight.yangtools.yang.data.api.InstanceIdentifier, CompositeNode> getModification() {
            return modification;
        }

        @Override
        public RpcResult<Void> rollback() throws IllegalStateException {
            // backing.cancel();
            return Rpcs.<Void> getRpcResult(true, null, Collections.<RpcError> emptySet());
        }

        @Override
        public RpcResult<Void> finish() throws IllegalStateException {
            Future<RpcResult<TransactionStatus>> result = backing.commit();
            try {
                RpcResult<TransactionStatus> baResult = result.get();
                return Rpcs.<Void> getRpcResult(baResult.isSuccessful(), null, baResult.getErrors());
            } catch (InterruptedException e) {
                throw new IllegalStateException("", e);
            } catch (ExecutionException e) {
                throw new IllegalStateException("", e);
            }
        }
    }

    private class BindingToDomTransaction implements
            DataCommitTransaction<InstanceIdentifier<? extends DataObject>, DataObject> {

        private DataModificationTransaction backing;
        private DataModification<InstanceIdentifier<? extends DataObject>, DataObject> modification;

        public BindingToDomTransaction(DataModificationTransaction backing,
                DataModification<InstanceIdentifier<? extends DataObject>, DataObject> modification) {
            this.backing = backing;
            this.modification = modification;
            domOpenedTransactions.put(backing.getIdentifier(), this);
        }

        @Override
        public DataModification<InstanceIdentifier<? extends DataObject>, DataObject> getModification() {
            return modification;
        }

        @Override
        public RpcResult<Void> finish() throws IllegalStateException {
            Future<RpcResult<TransactionStatus>> result = backing.commit();
            try {
                RpcResult<TransactionStatus> biResult = result.get();
                return Rpcs.<Void> getRpcResult(biResult.isSuccessful(), null, biResult.getErrors());
            } catch (InterruptedException e) {
                throw new IllegalStateException("", e);
            } catch (ExecutionException e) {
                throw new IllegalStateException("", e);
            } finally {
                domOpenedTransactions.remove(backing.getIdentifier());
            }
        }

        @Override
        public RpcResult<Void> rollback() throws IllegalStateException {
            domOpenedTransactions.remove(backing.getIdentifier());
            return Rpcs.<Void> getRpcResult(true, null, Collections.<RpcError> emptySet());
        }
    }

    private class BindingToDomCommitHandler implements
            DataCommitHandler<InstanceIdentifier<? extends DataObject>, DataObject> {

        @Override
        public org.opendaylight.controller.md.sal.common.api.data.DataCommitHandler.DataCommitTransaction<InstanceIdentifier<? extends DataObject>, DataObject> requestCommit(
                DataModification<InstanceIdentifier<? extends DataObject>, DataObject> bindingTransaction) {

            /**
             * Transaction was created as DOM transaction, in that case we do
             * not need to forward it back.
             */
            if (bindingOpenedTransactions.containsKey(bindingTransaction.getIdentifier())) {

                return CommitHandlersTransactions.allwaysSuccessfulTransaction(bindingTransaction);
            }
            DataModificationTransaction domTransaction = createBindingToDomTransaction(bindingTransaction);
            BindingToDomTransaction wrapped = new BindingToDomTransaction(domTransaction, bindingTransaction);
            LOG.info("Forwarding Binding Transaction: {} as DOM Transaction: {} .", bindingTransaction.getIdentifier(),
                    domTransaction.getIdentifier());
            return wrapped;
        }
    }

    private class DomToBindingCommitHandler implements //
            RegistrationListener<DataCommitHandlerRegistration<InstanceIdentifier<?>, DataObject>>, //
            DataCommitHandler<org.opendaylight.yangtools.yang.data.api.InstanceIdentifier, CompositeNode> {

        @Override
        public void onRegister(DataCommitHandlerRegistration<InstanceIdentifier<?>, DataObject> registration) {

            org.opendaylight.yangtools.yang.data.api.InstanceIdentifier domPath = mappingService.toDataDom(registration
                    .getPath());

        }

        @Override
        public void onUnregister(DataCommitHandlerRegistration<InstanceIdentifier<?>, DataObject> registration) {
            // NOOP for now
            // FIXME: do registration based on only active commit handlers.
        }

        public org.opendaylight.controller.md.sal.common.api.data.DataCommitHandler.DataCommitTransaction<org.opendaylight.yangtools.yang.data.api.InstanceIdentifier, CompositeNode> requestCommit(
                DataModification<org.opendaylight.yangtools.yang.data.api.InstanceIdentifier, CompositeNode> domTransaction) {
            Object identifier = domTransaction.getIdentifier();

            /**
             * We checks if the transcation was originated in this mapper. If it
             * was originated in this mapper we are returing allways success
             * commit hanlder to prevent creating loop in two-phase commit and
             * duplicating data.
             */
            if (domOpenedTransactions.containsKey(identifier)) {
                return CommitHandlersTransactions.allwaysSuccessfulTransaction(domTransaction);
            }

            org.opendaylight.controller.sal.binding.api.data.DataModificationTransaction baTransaction = createDomToBindingTransaction(domTransaction);
            DomToBindingTransaction forwardedTransaction = new DomToBindingTransaction(baTransaction, domTransaction);
            LOG.info("Forwarding DOM Transaction: {} as Binding Transaction: {}.", domTransaction.getIdentifier(),
                    baTransaction.getIdentifier());
            return forwardedTransaction;
        }
    }

    private class DomToBindingRpcForwardingManager implements
            RouteChangeListener<RpcContextIdentifier, InstanceIdentifier<?>> {

        private final Map<Class<? extends RpcService>, DomToBindingRpcForwarder> forwarders = new WeakHashMap<>();

        @Override
        public void onRouteChange(RouteChange<RpcContextIdentifier, InstanceIdentifier<?>> change) {
            for (Entry<RpcContextIdentifier, Set<InstanceIdentifier<?>>> entry : change.getAnnouncements().entrySet()) {
                bindingRoutesAdded(entry);
            }
        }

        private void bindingRoutesAdded(Entry<RpcContextIdentifier, Set<InstanceIdentifier<?>>> entry) {
            Class<? extends BaseIdentity> context = entry.getKey().getRoutingContext();
            Class<? extends RpcService> service = entry.getKey().getRpcService();
            if (context != null) {
                getRpcForwarder(service, context).registerPaths(context, service, entry.getValue());
            }
        }

        private DomToBindingRpcForwarder getRpcForwarder(Class<? extends RpcService> service,
                Class<? extends BaseIdentity> context) {
            DomToBindingRpcForwarder potential = forwarders.get(service);
            if (potential != null) {
                return potential;
            }
            if (context == null) {
                potential = new DomToBindingRpcForwarder(service);
            } else {
                potential = new DomToBindingRpcForwarder(service, context);
            }
            forwarders.put(service, potential);
            return potential;
        }

    }

    private class DomToBindingRpcForwarder implements RpcImplementation {

        private final Set<QName> supportedRpcs;
        private final WeakReference<Class<? extends RpcService>> rpcServiceType;
        private Set<org.opendaylight.controller.sal.core.api.Broker.RoutedRpcRegistration> registrations;

        public DomToBindingRpcForwarder(Class<? extends RpcService> service) {
            this.rpcServiceType = new WeakReference<Class<? extends RpcService>>(service);
            this.supportedRpcs = mappingService.getRpcQNamesFor(service);
            for (QName rpc : supportedRpcs) {
                biRpcRegistry.addRpcImplementation(rpc, this);
            }
            registrations = ImmutableSet.of();
        }

        public DomToBindingRpcForwarder(Class<? extends RpcService> service, Class<? extends BaseIdentity> context) {
            this.rpcServiceType = new WeakReference<Class<? extends RpcService>>(service);
            this.supportedRpcs = mappingService.getRpcQNamesFor(service);
            registrations = new HashSet<>();
            for (QName rpc : supportedRpcs) {
                registrations.add(biRpcRegistry.addRoutedRpcImplementation(rpc, this));
            }
            registrations = ImmutableSet.copyOf(registrations);
        }

        public void registerPaths(Class<? extends BaseIdentity> context, Class<? extends RpcService> service,
                Set<InstanceIdentifier<?>> set) {
            QName ctx = BindingReflections.findQName(context);
            for (org.opendaylight.yangtools.yang.data.api.InstanceIdentifier path : FluentIterable.from(set).transform(
                    toDOMInstanceIdentifier)) {
                for (org.opendaylight.controller.sal.core.api.Broker.RoutedRpcRegistration reg : registrations) {
                    reg.registerPath(ctx, path);
                }
            }
        }

        public void removePaths(Class<? extends BaseIdentity> context, Class<? extends RpcService> service,
                Set<InstanceIdentifier<?>> set) {
            QName ctx = BindingReflections.findQName(context);
            for (org.opendaylight.yangtools.yang.data.api.InstanceIdentifier path : FluentIterable.from(set).transform(
                    toDOMInstanceIdentifier)) {
                for (org.opendaylight.controller.sal.core.api.Broker.RoutedRpcRegistration reg : registrations) {
                    reg.unregisterPath(ctx, path);
                }
            }
        }

        @Override
        public Set<QName> getSupportedRpcs() {
            return supportedRpcs;
        }

        @Override
        public RpcResult<CompositeNode> invokeRpc(QName rpc, CompositeNode domInput) {
            checkArgument(rpc != null);
            checkArgument(domInput != null);

            Class<? extends RpcService> rpcType = rpcServiceType.get();
            checkState(rpcType != null);
            RpcService rpcService = baRpcRegistry.getRpcService(rpcType);
            checkState(rpcService != null);
            CompositeNode domUnwrappedInput = domInput.getFirstCompositeByName(QName.create(rpc, "input"));
            try {
                return resolveInvocationStrategy(rpc, rpcType).invokeOn(rpcService, domUnwrappedInput);
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
        }

        private RpcInvocationStrategy resolveInvocationStrategy(final QName rpc,
                final Class<? extends RpcService> rpcType) throws Exception {
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
                    Optional<Class<?>> outputClass = BindingReflections.resolveRpcOutputClass(targetMethod);
                    Optional<Class<? extends DataContainer>> inputClass = BindingReflections
                            .resolveRpcInputClass(targetMethod);

                    RpcInvocationStrategy strategy = null;
                    if (outputClass.isPresent()) {
                        if (inputClass.isPresent()) {
                            strategy = new DefaultInvocationStrategy(targetMethod, outputClass.get(), inputClass.get());
                        } else {
                            strategy = new NoInputNoOutputInvocationStrategy(targetMethod);
                        }
                    } else {
                        strategy = null;
                    }
                    return strategy;
                }

            });
        }
    }

    private abstract class RpcInvocationStrategy {

        protected final Method targetMethod;

        public RpcInvocationStrategy(Method targetMethod) {
            this.targetMethod = targetMethod;
        }

        public abstract RpcResult<CompositeNode> uncheckedInvoke(RpcService rpcService, CompositeNode domInput)
                throws Exception;

        public RpcResult<CompositeNode> invokeOn(RpcService rpcService, CompositeNode domInput) throws Exception {
            return uncheckedInvoke(rpcService, domInput);
        }
    }

    private class DefaultInvocationStrategy extends RpcInvocationStrategy {

        @SuppressWarnings("rawtypes")
        private WeakReference<Class> inputClass;

        @SuppressWarnings("rawtypes")
        private WeakReference<Class> outputClass;

        public DefaultInvocationStrategy(Method targetMethod, Class<?> outputClass,
                Class<? extends DataContainer> inputClass) {
            super(targetMethod);
            this.outputClass = new WeakReference(outputClass);
            this.inputClass = new WeakReference(inputClass);
        }

        @Override
        public RpcResult<CompositeNode> uncheckedInvoke(RpcService rpcService, CompositeNode domInput) throws Exception {
            DataContainer bindingInput = mappingService.dataObjectFromDataDom(inputClass.get(), domInput);
            Future<RpcResult<?>> result = (Future<RpcResult<?>>) targetMethod.invoke(rpcService, bindingInput);
            if (result == null) {
                return Rpcs.getRpcResult(false);
            }
            RpcResult<?> bindingResult = result.get();
            return Rpcs.getRpcResult(true);
        }

    }

    private class NoInputNoOutputInvocationStrategy extends RpcInvocationStrategy {

        public NoInputNoOutputInvocationStrategy(Method targetMethod) {
            super(targetMethod);
        }

        public RpcResult<CompositeNode> uncheckedInvoke(RpcService rpcService, CompositeNode domInput) throws Exception {
            Future<RpcResult<Void>> result = (Future<RpcResult<Void>>) targetMethod.invoke(rpcService);
            RpcResult<Void> bindingResult = result.get();
            return Rpcs.getRpcResult(bindingResult.isSuccessful(), bindingResult.getErrors());
        }

    }
}
