/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.core.impl;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import org.opendaylight.controller.sal.core.api.Broker;
import org.opendaylight.controller.sal.core.api.BrokerService;
import org.opendaylight.controller.sal.core.api.Consumer;
import org.opendaylight.controller.sal.core.api.Provider;
import org.opendaylight.controller.sal.core.api.RpcImplementation;
import org.opendaylight.controller.sal.core.spi.BrokerModule;
import org.opendaylight.controller.yang.common.QName;
import org.opendaylight.controller.yang.common.RpcResult;
import org.opendaylight.controller.yang.data.api.CompositeNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BrokerImpl implements Broker {
    private static Logger log = LoggerFactory.getLogger(BrokerImpl.class);

    // Broker Generic Context
    private Set<ConsumerSessionImpl> sessions = Collections
            .synchronizedSet(new HashSet<ConsumerSessionImpl>());
    private Set<ProviderSessionImpl> providerSessions = Collections
            .synchronizedSet(new HashSet<ProviderSessionImpl>());
    private Set<BrokerModule> modules = Collections
            .synchronizedSet(new HashSet<BrokerModule>());
    private Map<Class<? extends BrokerService>, BrokerModule> serviceProviders = Collections
            .synchronizedMap(new HashMap<Class<? extends BrokerService>, BrokerModule>());

    // RPC Context
    private Map<QName, RpcImplementation> rpcImpls = Collections
            .synchronizedMap(new HashMap<QName, RpcImplementation>());

    // Implementation specific
    private ExecutorService executor;

    @Override
    public ConsumerSession registerConsumer(Consumer consumer) {
        checkPredicates(consumer);
        log.info("Registering consumer " + consumer);
        ConsumerSessionImpl session = newSessionFor(consumer);
        consumer.onSessionInitiated(session);
        sessions.add(session);
        return session;
    }

    @Override
    public ProviderSession registerProvider(Provider provider) {
        checkPredicates(provider);

        ProviderSessionImpl session = newSessionFor(provider);
        provider.onSessionInitiated(session);
        providerSessions.add(session);
        return session;
    }

    public void addModule(BrokerModule module) {
        log.info("Registering broker module " + module);
        if (modules.contains(module)) {
            log.error("Module already registered");
            throw new IllegalArgumentException("Module already exists.");
        }
    
        Set<Class<? extends BrokerService>> provServices = module
                .getProvidedServices();
        for (Class<? extends BrokerService> serviceType : provServices) {
            log.info("  Registering session service implementation: "
                    + serviceType.getCanonicalName());
            serviceProviders.put(serviceType, module);
        }
    }

    public <T extends BrokerService> T serviceFor(Class<T> service,
            ConsumerSessionImpl session) {
        BrokerModule prov = serviceProviders.get(service);
        if (prov == null) {
            log.warn("Service " + service.toString() + " is not supported");
            return null;
        }
        return prov.getServiceForSession(service, session);
    }

    // RPC Functionality
    
    private void addRpcImplementation(QName rpcType,
            RpcImplementation implementation) {
        synchronized (rpcImpls) {
            if (rpcImpls.get(rpcType) != null) {
                throw new IllegalStateException("Implementation for rpc "
                        + rpcType + " is already registered.");
            }
            rpcImpls.put(rpcType, implementation);
        }
        // TODO Add notification for availability of Rpc Implementation
    }

    private void removeRpcImplementation(QName rpcType,
            RpcImplementation implToRemove) {
        synchronized (rpcImpls) {
            if (implToRemove == rpcImpls.get(rpcType)) {
                rpcImpls.remove(rpcType);
            }
        }
        // TODO Add notification for removal of Rpc Implementation
    }

    private Future<RpcResult<CompositeNode>> invokeRpc(QName rpc,
            CompositeNode input) {
        RpcImplementation impl = rpcImpls.get(rpc);
        // if()

        Callable<RpcResult<CompositeNode>> call = callableFor(impl,
                rpc, input);
        Future<RpcResult<CompositeNode>> result = executor.submit(call);

        return result;
    }
    
    // Validation

    private void checkPredicates(Provider prov) {
        if (prov == null)
            throw new IllegalArgumentException("Provider should not be null.");
        for (ProviderSessionImpl session : providerSessions) {
            if (prov.equals(session.getProvider()))
                throw new IllegalStateException("Provider already registered");
        }

    }

    private void checkPredicates(Consumer cons) {
        if (cons == null)
            throw new IllegalArgumentException("Consumer should not be null.");
        for (ConsumerSessionImpl session : sessions) {
            if (cons.equals(session.getConsumer()))
                throw new IllegalStateException("Consumer already registered");
        }
    }

    // Private Factory methods
    
    private ConsumerSessionImpl newSessionFor(Consumer cons) {
        return new ConsumerSessionImpl(cons);
    }

    private ProviderSessionImpl newSessionFor(Provider provider) {
        return new ProviderSessionImpl(provider);
    }

    private void consumerSessionClosed(ConsumerSessionImpl consumerSessionImpl) {
        sessions.remove(consumerSessionImpl);
        providerSessions.remove(consumerSessionImpl);
    }

    private static Callable<RpcResult<CompositeNode>> callableFor(
            final RpcImplementation implemenation, final QName rpc,
            final CompositeNode input) {

        return new Callable<RpcResult<CompositeNode>>() {

            @Override
            public RpcResult<CompositeNode> call() throws Exception {
                return implemenation.invokeRpc(rpc, input);
            }
        };
    }
    
    private class ConsumerSessionImpl implements ConsumerSession {

        private final Consumer consumer;

        private Map<Class<? extends BrokerService>, BrokerService> instantiatedServices = Collections
                .synchronizedMap(new HashMap<Class<? extends BrokerService>, BrokerService>());
        private boolean closed = false;

        public Consumer getConsumer() {
            return consumer;
        }

        public ConsumerSessionImpl(Consumer consumer) {
            this.consumer = consumer;
        }

        @Override
        public Future<RpcResult<CompositeNode>> rpc(QName rpc,
                CompositeNode input) {
            return BrokerImpl.this.invokeRpc(rpc, input);
        }

        @Override
        public <T extends BrokerService> T getService(Class<T> service) {
            BrokerService potential = instantiatedServices.get(service);
            if (potential != null) {
                @SuppressWarnings("unchecked")
                T ret = (T) potential;
                return ret;
            }
            T ret = BrokerImpl.this.serviceFor(service, this);
            if (ret != null) {
                instantiatedServices.put(service, ret);
            }
            return ret;
        }

        @Override
        public void close() {
            Collection<BrokerService> toStop = instantiatedServices.values();
            this.closed = true;
            for (BrokerService brokerService : toStop) {
                brokerService.closeSession();
            }
            BrokerImpl.this.consumerSessionClosed(this);
        }

        @Override
        public boolean isClosed() {
            return closed;
        }

    }

    private class ProviderSessionImpl extends ConsumerSessionImpl implements
            ProviderSession {

        private Provider provider;
        private Map<QName, RpcImplementation> sessionRpcImpls = Collections.synchronizedMap(new HashMap<QName, RpcImplementation>());

        public ProviderSessionImpl(Provider provider) {
            super(null);
            this.provider = provider;
        }

        @Override
        public void addRpcImplementation(QName rpcType,
                RpcImplementation implementation)
                throws IllegalArgumentException {
            if (rpcType == null) {
                throw new IllegalArgumentException("rpcType must not be null");
            }
            if (implementation == null) {
                throw new IllegalArgumentException(
                        "Implementation must not be null");
            }
            BrokerImpl.this.addRpcImplementation(rpcType, implementation);
            sessionRpcImpls.put(rpcType, implementation);
        }

        @Override
        public void removeRpcImplementation(QName rpcType,
                RpcImplementation implToRemove) throws IllegalArgumentException {
            RpcImplementation localImpl = rpcImpls.get(rpcType);
            if (localImpl != implToRemove) {
                throw new IllegalStateException(
                        "Implementation was not registered in this session");
            }

            BrokerImpl.this.removeRpcImplementation(rpcType, implToRemove);
            sessionRpcImpls.remove(rpcType);
        }

        public Provider getProvider() {
            return this.provider;
        }

    }
}
