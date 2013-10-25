/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.dom.broker;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.opendaylight.controller.sal.core.api.Broker;
import org.opendaylight.controller.sal.core.api.BrokerService;
import org.opendaylight.controller.sal.core.api.Consumer;
import org.opendaylight.controller.sal.core.api.Provider;
import org.opendaylight.controller.sal.core.api.RpcImplementation;
import org.opendaylight.controller.sal.core.spi.BrokerModule;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.data.api.CompositeNode;
import org.osgi.framework.BundleContext;
import org.slf4j.LoggerFactory;
import org.opendaylight.yangtools.concepts.ListenerRegistration
import org.opendaylight.controller.sal.core.api.RpcRegistrationListener
import org.opendaylight.controller.md.sal.common.impl.ListenerRegistry

public class BrokerImpl implements Broker {
    private static val log = LoggerFactory.getLogger(BrokerImpl);

    // Broker Generic Context
    private val Set<ConsumerContextImpl> sessions = Collections.synchronizedSet(new HashSet<ConsumerContextImpl>());
    private val Set<ProviderContextImpl> providerSessions = Collections.synchronizedSet(
        new HashSet<ProviderContextImpl>());
    private val Set<BrokerModule> modules = Collections.synchronizedSet(new HashSet<BrokerModule>());
    private val Map<Class<? extends BrokerService>, BrokerModule> serviceProviders = Collections.
        synchronizedMap(new HashMap<Class<? extends BrokerService>, BrokerModule>());


    private val rpcRegistrationListeners = new ListenerRegistry<RpcRegistrationListener>();
    // RPC Context
    private val Map<QName, RpcImplementation> rpcImpls = Collections.synchronizedMap(
        new HashMap<QName, RpcImplementation>());

    // Implementation specific
    @Property
    private var ExecutorService executor = Executors.newFixedThreadPool(5);
    @Property
    private var BundleContext bundleContext;

    override registerConsumer(Consumer consumer, BundleContext ctx) {
        checkPredicates(consumer);
        log.info("Registering consumer " + consumer);
        val session = newSessionFor(consumer, ctx);
        consumer.onSessionInitiated(session);
        sessions.add(session);
        return session;
    }

    override registerProvider(Provider provider, BundleContext ctx) {
        checkPredicates(provider);

        val session = newSessionFor(provider, ctx);
        provider.onSessionInitiated(session);
        providerSessions.add(session);
        return session;
    }

    public def addModule(BrokerModule module) {
        log.info("Registering broker module " + module);
        if(modules.contains(module)) {
            log.error("Module already registered");
            throw new IllegalArgumentException("Module already exists.");
        }

        val provServices = module.getProvidedServices();
        for (Class<? extends BrokerService> serviceType : provServices) {
            log.info("  Registering session service implementation: " + serviceType.getCanonicalName());
            serviceProviders.put(serviceType, module);
        }
    }

    public def <T extends BrokerService> T serviceFor(Class<T> service, ConsumerContextImpl session) {
        val prov = serviceProviders.get(service);
        if(prov == null) {
            log.warn("Service " + service.toString() + " is not supported");
            return null;
        }
        return prov.getServiceForSession(service, session);
    }

    // RPC Functionality
    protected def void addRpcImplementation(QName rpcType, RpcImplementation implementation) {
        if(rpcImpls.get(rpcType) != null) {
            throw new IllegalStateException("Implementation for rpc " + rpcType + " is already registered.");
        }

        
        rpcImpls.put(rpcType, implementation);

        
        for(listener : rpcRegistrationListeners.listeners)  {
            try {
                listener.instance.onRpcImplementationAdded(rpcType);
            } catch (Exception e){
                log.error("Unhandled exception during invoking listener",e);
            }
        }
    }

    protected def void removeRpcImplementation(QName rpcType, RpcImplementation implToRemove) {
        if(implToRemove == rpcImpls.get(rpcType)) {
            rpcImpls.remove(rpcType);
        }
        
        for(listener : rpcRegistrationListeners.listeners)  {
            try {
                listener.instance.onRpcImplementationRemoved(rpcType);
            } catch (Exception e){
                log.error("Unhandled exception during invoking listener",e);
            }
        }
    }

    protected def Future<RpcResult<CompositeNode>> invokeRpc(QName rpc, CompositeNode input) {
        val impl = rpcImpls.get(rpc);
        val result = executor.submit([|impl.invokeRpc(rpc, input)] as Callable<RpcResult<CompositeNode>>);
        return result;
    }

    // Validation
    private def void checkPredicates(Provider prov) {
        if(prov == null)
            throw new IllegalArgumentException("Provider should not be null.");
        for (ProviderContextImpl session : providerSessions) {
            if(prov.equals(session.getProvider()))
                throw new IllegalStateException("Provider already registered");
        }

    }

    private def void checkPredicates(Consumer cons) {
        if(cons == null)
            throw new IllegalArgumentException("Consumer should not be null.");
        for (ConsumerContextImpl session : sessions) {
            if(cons.equals(session.getConsumer()))
                throw new IllegalStateException("Consumer already registered");
        }
    }

    // Private Factory methods
    private def ConsumerContextImpl newSessionFor(Consumer provider, BundleContext ctx) {
        val ret = new ConsumerContextImpl(provider, ctx);
        ret.broker = this;
        return ret;
    }

    private def ProviderContextImpl newSessionFor(Provider provider, BundleContext ctx) {
        val ret = new ProviderContextImpl(provider, ctx);
        ret.broker = this;
        return ret;
    }

    protected def void consumerSessionClosed(ConsumerContextImpl consumerContextImpl) {
        sessions.remove(consumerContextImpl);
        providerSessions.remove(consumerContextImpl);
    }
    
    protected def getSupportedRpcs() {
        rpcImpls.keySet;
    }
    
    def ListenerRegistration<RpcRegistrationListener> addRpcRegistrationListener(RpcRegistrationListener listener) {
        rpcRegistrationListeners.register(listener);
    }
}
