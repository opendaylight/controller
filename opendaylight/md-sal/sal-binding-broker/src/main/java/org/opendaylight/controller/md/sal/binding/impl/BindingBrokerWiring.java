/*
 * Copyright (c) 2018 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.binding.impl;

import javassist.ClassPool;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.MountPointService;
import org.opendaylight.controller.md.sal.binding.api.NotificationPublishService;
import org.opendaylight.controller.md.sal.binding.api.NotificationService;
import org.opendaylight.controller.md.sal.binding.compat.HeliumNotificationProviderServiceWithInterestListeners;
import org.opendaylight.controller.md.sal.binding.compat.HeliumRpcProviderRegistry;
import org.opendaylight.controller.md.sal.binding.spi.AdapterFactory;
import org.opendaylight.controller.md.sal.dom.api.DOMDataBroker;
import org.opendaylight.controller.md.sal.dom.api.DOMMountPointService;
import org.opendaylight.controller.md.sal.dom.api.DOMNotificationPublishService;
import org.opendaylight.controller.md.sal.dom.api.DOMNotificationService;
import org.opendaylight.controller.md.sal.dom.api.DOMRpcProviderService;
import org.opendaylight.controller.md.sal.dom.api.DOMRpcService;
import org.opendaylight.controller.md.sal.dom.spi.DOMNotificationSubscriptionListenerRegistry;
import org.opendaylight.controller.sal.binding.api.NotificationProviderService;
import org.opendaylight.controller.sal.binding.api.RpcProviderRegistry;
import org.opendaylight.mdsal.binding.dom.codec.gen.impl.StreamWriterGenerator;
import org.opendaylight.mdsal.binding.dom.codec.impl.BindingNormalizedNodeCodecRegistry;
import org.opendaylight.mdsal.binding.generator.api.ClassLoadingStrategy;
import org.opendaylight.mdsal.binding.generator.util.JavassistUtils;
import org.opendaylight.mdsal.dom.api.DOMSchemaService;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.model.api.SchemaContextListener;

/**
 * Provides the implementations of the APIs.
 *
 * <p>Intended to be usable in a standalone environment (non-OSGi/Karaf). Also
 * internally used by the Blueprint XML to expose the same as OSGi services.
 * This class does not require (depend on) the Guice dependency injection
 * framework, but can we used with it.
 *
 * @author Michael Vorburger.ch, partially based on refactored code originally by Thomas Pantelis
 */
public class BindingBrokerWiring implements AutoCloseable {

    private static final JavassistUtils JAVASSIST = JavassistUtils.forClassPool(ClassPool.getDefault());

    private final BindingToNormalizedNodeCodec bindingToNormalizedNodeCodec;
    private final ListenerRegistration<SchemaContextListener> mappingCodecListenerReg;
    private final RpcProviderRegistry rpcProviderRegistry;
    private final MountPointService mountPointService;
    private final NotificationService notificationService;
    private final NotificationPublishService notificationPublishService;
    private final HeliumNotificationProviderServiceWithInterestListeners notificationAndProviderService;
    private final AdapterFactory adapterFactory;
    private final DataBroker dataBroker;
    private final DataBroker pingPongDataBroker;

    public BindingBrokerWiring(ClassLoadingStrategy classLoadingStrategy, DOMSchemaService schemaService,
            DOMRpcService domRpcService, DOMRpcProviderService domRpcProviderService,
            DOMMountPointService domMountPointService, DOMNotificationService domNotificationService,
            DOMNotificationPublishService domNotificationPublishService,
            DOMNotificationSubscriptionListenerRegistry domNotificationListenerRegistry, DOMDataBroker domDataBroker,
            DOMDataBroker domPingPongDataBroker) {
        // Runtime binding/normalized mapping service
        BindingNormalizedNodeCodecRegistry codecRegistry
            = new BindingNormalizedNodeCodecRegistry(StreamWriterGenerator.create(JAVASSIST));
        bindingToNormalizedNodeCodec = new BindingToNormalizedNodeCodec(classLoadingStrategy, codecRegistry, true);

        // Register the BindingToNormalizedNodeCodec with the SchemaService as a SchemaContextListener
        mappingCodecListenerReg = schemaService.registerSchemaContextListener(bindingToNormalizedNodeCodec);

        // Binding RPC Registry Service
        BindingDOMRpcServiceAdapter bindingDOMRpcServiceAdapter
            = new BindingDOMRpcServiceAdapter(domRpcService, bindingToNormalizedNodeCodec);
        BindingDOMRpcProviderServiceAdapter bindingDOMRpcProviderServiceAdapter
            = new BindingDOMRpcProviderServiceAdapter(domRpcProviderService, bindingToNormalizedNodeCodec);
        rpcProviderRegistry
            = new HeliumRpcProviderRegistry(bindingDOMRpcServiceAdapter, bindingDOMRpcProviderServiceAdapter);

        // Binding MountPoint Service
        mountPointService = new BindingDOMMountPointServiceAdapter(domMountPointService, bindingToNormalizedNodeCodec);

        // Binding Notification Service
        BindingDOMNotificationServiceAdapter notificationServiceImpl = new BindingDOMNotificationServiceAdapter(
                bindingToNormalizedNodeCodec.getCodecRegistry(), domNotificationService);
        notificationService = notificationServiceImpl;
        BindingDOMNotificationPublishServiceAdapter notificationPublishServiceImpl =
                new BindingDOMNotificationPublishServiceAdapter(
                        bindingToNormalizedNodeCodec, domNotificationPublishService);
        notificationPublishService = notificationPublishServiceImpl;
        notificationAndProviderService = new HeliumNotificationProviderServiceWithInterestListeners(
                notificationPublishServiceImpl, notificationServiceImpl, domNotificationListenerRegistry);

        adapterFactory = new BindingToDOMAdapterFactory(bindingToNormalizedNodeCodec);

        // Binding DataBroker
        dataBroker = adapterFactory.createDataBroker(domDataBroker);

        // Binding PingPong DataBroker
        pingPongDataBroker = adapterFactory.createDataBroker(domPingPongDataBroker);
    }

    @Override
    public void close() throws Exception {
        mappingCodecListenerReg.close();
    }

    public BindingToNormalizedNodeCodec getBindingToNormalizedNodeCodec() {
        return bindingToNormalizedNodeCodec;
    }

    public AdapterFactory getAdapterFactory() {
        return adapterFactory;
    }

    public RpcProviderRegistry getRpcProviderRegistry() {
        return rpcProviderRegistry;
    }

    public MountPointService getMountPointService() {
        return mountPointService;
    }

    public NotificationService getNotificationService() {
        return notificationService;
    }

    public NotificationPublishService getNotificationPublishService() {
        return notificationPublishService;
    }

    @Deprecated
    public NotificationProviderService getNotificationProviderService() {
        return notificationAndProviderService;
    }

    @Deprecated
    public org.opendaylight.controller.sal.binding.api.NotificationService getDeprecatedNotificationService() {
        return notificationAndProviderService;
    }

    public DataBroker getDataBroker() {
        return dataBroker;
    }

    public DataBroker getPingPongDataBroker() {
        return pingPongDataBroker;
    }

}
