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
import javassist.CtMethod
import javassist.CtField
import org.osgi.framework.BundleContext
import java.util.Map
import java.util.HashMap
import javassist.LoaderClassPath
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker
import java.util.Hashtable

import static extension org.opendaylight.controller.sal.binding.impl.utils.PropertiesUtils.*
import static extension org.opendaylight.controller.sal.binding.impl.utils.GeneratorUtils.*
import org.opendaylight.controller.sal.binding.api.NotificationProviderService
import org.osgi.framework.ServiceRegistration
import org.opendaylight.controller.sal.binding.impl.utils.PropertiesUtils
import org.opendaylight.controller.sal.binding.api.NotificationService
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker.ProviderContext
import javassist.Modifier
import org.slf4j.LoggerFactory

class BindingAwareBrokerImpl implements BindingAwareBroker {
    private static val DELEGATE_FIELD = "_delegate"
    private static val log = LoggerFactory.getLogger(BindingAwareBrokerImpl)
    
    private val clsPool = ClassPool.getDefault()
    private Map<Class<? extends RpcService>, RpcProxyContext> managedProxies = new HashMap();
    private var NotificationBrokerImpl notifyBroker
    private var ServiceRegistration<NotificationProviderService> notifyBrokerRegistration

    @Property
    var BundleContext brokerBundleContext

    def start() {
        initGenerator();

        // Initialization of notificationBroker
        notifyBroker = new NotificationBrokerImpl(null);
        val brokerProperties = PropertiesUtils.newProperties();
        notifyBrokerRegistration = brokerBundleContext.registerService(NotificationProviderService, notifyBroker,
            brokerProperties)
        brokerBundleContext.registerService(NotificationService, notifyBroker, brokerProperties)
    }

    def initGenerator() {

        // YANG Binding Class Loader
        clsPool.appendClassPath(new LoaderClassPath(RpcService.classLoader))
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

    def <T extends RpcService> getManagedDirectProxy(Class<T> service) {
        
        var RpcProxyContext existing = null
        if ((existing = managedProxies.get(service)) != null) {
            return existing.proxy
        }
        val proxyClass = service.generateDirectProxy()
        val rpcProxyCtx = new RpcProxyContext(proxyClass)
        val properties = new Hashtable<String, String>()
        rpcProxyCtx.proxy = proxyClass.newInstance as RpcService

        properties.salServiceType = Constants.SAL_SERVICE_TYPE_CONSUMER_PROXY
        rpcProxyCtx.registration = brokerBundleContext.registerService(service, rpcProxyCtx.proxy as T, properties)
        managedProxies.put(service, rpcProxyCtx)
        return rpcProxyCtx.proxy
    }

    protected def generateDirectProxy(Class<? extends RpcService> delegate) {
        val targetFqn = delegate.generatedName(Constants.PROXY_DIRECT_SUFFIX)
        log.debug("Generating DirectProxy for {} Proxy name: {}",delegate,targetFqn);
        val objCls = clsPool.get(Object)
        val delegateCls = clsPool.get(delegate)
        val proxyCls = clsPool.makeClass(targetFqn)
        proxyCls.addInterface(delegateCls)
        val delField = new CtField(delegateCls, DELEGATE_FIELD, proxyCls);
        delField.modifiers = Modifier.PUBLIC
        proxyCls.addField(delField)
        delegateCls.methods.filter[it.declaringClass != objCls].forEach [
            val proxyMethod = new CtMethod(it, proxyCls, null);
            proxyMethod.body = '''return ($r) «DELEGATE_FIELD».«it.name»($$);'''
            proxyCls.addMethod(proxyMethod)
        ]
        return proxyCls.toClass(delegate.classLoader)
    }

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
    
    def <T extends RpcService> getDelegate(RpcService proxy) {
        val field = proxy.class.getField(DELEGATE_FIELD)
        if(field == null) throw new UnsupportedOperationException("Unable to get delegate from proxy");
        return field.get(proxy) as T
    }
    
    def void setDelegate(RpcService proxy, RpcService delegate) {
        val field = proxy.class.getField(DELEGATE_FIELD)
        if(field == null) throw new UnsupportedOperationException("Unable to set delegate to proxy");
        if (field.type.isAssignableFrom(delegate.class)) {
            field.set(proxy,delegate)
        } else throw new IllegalArgumentException("delegate class is not assignable to proxy");
    }
    
    
}
