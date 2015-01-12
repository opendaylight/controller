/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.binding.impl.connect.dom;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.opendaylight.controller.md.sal.binding.impl.AbstractForwardedDataBroker;
import org.opendaylight.controller.md.sal.common.api.data.DataReader;
import org.opendaylight.controller.md.sal.common.api.routing.RouteChangePublisher;
import org.opendaylight.controller.sal.binding.api.NotificationProviderService;
import org.opendaylight.controller.sal.binding.api.RpcProviderRegistry;
import org.opendaylight.controller.sal.binding.api.data.DataProviderService;
import org.opendaylight.controller.sal.binding.impl.DataBrokerImpl;
import org.opendaylight.controller.sal.binding.impl.MountPointManagerImpl.BindingMountPointImpl;
import org.opendaylight.controller.sal.binding.impl.RpcProviderRegistryImpl;
import org.opendaylight.controller.sal.core.api.Broker.ProviderSession;
import org.opendaylight.controller.sal.core.api.Provider;
import org.opendaylight.controller.sal.core.api.RpcProvisionRegistry;
import org.opendaylight.controller.sal.core.api.notify.NotificationPublishService;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.concepts.Registration;
import org.opendaylight.yangtools.yang.binding.Augmentable;
import org.opendaylight.yangtools.yang.binding.Augmentation;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.CompositeNode;
import org.opendaylight.yangtools.yang.data.impl.codec.BindingIndependentMappingService;
import org.opendaylight.yangtools.yang.data.impl.codec.DeserializationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BindingIndependentConnector implements //
        DataReader<InstanceIdentifier<? extends DataObject>, DataObject>, //
        Provider, //
        AutoCloseable {

    private static final Logger LOG = LoggerFactory.getLogger(BindingIndependentConnector.class);
    private static final org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier ROOT_BI = org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier
            .builder().toInstance();

    private BindingIndependentMappingService mappingService;
    private org.opendaylight.controller.sal.core.api.data.DataProviderService biDataService;
    private DataProviderService baDataService;

    private final ConcurrentMap<Object, BindingToDomTransaction> domOpenedTransactions;
    private final ConcurrentMap<Object, DomToBindingTransaction> bindingOpenedTransactions;
    private final BindingToDomCommitHandler bindingToDomCommitHandler;
    private final DomToBindingCommitHandler domToBindingCommitHandler;

    private Registration biCommitHandlerRegistration;
    private RpcProvisionRegistry biRpcRegistry;
    private RpcProviderRegistry baRpcRegistry;

    private ListenerRegistration<DomToBindingRpcForwardingManager> domToBindingRpcManager;

    private boolean rpcForwarding;
    private boolean dataForwarding;
    private boolean notificationForwarding;

    private RpcProviderRegistryImpl baRpcRegistryImpl;

    private NotificationProviderService baNotifyService;

    private NotificationPublishService domNotificationService;

    public BindingIndependentConnector() {
        domOpenedTransactions = new ConcurrentHashMap<>();
        bindingOpenedTransactions = new ConcurrentHashMap<>();

        bindingToDomCommitHandler = new BindingToDomCommitHandler(bindingOpenedTransactions, domOpenedTransactions);
        domToBindingCommitHandler = new DomToBindingCommitHandler(bindingOpenedTransactions, domOpenedTransactions);
        rpcForwarding = false;
        dataForwarding = false;
        notificationForwarding = false;
    }

    @Override
    public DataObject readOperationalData(final InstanceIdentifier<? extends DataObject> path) {
        try {
            org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier biPath = mappingService.toDataDom(path);
            CompositeNode result = biDataService.readOperationalData(biPath);
            return potentialAugmentationRead(path, biPath, result);
        } catch (DeserializationException e) {
            throw new IllegalStateException(e);
        }
    }

    private DataObject potentialAugmentationRead(InstanceIdentifier<? extends DataObject> path,
            final org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier biPath, final CompositeNode result)
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
    public DataObject readConfigurationData(final InstanceIdentifier<? extends DataObject> path) {
        try {
            org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier biPath = mappingService.toDataDom(path);
            CompositeNode result = biDataService.readConfigurationData(biPath);
            return potentialAugmentationRead(path, biPath, result);
        } catch (DeserializationException e) {
            throw new IllegalStateException(e);
        }
    }

    public org.opendaylight.controller.sal.core.api.data.DataProviderService getBiDataService() {
        return biDataService;
    }

    protected void setDomDataService(
            final org.opendaylight.controller.sal.core.api.data.DataProviderService biDataService) {
        this.biDataService = biDataService;
        bindingToDomCommitHandler.setBindingIndependentDataService(this.biDataService);
    }

    public DataProviderService getBaDataService() {
        return baDataService;
    }

    protected void setBindingDataService(final DataProviderService baDataService) {
        this.baDataService = baDataService;
        domToBindingCommitHandler.setBindingAwareDataService(this.baDataService);
    }

    public RpcProviderRegistry getRpcRegistry() {
        return baRpcRegistry;
    }

    protected void setBindingRpcRegistry(final RpcProviderRegistry rpcRegistry) {
        this.baRpcRegistry = rpcRegistry;
    }

    public void startDataForwarding() {
        if (baDataService instanceof AbstractForwardedDataBroker) {
            dataForwarding = true;
            return;
        }

        final DataProviderService baData;
        if (baDataService instanceof BindingMountPointImpl) {
            baData = ((BindingMountPointImpl) baDataService).getDataBrokerImpl();
            LOG.debug("Extracted BA Data provider {} from mount point {}", baData, baDataService);
        } else {
            baData = baDataService;
        }

        if (baData instanceof DataBrokerImpl) {
            checkState(!dataForwarding, "Connector is already forwarding data.");
            ((DataBrokerImpl) baData).setDataReadDelegate(this);
            ((DataBrokerImpl) baData).setRootCommitHandler(bindingToDomCommitHandler);
            biCommitHandlerRegistration = biDataService.registerCommitHandler(ROOT_BI, domToBindingCommitHandler);
            baDataService.registerCommitHandlerListener(domToBindingCommitHandler);
        }

        dataForwarding = true;
    }

    //WTF? - cycle references to biFwdManager - need to solve :-/
    public void startRpcForwarding() {
        checkNotNull(mappingService, "Unable to start Rpc forwarding. Reason: Mapping Service is not initialized properly!");
        if (biRpcRegistry != null && baRpcRegistry instanceof RouteChangePublisher<?, ?>) {
            checkState(!rpcForwarding, "Connector is already forwarding RPCs");
            final DomToBindingRpcForwardingManager biFwdManager = new DomToBindingRpcForwardingManager(mappingService, biRpcRegistry, baRpcRegistry);

            domToBindingRpcManager = baRpcRegistry.registerRouteChangeListener(biFwdManager);
            biRpcRegistry.addRpcRegistrationListener(biFwdManager);
            if (baRpcRegistry instanceof RpcProviderRegistryImpl) {
                baRpcRegistryImpl = (RpcProviderRegistryImpl) baRpcRegistry;
                baRpcRegistryImpl.registerRouterInstantiationListener(domToBindingRpcManager.getInstance());
                baRpcRegistryImpl.registerGlobalRpcRegistrationListener(domToBindingRpcManager.getInstance());
                biFwdManager.setRegistryImpl(baRpcRegistryImpl);
            }
            rpcForwarding = true;
        }
    }

    public void startNotificationForwarding() {
        checkState(!notificationForwarding, "Connector is already forwarding notifications.");
        if (mappingService == null) {
            LOG.warn("Unable to start Notification forwarding. Reason: Mapping Service is not initialized properly!");
        } else if (baNotifyService == null) {
            LOG.warn("Unable to start Notification forwarding. Reason: Binding Aware Notify Service is not initialized properly!");
        } else if (domNotificationService == null) {
            LOG.warn("Unable to start Notification forwarding. Reason: DOM Notification Service is not initialized properly!");
        } else {
            baNotifyService.registerInterestListener(
                new DomToBindingNotificationForwarder(mappingService, baNotifyService, domNotificationService));
            notificationForwarding = true;
        }
    }

    protected void setMappingService(final BindingIndependentMappingService mappingService) {
        this.mappingService = mappingService;
        bindingToDomCommitHandler.setMappingService(this.mappingService);
        domToBindingCommitHandler.setMappingService(this.mappingService);
    }

    @Override
    public Collection<ProviderFunctionality> getProviderFunctionality() {
        return Collections.emptyList();
    }

    @Override
    public void onSessionInitiated(final ProviderSession session) {
        setDomDataService(session.getService(org.opendaylight.controller.sal.core.api.data.DataProviderService.class));
        setDomRpcRegistry(session.getService(RpcProvisionRegistry.class));

    }

    public void setDomRpcRegistry(final RpcProvisionRegistry registry) {
        biRpcRegistry = registry;
    }

    @Override
    public void close() throws Exception {
        if (biCommitHandlerRegistration != null) {
            biCommitHandlerRegistration.close();
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

    public void setBindingNotificationService(final NotificationProviderService baService) {
        this.baNotifyService = baService;

    }

    public void setDomNotificationService(final NotificationPublishService domService) {
        this.domNotificationService = domService;
    }
}
