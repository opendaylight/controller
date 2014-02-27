/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.binding.impl.connect.dom;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;

import java.lang.ref.WeakReference;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
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
import org.opendaylight.controller.md.sal.common.api.data.DataReader;
import org.opendaylight.controller.md.sal.common.api.routing.RouteChange;
import org.opendaylight.controller.md.sal.common.api.routing.RouteChangeListener;
import org.opendaylight.controller.md.sal.common.api.routing.RouteChangePublisher;
import org.opendaylight.controller.sal.binding.api.NotificationProviderService;
import org.opendaylight.controller.sal.binding.api.NotificationProviderService.NotificationInterestListener;
import org.opendaylight.controller.sal.binding.api.RpcProviderRegistry;
import org.opendaylight.controller.sal.binding.api.data.DataProviderService;
import org.opendaylight.controller.sal.binding.api.data.RuntimeDataProvider;
import org.opendaylight.controller.sal.binding.api.rpc.RpcContextIdentifier;
import org.opendaylight.controller.sal.binding.api.rpc.RpcRouter;
import org.opendaylight.controller.sal.binding.impl.RpcProviderRegistryImpl;
import org.opendaylight.controller.sal.binding.impl.RpcProviderRegistryImpl.GlobalRpcRegistrationListener;
import org.opendaylight.controller.sal.binding.impl.RpcProviderRegistryImpl.RouterInstantiationListener;
import org.opendaylight.controller.sal.common.util.CommitHandlerTransactions;
import org.opendaylight.controller.sal.common.util.Rpcs;
import org.opendaylight.controller.sal.core.api.Broker.ProviderSession;
import org.opendaylight.controller.sal.core.api.Broker.RoutedRpcRegistration;
import org.opendaylight.controller.sal.core.api.Provider;
import org.opendaylight.controller.sal.core.api.RpcImplementation;
import org.opendaylight.controller.sal.core.api.RpcProvisionRegistry;
import org.opendaylight.controller.sal.core.api.data.DataModificationTransaction;
import org.opendaylight.controller.sal.core.api.notify.NotificationListener;
import org.opendaylight.controller.sal.core.api.notify.NotificationPublishService;
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
import org.opendaylight.yangtools.yang.binding.Notification;
import org.opendaylight.yangtools.yang.binding.RpcService;
import org.opendaylight.yangtools.yang.binding.util.BindingReflections;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.common.RpcError;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.data.api.CompositeNode;
import org.opendaylight.yangtools.yang.data.api.Node;
import org.opendaylight.yangtools.yang.data.impl.ImmutableCompositeNode;
import org.opendaylight.yangtools.yang.data.impl.codec.BindingIndependentMappingService;
import org.opendaylight.yangtools.yang.data.impl.codec.DeserializationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSet.Builder;
import com.google.common.util.concurrent.Futures;

public class BindingIndependentConnector implements //
        RuntimeDataProvider, //
        Provider, //
        AutoCloseable {



    private final Logger LOG = LoggerFactory.getLogger(BindingIndependentConnector.class);

    @SuppressWarnings("deprecation")
    private static final InstanceIdentifier<? extends DataObject> ROOT = InstanceIdentifier.builder().toInstance();

    private static final org.opendaylight.yangtools.yang.data.api.InstanceIdentifier ROOT_BI = org.opendaylight.yangtools.yang.data.api.InstanceIdentifier
            .builder().toInstance();

    private final static Method EQUALS_METHOD;

    private BindingIndependentMappingService mappingService;

    private org.opendaylight.controller.sal.core.api.data.DataProviderService biDataService;

    private DataProviderService baDataService;

    private final ConcurrentMap<Object, BindingToDomTransaction> domOpenedTransactions = new ConcurrentHashMap<>();
    private final ConcurrentMap<Object, DomToBindingTransaction> bindingOpenedTransactions = new ConcurrentHashMap<>();

    private final BindingToDomCommitHandler bindingToDomCommitHandler = new BindingToDomCommitHandler();
    private final DomToBindingCommitHandler domToBindingCommitHandler = new DomToBindingCommitHandler();

    private Registration<DataCommitHandler<InstanceIdentifier<? extends DataObject>, DataObject>> baCommitHandlerRegistration;

    private Registration<DataCommitHandler<org.opendaylight.yangtools.yang.data.api.InstanceIdentifier, CompositeNode>> biCommitHandlerRegistration;

    private RpcProvisionRegistry biRpcRegistry;
    private RpcProviderRegistry baRpcRegistry;

    private ListenerRegistration<DomToBindingRpcForwardingManager> domToBindingRpcManager;
    // private ListenerRegistration<BindingToDomRpcForwardingManager>
    // bindingToDomRpcManager;

    private final Function<InstanceIdentifier<?>, org.opendaylight.yangtools.yang.data.api.InstanceIdentifier> toDOMInstanceIdentifier = new Function<InstanceIdentifier<?>, org.opendaylight.yangtools.yang.data.api.InstanceIdentifier>() {

        @Override
        public org.opendaylight.yangtools.yang.data.api.InstanceIdentifier apply(InstanceIdentifier<?> input) {
            return mappingService.toDataDom(input);
        }

    };

    private Registration<DataReader<InstanceIdentifier<? extends DataObject>, DataObject>> baDataReaderRegistration;

    private boolean rpcForwarding = false;

    private boolean dataForwarding = false;

    private boolean notificationForwarding = false;

    private RpcProviderRegistryImpl baRpcRegistryImpl;

    private NotificationProviderService baNotifyService;

    private NotificationPublishService domNotificationService;

    static {
        try {
            EQUALS_METHOD = Object.class.getMethod("equals", Object.class);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public DataObject readOperationalData(InstanceIdentifier<? extends DataObject> path) {
        try {
            org.opendaylight.yangtools.yang.data.api.InstanceIdentifier biPath = mappingService.toDataDom(path);
            CompositeNode result = biDataService.readOperationalData(biPath);
            return potentialAugmentationRead(path, biPath, result);
        } catch (DeserializationException e) {
            throw new IllegalStateException(e);
        }
    }

    private DataObject potentialAugmentationRead(InstanceIdentifier<? extends DataObject> path,
            org.opendaylight.yangtools.yang.data.api.InstanceIdentifier biPath, CompositeNode result)
            throws DeserializationException {
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
    }

    @Override
    public DataObject readConfigurationData(InstanceIdentifier<? extends DataObject> path) {
        try {
            org.opendaylight.yangtools.yang.data.api.InstanceIdentifier biPath = mappingService.toDataDom(path);
            CompositeNode result = biDataService.readConfigurationData(biPath);
            return potentialAugmentationRead(path, biPath, result);
        } catch (DeserializationException e) {
            throw new IllegalStateException(e);
        }
    }

    private DataModificationTransaction createBindingToDomTransaction(
            DataModification<InstanceIdentifier<? extends DataObject>, DataObject> source) {
        DataModificationTransaction target = biDataService.beginTransaction();
        LOG.debug("Created DOM Transaction {} for {},", target.getIdentifier(),source.getIdentifier());
        for (InstanceIdentifier<? extends DataObject> entry : source.getRemovedConfigurationData()) {
            org.opendaylight.yangtools.yang.data.api.InstanceIdentifier biEntry = mappingService.toDataDom(entry);
            target.removeConfigurationData(biEntry);
            LOG.debug("Delete of Binding Configuration Data {} is translated to {}",entry,biEntry);
        }
        for (InstanceIdentifier<? extends DataObject> entry : source.getRemovedOperationalData()) {
            org.opendaylight.yangtools.yang.data.api.InstanceIdentifier biEntry = mappingService.toDataDom(entry);
            target.removeOperationalData(biEntry);
            LOG.debug("Delete of Binding Operational Data {} is translated to {}",entry,biEntry);
        }
        for (Entry<InstanceIdentifier<? extends DataObject>, DataObject> entry : source.getUpdatedConfigurationData()
                .entrySet()) {
            Entry<org.opendaylight.yangtools.yang.data.api.InstanceIdentifier, CompositeNode> biEntry = mappingService
                    .toDataDom(entry);
            target.putConfigurationData(biEntry.getKey(), biEntry.getValue());
            LOG.debug("Update of Binding Configuration Data {} is translated to {}",entry,biEntry);
        }
        for (Entry<InstanceIdentifier<? extends DataObject>, DataObject> entry : source.getUpdatedOperationalData()
                .entrySet()) {
            Entry<org.opendaylight.yangtools.yang.data.api.InstanceIdentifier, CompositeNode> biEntry = mappingService
                    .toDataDom(entry);
            target.putOperationalData(biEntry.getKey(), biEntry.getValue());
            LOG.debug("Update of Binding Operational Data {} is translated to {}",entry,biEntry);
        }

        return target;
    }

    private org.opendaylight.controller.sal.binding.api.data.DataModificationTransaction createDomToBindingTransaction(
            DataModification<org.opendaylight.yangtools.yang.data.api.InstanceIdentifier, CompositeNode> source) {
        org.opendaylight.controller.sal.binding.api.data.DataModificationTransaction target = baDataService
                .beginTransaction();
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
        return target;
    }

    public org.opendaylight.controller.sal.core.api.data.DataProviderService getBiDataService() {
        return biDataService;
    }

    protected void setDomDataService(org.opendaylight.controller.sal.core.api.data.DataProviderService biDataService) {
        this.biDataService = biDataService;
    }

    public DataProviderService getBaDataService() {
        return baDataService;
    }

    protected void setBindingDataService(DataProviderService baDataService) {
        this.baDataService = baDataService;
    }

    public RpcProviderRegistry getRpcRegistry() {
        return baRpcRegistry;
    }

    protected void setBindingRpcRegistry(RpcProviderRegistry rpcRegistry) {
        this.baRpcRegistry = rpcRegistry;
    }

    public void startDataForwarding() {
        checkState(!dataForwarding, "Connector is already forwarding data.");
        baDataReaderRegistration = baDataService.registerDataReader(ROOT, this);
        baCommitHandlerRegistration = baDataService.registerCommitHandler(ROOT, bindingToDomCommitHandler);
        biCommitHandlerRegistration = biDataService.registerCommitHandler(ROOT_BI, domToBindingCommitHandler);
        baDataService.registerCommitHandlerListener(domToBindingCommitHandler);
        dataForwarding = true;
    }

    public void startRpcForwarding() {
        if (baRpcRegistry != null && biRpcRegistry != null && baRpcRegistry instanceof RouteChangePublisher<?, ?>) {
            checkState(!rpcForwarding, "Connector is already forwarding RPCs");
            domToBindingRpcManager = baRpcRegistry.registerRouteChangeListener(new DomToBindingRpcForwardingManager());
            if (baRpcRegistry instanceof RpcProviderRegistryImpl) {
                baRpcRegistryImpl = (RpcProviderRegistryImpl) baRpcRegistry;
                baRpcRegistryImpl.registerRouterInstantiationListener(domToBindingRpcManager.getInstance());
                baRpcRegistryImpl.registerGlobalRpcRegistrationListener(domToBindingRpcManager.getInstance());
            }
            rpcForwarding = true;
        }
    }

    public void startNotificationForwarding() {
        checkState(!notificationForwarding, "Connector is already forwarding notifications.");
        if (baNotifyService != null && domNotificationService != null) {
            baNotifyService.registerInterestListener(new DomToBindingNotificationForwarder());

            notificationForwarding = true;
        }
    }

    protected void setMappingService(BindingIndependentMappingService mappingService) {
        this.mappingService = mappingService;
    }

    @Override
    public Collection<ProviderFunctionality> getProviderFunctionality() {
        return Collections.emptyList();
    }

    @Override
    public void onSessionInitiated(ProviderSession session) {
        setDomDataService(session.getService(org.opendaylight.controller.sal.core.api.data.DataProviderService.class));
        setDomRpcRegistry(session.getService(RpcProvisionRegistry.class));

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

        private final DataModificationTransaction backing;
        private final DataModification<InstanceIdentifier<? extends DataObject>, DataObject> modification;

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

                return CommitHandlerTransactions.allwaysSuccessfulTransaction(bindingTransaction);
            }
            DataModificationTransaction domTransaction = createBindingToDomTransaction(bindingTransaction);
            BindingToDomTransaction wrapped = new BindingToDomTransaction(domTransaction, bindingTransaction);
            LOG.trace("Forwarding Binding Transaction: {} as DOM Transaction: {} .", bindingTransaction.getIdentifier(),
                    domTransaction.getIdentifier());
            return wrapped;
        }
    }

    private class DomToBindingCommitHandler implements //
            RegistrationListener<DataCommitHandlerRegistration<InstanceIdentifier<? extends DataObject>, DataObject>>, //
            DataCommitHandler<org.opendaylight.yangtools.yang.data.api.InstanceIdentifier, CompositeNode> {

        @Override
        public void onRegister(DataCommitHandlerRegistration<InstanceIdentifier<? extends DataObject>, DataObject> registration) {

            org.opendaylight.yangtools.yang.data.api.InstanceIdentifier domPath = mappingService.toDataDom(registration
                    .getPath());

        }

        @Override
        public void onUnregister(DataCommitHandlerRegistration<InstanceIdentifier<? extends DataObject>, DataObject> registration) {
            // NOOP for now
            // FIXME: do registration based on only active commit handlers.
        }

        @Override
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
                return CommitHandlerTransactions.allwaysSuccessfulTransaction(domTransaction);
            }

            org.opendaylight.controller.sal.binding.api.data.DataModificationTransaction baTransaction = createDomToBindingTransaction(domTransaction);
            DomToBindingTransaction forwardedTransaction = new DomToBindingTransaction(baTransaction, domTransaction);
            LOG.trace("Forwarding DOM Transaction: {} as Binding Transaction: {}.", domTransaction.getIdentifier(),
                    baTransaction.getIdentifier());
            return forwardedTransaction;
        }
    }

    /**
     * Manager responsible for instantiating forwarders responsible for
     * forwarding of RPC invocations from DOM Broker to Binding Aware Broker
     *
     */
    private class DomToBindingRpcForwardingManager implements
            RouteChangeListener<RpcContextIdentifier, InstanceIdentifier<?>>,
            RouterInstantiationListener,
            GlobalRpcRegistrationListener {

        private final Map<Class<? extends RpcService>, DomToBindingRpcForwarder> forwarders = new WeakHashMap<>();
        private RpcProviderRegistryImpl registryImpl;

        public RpcProviderRegistryImpl getRegistryImpl() {
            return registryImpl;
        }

        public void setRegistryImpl(RpcProviderRegistryImpl registryImpl) {
            this.registryImpl = registryImpl;
        }

        @Override
        public void onGlobalRpcRegistered(Class<? extends RpcService> cls) {
            getRpcForwarder(cls, null);
        }

        @Override
        public void onGlobalRpcUnregistered(Class<? extends RpcService> cls) {
            // NOOP
        }

        @Override
        public void onRpcRouterCreated(RpcRouter<?> router) {
            Class<? extends BaseIdentity> ctx = router.getContexts().iterator().next();
            getRpcForwarder(router.getServiceType(), ctx);
        }

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

    private class DomToBindingRpcForwarder implements RpcImplementation, InvocationHandler {

        private final Set<QName> supportedRpcs;
        private final WeakReference<Class<? extends RpcService>> rpcServiceType;
        private final Set<org.opendaylight.controller.sal.core.api.Broker.RoutedRpcRegistration> registrations;
        private final Map<QName, RpcInvocationStrategy> strategiesByQName = new HashMap<>();
        private final WeakHashMap<Method, RpcInvocationStrategy> strategiesByMethod = new WeakHashMap<>();

        public DomToBindingRpcForwarder(Class<? extends RpcService> service) {
            this.rpcServiceType = new WeakReference<Class<? extends RpcService>>(service);
            this.supportedRpcs = mappingService.getRpcQNamesFor(service);
            try {
                for (QName rpc : supportedRpcs) {
                    RpcInvocationStrategy strategy = createInvocationStrategy(rpc, service);
                    strategiesByMethod.put(strategy.targetMethod, strategy);
                    strategiesByQName.put(rpc, strategy);
                    biRpcRegistry.addRpcImplementation(rpc, this);
                }

            } catch (Exception e) {
                LOG.error("Could not forward Rpcs of type {}", service.getName(),e);
            }
            registrations = ImmutableSet.of();
        }

        /**
         * Constructor for Routed RPC Forwareder.
         *
         * @param service
         * @param context
         */
        public DomToBindingRpcForwarder(Class<? extends RpcService> service, Class<? extends BaseIdentity> context) {
            this.rpcServiceType = new WeakReference<Class<? extends RpcService>>(service);
            this.supportedRpcs = mappingService.getRpcQNamesFor(service);
            Builder<RoutedRpcRegistration> registrationsBuilder = ImmutableSet
                    .<org.opendaylight.controller.sal.core.api.Broker.RoutedRpcRegistration> builder();
            try {
                for (QName rpc : supportedRpcs) {
                    RpcInvocationStrategy strategy = createInvocationStrategy(rpc, service);
                    strategiesByMethod.put(strategy.targetMethod, strategy);
                    strategiesByQName.put(rpc, strategy);
                    registrationsBuilder.add(biRpcRegistry.addRoutedRpcImplementation(rpc, this));
                }
                createDefaultDomForwarder();
            } catch (Exception e) {
                LOG.error("Could not forward Rpcs of type {}", service.getName(), e);
            }
            registrations = registrationsBuilder.build();
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


        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
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
        public RpcResult<CompositeNode> invokeRpc(QName rpc, CompositeNode domInput) {
            checkArgument(rpc != null);
            checkArgument(domInput != null);

            Class<? extends RpcService> rpcType = rpcServiceType.get();
            checkState(rpcType != null);
            RpcService rpcService = baRpcRegistry.getRpcService(rpcType);
            checkState(rpcService != null);
            CompositeNode domUnwrappedInput = domInput.getFirstCompositeByName(QName.create(rpc, "input"));
            try {
                return resolveInvocationStrategy(rpc).invokeOn(rpcService, domUnwrappedInput);
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
        }

        private RpcInvocationStrategy resolveInvocationStrategy(QName rpc) {
            return strategiesByQName.get(rpc);
        }

        private RpcInvocationStrategy createInvocationStrategy(final QName rpc,
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
                            strategy = new DefaultInvocationStrategy(rpc, targetMethod, outputClass.get(), inputClass
                                    .get());
                        } else {
                            strategy = new NoInputNoOutputInvocationStrategy(rpc, targetMethod);
                        }
                    } else if(inputClass.isPresent()){
                        strategy = new NoOutputInvocationStrategy(rpc,targetMethod, inputClass.get());
                    } else {
                        strategy = new NoInputNoOutputInvocationStrategy(rpc,targetMethod);
                    }
                    return strategy;
                }

            });
        }
    }

    private abstract class RpcInvocationStrategy {

        protected final Method targetMethod;
        protected final QName rpc;

        public RpcInvocationStrategy(QName rpc, Method targetMethod) {
            this.targetMethod = targetMethod;
            this.rpc = rpc;
        }

        public abstract Future<RpcResult<?>> forwardToDomBroker(DataObject input);

        public abstract RpcResult<CompositeNode> uncheckedInvoke(RpcService rpcService, CompositeNode domInput)
                throws Exception;

        public RpcResult<CompositeNode> invokeOn(RpcService rpcService, CompositeNode domInput) throws Exception {
            return uncheckedInvoke(rpcService, domInput);
        }
    }

    private class DefaultInvocationStrategy extends RpcInvocationStrategy {

        @SuppressWarnings("rawtypes")
        private final WeakReference<Class> inputClass;

        @SuppressWarnings("rawtypes")
        private final WeakReference<Class> outputClass;

        @SuppressWarnings({ "rawtypes", "unchecked" })
        public DefaultInvocationStrategy(QName rpc, Method targetMethod, Class<?> outputClass,
                Class<? extends DataContainer> inputClass) {
            super(rpc, targetMethod);
            this.outputClass = new WeakReference(outputClass);
            this.inputClass = new WeakReference(inputClass);
        }

        @SuppressWarnings("unchecked")
        @Override
        public RpcResult<CompositeNode> uncheckedInvoke(RpcService rpcService, CompositeNode domInput) throws Exception {
            DataContainer bindingInput = mappingService.dataObjectFromDataDom(inputClass.get(), domInput);
            Future<RpcResult<?>> futureResult = (Future<RpcResult<?>>) targetMethod.invoke(rpcService, bindingInput);
            if (futureResult == null) {
                return Rpcs.getRpcResult(false);
            }
            RpcResult<?> bindingResult = futureResult.get();
            final Object resultObj = bindingResult.getResult();
            if (resultObj instanceof DataObject) {
                final CompositeNode output = mappingService.toDataDom((DataObject)resultObj);
                return Rpcs.getRpcResult(true, output, Collections.<RpcError>emptySet());
            }
            return Rpcs.getRpcResult(true);
        }

        @Override
        public Future<RpcResult<?>> forwardToDomBroker(DataObject input) {
            if(biRpcRegistry != null) {
                CompositeNode xml = mappingService.toDataDom(input);
                CompositeNode wrappedXml = ImmutableCompositeNode.create(rpc, ImmutableList.<Node<?>> of(xml));
                RpcResult<CompositeNode> result = biRpcRegistry.invokeRpc(rpc, wrappedXml);
                Object baResultValue = null;
                if (result.getResult() != null) {
                    baResultValue = mappingService.dataObjectFromDataDom(outputClass.get(), result.getResult());
                }
                RpcResult<?> baResult = Rpcs.getRpcResult(result.isSuccessful(), baResultValue, result.getErrors());
                return Futures.<RpcResult<?>> immediateFuture(baResult);
            }
            return Futures.<RpcResult<?>> immediateFuture(Rpcs.getRpcResult(false));
        }

    }

    private class NoInputNoOutputInvocationStrategy extends RpcInvocationStrategy {

        public NoInputNoOutputInvocationStrategy(QName rpc, Method targetMethod) {
            super(rpc, targetMethod);
        }

        @Override
        public RpcResult<CompositeNode> uncheckedInvoke(RpcService rpcService, CompositeNode domInput) throws Exception {
            @SuppressWarnings("unchecked")
            Future<RpcResult<Void>> result = (Future<RpcResult<Void>>) targetMethod.invoke(rpcService);
            RpcResult<Void> bindingResult = result.get();
            return Rpcs.getRpcResult(bindingResult.isSuccessful(), bindingResult.getErrors());
        }

        @Override
        public Future<RpcResult<?>> forwardToDomBroker(DataObject input) {
            return Futures.immediateFuture(null);
        }
    }

    private class NoOutputInvocationStrategy extends RpcInvocationStrategy {


        @SuppressWarnings("rawtypes")
        private final WeakReference<Class> inputClass;

        @SuppressWarnings({ "rawtypes", "unchecked" })
        public NoOutputInvocationStrategy(QName rpc, Method targetMethod,
                Class<? extends DataContainer> inputClass) {
            super(rpc,targetMethod);
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

        @Override
        public Future<RpcResult<?>> forwardToDomBroker(DataObject input) {
            if(biRpcRegistry != null) {
                CompositeNode xml = mappingService.toDataDom(input);
                CompositeNode wrappedXml = ImmutableCompositeNode.create(rpc,ImmutableList.<Node<?>>of(xml));
                RpcResult<CompositeNode> result = biRpcRegistry.invokeRpc(rpc, wrappedXml);
                Object baResultValue = null;
                RpcResult<?> baResult = Rpcs.<Void>getRpcResult(result.isSuccessful(), null, result.getErrors());
                return Futures.<RpcResult<?>>immediateFuture(baResult);
            }
            return Futures.<RpcResult<?>>immediateFuture(Rpcs.getRpcResult(false));
        }

    }

    public boolean isRpcForwarding() {
        return rpcForwarding;
    }

    public boolean isDataForwarding() {
        return dataForwarding;
    }

    public boolean isNotificationForwarding() {
        return notificationForwarding;
    }

    public BindingIndependentMappingService getMappingService() {
        return mappingService;
    }

    public void setBindingNotificationService(NotificationProviderService baService) {
        this.baNotifyService = baService;

    }

    public void setDomNotificationService(NotificationPublishService domService) {
        this.domNotificationService = domService;
    }

    private class DomToBindingNotificationForwarder implements NotificationInterestListener, NotificationListener {

        private final ConcurrentMap<QName, WeakReference<Class<? extends Notification>>> notifications = new ConcurrentHashMap<>();
        private final Set<QName> supportedNotifications = new HashSet<>();

        @Override
        public Set<QName> getSupportedNotifications() {
            return Collections.unmodifiableSet(supportedNotifications);
        }

        @Override
        public void onNotification(CompositeNode notification) {
            QName qname = notification.getNodeType();
            WeakReference<Class<? extends Notification>> potential = notifications.get(qname);
            if (potential != null) {
                Class<? extends Notification> potentialClass = potential.get();
                if (potentialClass != null) {
                    final DataContainer baNotification = mappingService.dataObjectFromDataDom(potentialClass,
                            notification);

                    if (baNotification instanceof Notification) {
                        baNotifyService.publish((Notification) baNotification);
                    }
                }
            }
        }

        @Override
        public void onNotificationSubscribtion(Class<? extends Notification> notificationType) {
            QName qname = BindingReflections.findQName(notificationType);
            if (qname != null) {
                WeakReference<Class<? extends Notification>> already = notifications.putIfAbsent(qname,
                        new WeakReference<Class<? extends Notification>>(notificationType));
                if (already == null) {
                    domNotificationService.addNotificationListener(qname, this);
                    supportedNotifications.add(qname);
                }
            }
        }
    }
}
