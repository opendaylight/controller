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
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker.RoutedRpcRegistration
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker.RpcRegistration
import org.opendaylight.controller.sal.binding.api.data.DataProviderService
import org.opendaylight.controller.sal.binding.api.data.DataBrokerService
import org.opendaylight.controller.sal.binding.spi.RpcRouter
import java.util.concurrent.ConcurrentHashMap
import static com.google.common.base.Preconditions.*
import org.opendaylight.yangtools.concepts.AbstractObjectRegistration
import org.opendaylight.yangtools.yang.binding.BaseIdentity
import com.google.common.collect.Multimap
import com.google.common.collect.HashMultimap
import static org.opendaylight.controller.sal.binding.impl.util.ClassLoaderUtils.*
import java.util.concurrent.Executors
import java.util.Collections
import org.opendaylight.yangtools.yang.binding.DataObject
import java.util.concurrent.locks.ReentrantLock
import java.util.concurrent.Callable
import java.util.WeakHashMap
import javax.annotation.concurrent.GuardedBy

class BindingAwareBrokerImpl implements BindingAwareBroker, AutoCloseable {
    private static val log = LoggerFactory.getLogger(BindingAwareBrokerImpl)

    private InstanceIdentifier<? extends DataObject> root = InstanceIdentifier.builder().toInstance();

    private static val clsPool = ClassPool.getDefault()
    public static var RuntimeCodeGenerator generator;

    /**
     * Map of all Managed Direct Proxies
     * 
     */
    private val Map<Class<? extends RpcService>, RpcProxyContext> managedProxies = new ConcurrentHashMap();

    /**
     * 
     * Map of all available Rpc Routers
     * 
     * 
     */
    private val Map<Class<? extends RpcService>, RpcRouter<? extends RpcService>> rpcRouters = new WeakHashMap();

    @Property
    private var NotificationProviderService notifyBroker

    @Property
    private var DataProviderService dataBroker

    @Property
    var BundleContext brokerBundleContext

    ServiceRegistration<NotificationProviderService> notifyProviderRegistration

    ServiceRegistration<NotificationService> notifyConsumerRegistration

    ServiceRegistration<DataProviderService> dataProviderRegistration

    ServiceRegistration<DataBrokerService> dataConsumerRegistration

    private val proxyGenerationLock = new ReentrantLock;

    private val routerGenerationLock = new ReentrantLock;

    public new(BundleContext bundleContext) {
        _brokerBundleContext = bundleContext;
    }

    def start() {
        log.info("Starting MD-SAL: Binding Aware Broker");
        initGenerator();

        val executor = Executors.newCachedThreadPool;

        // Initialization of notificationBroker
        log.info("Starting MD-SAL: Binding Aware Notification Broker");

        log.info("Starting MD-SAL: Binding Aware Data Broker");

        log.info("Starting MD-SAL: Binding Aware Data Broker");
        log.info("MD-SAL: Binding Aware Broker Started");
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
    private def <T extends RpcService> getManagedDirectProxy(Class<T> service) {
        var RpcProxyContext existing = null

        if ((existing = managedProxies.get(service)) != null) {
            return existing.proxy
        }
        return withLock(proxyGenerationLock) [ |
            val maybeProxy = managedProxies.get(service);
            if (maybeProxy !== null) {
                return maybeProxy.proxy;
            }
            
            
            val proxyInstance = generator.getDirectProxyFor(service)
            val rpcProxyCtx = new RpcProxyContext(proxyInstance.class)
            val properties = new Hashtable<String, String>()
            rpcProxyCtx.proxy = proxyInstance as RpcService
            properties.salServiceType = SAL_SERVICE_TYPE_CONSUMER_PROXY
            rpcProxyCtx.registration = brokerBundleContext.registerService(service, rpcProxyCtx.proxy as T, properties)
            managedProxies.put(service, rpcProxyCtx)
            return rpcProxyCtx.proxy
        ]
    }

    private static def <T> T withLock(ReentrantLock lock, Callable<T> method) {
        try {
            lock.lock();
            val ret = method.call;
            return ret;
        } finally {
            lock.unlock();
        }
    }

    /**
     * Registers RPC Implementation
     * 
     */
    def <T extends RpcService> registerRpcImplementation(Class<T> type, T service, OsgiProviderContext context,
        Hashtable<String, String> properties) {
        val proxy = getManagedDirectProxy(type)
        checkState(proxy.delegate === null, "The Service for type %s is already registered", type)

        val osgiReg = context.bundleContext.registerService(type, service, properties);
        proxy.delegate = service;
        return new RpcServiceRegistrationImpl<T>(type, service, osgiReg, this);
    }

    def <T extends RpcService> RoutedRpcRegistration<T> registerRoutedRpcImplementation(Class<T> type, T service,
        OsgiProviderContext context) {
        val router = resolveRpcRouter(type);
        checkState(router !== null)
        return new RoutedRpcRegistrationImpl<T>(service, router, this)
    }

    private def <T extends RpcService> RpcRouter<T> resolveRpcRouter(Class<T> type) {

        val router = rpcRouters.get(type);
        if (router !== null) {
            return router as RpcRouter<T>;
        }

        // We created Router
        return withLock(routerGenerationLock) [ |
            val maybeRouter = rpcRouters.get(type);
            if (maybeRouter !== null) {
                return maybeRouter as RpcRouter<T>;
            }
            
            val newRouter = generator.getRouterFor(type);
            checkState(newRouter !== null);
            rpcRouters.put(type, newRouter);
            // We create / update Direct Proxy for router
            val proxy = getManagedDirectProxy(type);
            proxy.delegate = newRouter.invocationProxy
            return newRouter;
        ]

    }

    protected def <T extends RpcService> void registerPath(RoutedRpcRegistrationImpl<T> registration,
        Class<? extends BaseIdentity> context, InstanceIdentifier<? extends Object> path) {

        val router = registration.router;
        val paths = registration.registeredPaths;

        val routingTable = router.getRoutingTable(context)
        checkState(routingTable != null);

        // Updating internal structure of registration
        routingTable.updateRoute(path, registration.instance)

        // Update routing table / send announce to message bus
        val success = paths.put(context, path);
    }

    protected def <T extends RpcService> void unregisterPath(RoutedRpcRegistrationImpl<T> registration,
        Class<? extends BaseIdentity> context, InstanceIdentifier<? extends Object> path) {

        val router = registration.router;
        val paths = registration.registeredPaths;

        val routingTable = router.getRoutingTable(context)
        checkState(routingTable != null);

        // Updating internal structure of registration
        val target = routingTable.getRoute(path)
        checkState(target === registration.instance)
        routingTable.removeRoute(path)
        checkState(paths.remove(context, path));
    }

    protected def <T extends RpcService> void unregisterRoutedRpcService(RoutedRpcRegistrationImpl<T> registration) {

        val router = registration.router;
        val paths = registration.registeredPaths;

        for (ctxMap : registration.registeredPaths.entries) {
            val context = ctxMap.key
            val routingTable = router.getRoutingTable(context)
            val path = ctxMap.value
            routingTable.removeRoute(path)
        }
    }

    protected def <T extends RpcService> void unregisterRpcService(RpcServiceRegistrationImpl<T> registration) {

        val type = registration.serviceType;

        val proxy = managedProxies.get(type);
        if (proxy.proxy.delegate === registration.instance) {
            proxy.proxy.delegate = null;
        }
    }

    def createDelegate(Class<? extends RpcService> type) {
        getManagedDirectProxy(type);
    }

    def getRpcRouters() {
        return Collections.unmodifiableMap(rpcRouters);
    }

    override close() {
        dataConsumerRegistration.unregister()
        dataProviderRegistration.unregister()
        notifyConsumerRegistration.unregister()
        notifyProviderRegistration.unregister()
    }

}

class RoutedRpcRegistrationImpl<T extends RpcService> extends AbstractObjectRegistration<T> implements RoutedRpcRegistration<T> {

    @Property
    private val BindingAwareBrokerImpl broker;

    @Property
    private val RpcRouter<T> router;

    @Property
    private val Multimap<Class<? extends BaseIdentity>, InstanceIdentifier<?>> registeredPaths = HashMultimap.create();

    private var closed = false;

    new(T instance, RpcRouter<T> backingRouter, BindingAwareBrokerImpl broker) {
        super(instance)
        _router = backingRouter;
        _broker = broker;
    }

    override protected removeRegistration() {
        closed = true
        broker.unregisterRoutedRpcService(this)
    }

    override registerInstance(Class<? extends BaseIdentity> context, InstanceIdentifier<? extends Object> instance) {
        registerPath(context, instance);
    }

    override unregisterInstance(Class<? extends BaseIdentity> context, InstanceIdentifier<? extends Object> instance) {
        unregisterPath(context, instance);
    }

    override registerPath(Class<? extends BaseIdentity> context, InstanceIdentifier<? extends Object> path) {
        checkClosed()
        broker.registerPath(this, context, path);
    }

    override unregisterPath(Class<? extends BaseIdentity> context, InstanceIdentifier<? extends Object> path) {
        checkClosed()
        broker.unregisterPath(this, context, path);
    }

    override getServiceType() {
        return router.serviceType;
    }

    private def checkClosed() {
        if (closed)
            throw new IllegalStateException("Registration was closed.");
    }

}

class RpcServiceRegistrationImpl<T extends RpcService> extends AbstractObjectRegistration<T> implements RpcRegistration<T> {

    val ServiceRegistration<T> osgiRegistration;
    private var BindingAwareBrokerImpl broker;

    @Property
    val Class<T> serviceType;

    public new(Class<T> type, T service, ServiceRegistration<T> osgiReg, BindingAwareBrokerImpl broker) {
        super(service);
        this._serviceType = type;
        this.osgiRegistration = osgiReg;
        this.broker = broker;
    }

    override protected removeRegistration() {
        broker.unregisterRpcService(this);
        broker = null;
    }

}
