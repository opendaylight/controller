/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.binding.impl

import org.opendaylight.controller.sal.binding.api.BindingAwareConsumer
import org.opendaylight.controller.sal.binding.api.BindingAwareProvider
import org.opendaylight.yangtools.yang.binding.RpcService
import javassist.ClassPool
import org.osgi.framework.BundleContext
import java.util.Map
import java.util.HashMap
import javassist.LoaderClassPath
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker
import java.util.Hashtable
import static extension org.opendaylight.controller.sal.binding.codegen.RuntimeCodeHelper.*

import org.opendaylight.controller.sal.binding.api.NotificationProviderService
import org.osgi.framework.ServiceRegistration
import static org.opendaylight.controller.sal.binding.impl.osgi.Constants.*
import static extension org.opendaylight.controller.sal.binding.impl.osgi.PropertiesUtils.*
import org.opendaylight.controller.sal.binding.api.NotificationService
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker.ProviderContext

import org.slf4j.LoggerFactory
import org.opendaylight.controller.sal.binding.codegen.impl.RuntimeCodeGenerator

class BindingAwareBrokerImpl implements BindingAwareBroker {
    private static val log = LoggerFactory.getLogger(BindingAwareBrokerImpl)
    
    private val clsPool = ClassPool.getDefault()
    private var RuntimeCodeGenerator generator;
    private Map<Class<? extends RpcService>, RpcProxyContext> managedProxies = new HashMap();
    private var NotificationBrokerImpl notifyBroker
    private var ServiceRegistration<NotificationProviderService> notifyBrokerRegistration
    
    @Property
    var BundleContext brokerBundleContext

    def start() {
        initGenerator();

        // Initialization of notificationBroker
        notifyBroker = new NotificationBrokerImpl(null);
        val brokerProperties = newProperties();
        notifyBrokerRegistration = brokerBundleContext.registerService(NotificationProviderService, notifyBroker,
            brokerProperties)
        brokerBundleContext.registerService(NotificationService, notifyBroker, brokerProperties)
    }

    def initGenerator() {

        // YANG Binding Class Loader
        clsPool.appendClassPath(new LoaderClassPath(RpcService.classLoader));
        generator = new RuntimeCodeGenerator(clsPool);
    }

    override registerConsumer(BindingAwareConsumer consumer, BundleContext bundleCtx) {
        val ctx = consumer.createContext(bundleCtx)
        consumer.onSessionInitialized(ctx)
        return ctx
    }

    override registerProvider(BindingAwareProvider provider, BundleContext bundleCtx) {
        val ctx = provider.createContext(bundleCtx)
        provider.onSessionInitialized(ctx)
        provider.onSessionInitiated(ctx as ProviderContext)
        return ctx
    }

    private def createContext(BindingAwareConsumer consumer, BundleContext consumerCtx) {
        new OsgiConsumerContext(consumerCtx, this)
    }

    private def createContext(BindingAwareProvider provider, BundleContext providerCtx) {
        new OsgiProviderContext(providerCtx, this)
    }

    /**
     * Returns a Managed Direct Proxy for supplied class
     * 
     * Managed direct proxy is a generated proxy class conforming to the supplied interface
     * which delegates all calls to the backing delegate.
     * 
     * Proxy does not do any validation, null pointer checks or modifies data in any way, it
     * is only use to avoid exposing direct references to backing implementation of service.
     * 
     * If proxy class does not exist for supplied service class it will be generated automatically.
     */
    def <T extends RpcService> getManagedDirectProxy(Class<T> service) {
        
        var RpcProxyContext existing = null
        if ((existing = managedProxies.get(service)) != null) {
            return existing.proxy
        }
        val proxyClass = generator.generateDirectProxy(service)
        val rpcProxyCtx = new RpcProxyContext(proxyClass)
        val properties = new Hashtable<String, String>()
        rpcProxyCtx.proxy = proxyClass.newInstance as RpcService

        properties.salServiceType = SAL_SERVICE_TYPE_CONSUMER_PROXY
        rpcProxyCtx.registration = brokerBundleContext.registerService(service, rpcProxyCtx.proxy as T, properties)
        managedProxies.put(service, rpcProxyCtx)
        return rpcProxyCtx.proxy
    }
    /**
     * Registers RPC Implementation
     * 
     */
    def <T extends RpcService> registerRpcImplementation(Class<T> type, T service, OsgiProviderContext context,
        Hashtable<String, String> properties) {
        val proxy = getManagedDirectProxy(type)
        if(proxy.delegate != null) {
            throw new IllegalStateException("Service " + type + "is already registered");
        }
        val osgiReg = context.bundleContext.registerService(type, service, properties);
        proxy.delegate = service;
        return new RpcServiceRegistrationImpl<T>(type, service, osgiReg);
    }
}
