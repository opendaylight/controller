/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.binding.impl;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.opendaylight.controller.sal.binding.api.BindingAwareBroker;
import org.opendaylight.controller.sal.binding.api.BindingAwareConsumer;
import org.opendaylight.controller.sal.binding.api.BindingAwareProvider;
import org.opendaylight.controller.sal.binding.api.BindingAwareService;
import org.opendaylight.controller.sal.binding.spi.Mapper;
import org.opendaylight.controller.sal.binding.spi.MappingProvider;
import org.opendaylight.controller.sal.binding.spi.RpcMapper;
import org.opendaylight.controller.sal.binding.spi.RpcMapper.RpcProxyInvocationHandler;
import org.opendaylight.controller.sal.binding.spi.SALBindingModule;
import org.opendaylight.controller.sal.common.util.Rpcs;
import org.opendaylight.controller.sal.core.api.Provider;
import org.opendaylight.controller.sal.core.api.RpcImplementation;
import org.opendaylight.controller.yang.binding.DataObject;
import org.opendaylight.controller.yang.binding.RpcService;
import org.opendaylight.controller.yang.common.QName;
import org.opendaylight.controller.yang.common.RpcResult;
import org.opendaylight.controller.yang.data.api.CompositeNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BindingBrokerImpl implements BindingAwareBroker {

    private static Logger log = LoggerFactory
            .getLogger(BindingBrokerImpl.class);

    private Set<ConsumerSessionImpl> sessions = new HashSet<ConsumerSessionImpl>();
    private Set<ProviderSessionImpl> providerSessions = new HashSet<ProviderSessionImpl>();

    private Set<SALBindingModule> modules = new HashSet<SALBindingModule>();
    private Map<Class<? extends BindingAwareService>, SALBindingModule> salServiceProviders = new HashMap<Class<? extends BindingAwareService>, SALBindingModule>();
    private MappingProvider mapping;
    private BIFacade biFacade = new BIFacade();
    private org.opendaylight.controller.sal.core.api.Broker.ProviderSession biSession;
    private ExecutorService executor;

    Map<Class<? extends RpcService>, RpcService> rpcImpls = Collections
            .synchronizedMap(new HashMap<Class<? extends RpcService>, RpcService>());

    private RpcProxyInvocationHandler rpcProxyHandler = new RpcProxyInvocationHandler() {

        @Override
        public Future<RpcResult<? extends DataObject>> invokeRpc(
                RpcService proxy, QName rpc, DataObject input) {
            return rpcProxyInvoked(proxy, rpc, input);
        }
    };

    @Override
    public ConsumerSession registerConsumer(BindingAwareConsumer consumer) {
        checkPredicates(consumer);
        log.info("Registering consumer " + consumer);

        ConsumerSessionImpl session = newSessionFor(consumer);
        consumer.onSessionInitialized(session);

        sessions.add(session);
        return session;
    }

    @Override
    public ProviderSession registerProvider(BindingAwareProvider provider) {
        checkPredicates(provider);

        ProviderSessionImpl session = newSessionFor(provider);
        provider.onSessionInitiated(session);

        providerSessions.add(session);
        return session;
    }

    public void addModule(SALBindingModule module) {
        log.info("Registering broker module " + module);
        if (modules.contains(module)) {
            log.error("Module already registered");
            throw new IllegalArgumentException("Module already exists.");
        }

        Set<Class<? extends BindingAwareService>> provServices = module
                .getProvidedServices();
        for (Class<? extends BindingAwareService> serviceType : provServices) {
            log.info("  Registering session service implementation: "
                    + serviceType.getCanonicalName());
            salServiceProviders.put(serviceType, module);
        }
    }

    public void consumerSessionClosed(ConsumerSessionImpl consumerSessionImpl) {
        sessions.remove(consumerSessionImpl);
        providerSessions.remove(consumerSessionImpl);
    }

    private void checkPredicates(BindingAwareProvider prov) {
        if (prov == null)
            throw new IllegalArgumentException("Provider should not be null.");
        for (ProviderSessionImpl session : providerSessions) {
            if (prov.equals(session.getProvider()))
                throw new IllegalStateException("Provider already registered");
        }

    }

    private void checkPredicates(BindingAwareConsumer cons) {
        if (cons == null)
            throw new IllegalArgumentException("Consumer should not be null.");
        for (ConsumerSessionImpl session : sessions) {
            if (cons.equals(session.getConsumer()))
                throw new IllegalStateException("Consumer already registered");
        }
    }

    private ConsumerSessionImpl newSessionFor(BindingAwareConsumer cons) {
        return new ConsumerSessionImpl(cons);
    }

    private ProviderSessionImpl newSessionFor(BindingAwareProvider provider) {
        return new ProviderSessionImpl(provider);
    }

    private <T extends BindingAwareService> T newSALServiceForSession(
            Class<T> service, ConsumerSession session) {

        SALBindingModule serviceProvider = salServiceProviders.get(service);
        if (serviceProvider == null) {
            return null;
        }
        return serviceProvider.getServiceForSession(service, session);

    }

    private <T extends RpcService> T newRpcProxyForSession(Class<T> service) {

        RpcMapper<T> mapper = mapping.rpcMapperForClass(service);
        if (mapper == null) {
            log.error("Mapper for " + service + "is unavailable.");
            return null;
        }
        T proxy = mapper.getConsumerProxy(rpcProxyHandler);

        return proxy;
    }

    private Future<RpcResult<? extends DataObject>> rpcProxyInvoked(
            RpcService rpcProxy, QName rpcType, DataObject inputData) {
        if (rpcProxy == null) {
            throw new IllegalArgumentException("Proxy must not be null");
        }
        if (rpcType == null) {
            throw new IllegalArgumentException(
                    "rpcType (QName) should not be null");
        }
        Future<RpcResult<? extends DataObject>> ret = null;

        // Real invocation starts here
        RpcMapper<? extends RpcService> mapper = mapping
                .rpcMapperForProxy(rpcProxy);
        RpcService impl = rpcImpls.get(mapper.getServiceClass());

        if (impl == null) {
            // RPC is probably remote
            CompositeNode inputNode = null;
            Mapper<? extends DataObject> inputMapper = mapper.getInputMapper();
            if (inputMapper != null) {
                inputNode = inputMapper.domFromObject(inputData);
            }
            Future<RpcResult<CompositeNode>> biResult = biSession.rpc(rpcType,
                    inputNode);
            ret = new TranslatedFuture(biResult, mapper);

        } else {
            // RPC is local
            Callable<RpcResult<? extends DataObject>> invocation = localRpcCallableFor(
                    impl, mapper, rpcType, inputData);
            ret = executor.submit(invocation);
        }
        return ret;
    }

    private Callable<RpcResult<? extends DataObject>> localRpcCallableFor(
            final RpcService impl,
            final RpcMapper<? extends RpcService> mapper, final QName rpcType,
            final DataObject inputData) {

        return new Callable<RpcResult<? extends DataObject>>() {

            @Override
            public RpcResult<? extends DataObject> call() throws Exception {
                return mapper.invokeRpcImplementation(rpcType, impl, inputData);
            }
        };
    }

    // Binding Independent invocation of Binding Aware RPC
    private RpcResult<CompositeNode> invokeLocalRpc(QName rpc,
            CompositeNode inputNode) {
        RpcMapper<? extends RpcService> mapper = mapping.rpcMapperForData(rpc,
                inputNode);

        DataObject inputTO = mapper.getInputMapper().objectFromDom(inputNode);

        RpcService impl = rpcImpls.get(mapper.getServiceClass());
        if (impl == null) {
            log.warn("Implementation for rpc: " + rpc + "not available.");
        }
        RpcResult<? extends DataObject> result = mapper
                .invokeRpcImplementation(rpc, impl, inputTO);
        DataObject outputTO = result.getResult();

        CompositeNode outputNode = null;
        if (outputTO != null) {
            outputNode = mapper.getOutputMapper().domFromObject(outputTO);
        }
        return Rpcs.getRpcResult(result.isSuccessful(), outputNode,
                result.getErrors());
    }

    private class ConsumerSessionImpl implements
            BindingAwareBroker.ConsumerSession {

        private final BindingAwareConsumer consumer;
        private Map<Class<? extends BindingAwareService>, BindingAwareService> sessionSalServices = Collections
                .synchronizedMap(new HashMap<Class<? extends BindingAwareService>, BindingAwareService>());

        private Map<Class<? extends RpcService>, RpcService> sessionRpcProxies = Collections
                .synchronizedMap(new HashMap<Class<? extends RpcService>, RpcService>());

        public ConsumerSessionImpl(BindingAwareConsumer cons) {
            this.consumer = cons;
        }

        @Override
        public <T extends BindingAwareService> T getSALService(Class<T> service) {

            BindingAwareService serv = sessionSalServices.get(service);
            if (serv != null) {
                if (service.isInstance(serv)) {
                    @SuppressWarnings("unchecked")
                    T ret = (T) serv;
                    return ret;
                } else {
                    log.error("Implementation for service " + service.getName()
                            + " does not implement the service interface");
                    throw new IllegalStateException("Service implementation "
                            + serv.getClass().getName() + "does not implement "
                            + service.getName());
                }
            } else {
                T ret = BindingBrokerImpl.this.newSALServiceForSession(service,
                        this);
                if (ret != null) {
                    sessionSalServices.put(service, ret);
                }
                return ret;
            }
        }

        @Override
        public <T extends RpcService> T getRpcService(Class<T> service) {
            RpcService current = sessionRpcProxies.get(service);
            if (current != null) {
                if (service.isInstance(current)) {
                    @SuppressWarnings("unchecked")
                    T ret = (T) current;
                    return ret;
                } else {
                    log.error("Proxy  for rpc service " + service.getName()
                            + " does not implement the service interface");
                    throw new IllegalStateException("Service implementation "
                            + current.getClass().getName()
                            + "does not implement " + service.getName());
                }
            } else {
                T ret = BindingBrokerImpl.this.newRpcProxyForSession(service);
                if (ret != null) {
                    sessionRpcProxies.put(service, ret);
                }
                return ret;
            }
        }

        public BindingAwareConsumer getConsumer() {
            return this.consumer;
        }

    }

    private class ProviderSessionImpl extends ConsumerSessionImpl implements
            BindingAwareBroker.ProviderSession {

        private final BindingAwareProvider provider;

        public ProviderSessionImpl(BindingAwareProvider provider2) {
            super(null);
            this.provider = provider2;
        }

        @Override
        public void addRpcImplementation(RpcService implementation) {
            if (implementation == null) {
                throw new IllegalArgumentException(
                        "Implementation should not be null");
            }
            // TODO Implement this method
            throw new UnsupportedOperationException("Not implemented");
        }

        @Override
        public void removeRpcImplementation(RpcService implementation) {
            if (implementation == null) {
                throw new IllegalArgumentException(
                        "Implementation should not be null");
            }
            // TODO Implement this method
            throw new UnsupportedOperationException("Not implemented");
        }

        public BindingAwareProvider getProvider() {
            return this.provider;
        }

    }

    private class BIFacade implements Provider,RpcImplementation {

        @Override
        public Set<QName> getSupportedRpcs() {
            return Collections.emptySet();
        }

        @Override
        public RpcResult<CompositeNode> invokeRpc(QName rpc, CompositeNode input) {
            if (rpc == null) {
                throw new IllegalArgumentException(
                        "Rpc type should not be null");
            }

            return BindingBrokerImpl.this.invokeLocalRpc(rpc, input);
        }

        @Override
        public void onSessionInitiated(
                org.opendaylight.controller.sal.core.api.Broker.ProviderSession session) {
            
            BindingBrokerImpl.this.biSession = session;
            for (SALBindingModule module : modules) {
                try {
                    module.onBISessionAvailable(biSession);
                } catch(Exception e) {
                    log.error("Module " +module +" throwed unexpected exception",e);
                }
            }
        }

        @Override
        public Collection<ProviderFunctionality> getProviderFunctionality() {
            return Collections.emptySet();
        }

    }

    private static class TranslatedFuture implements
            Future<RpcResult<? extends DataObject>> {
        private final Future<RpcResult<CompositeNode>> realFuture;
        private final RpcMapper<?> mapper;

        public TranslatedFuture(Future<RpcResult<CompositeNode>> future,
                RpcMapper<?> mapper) {
            realFuture = future;
            this.mapper = mapper;
        }

        @Override
        public boolean cancel(boolean mayInterruptIfRunning) {
            return realFuture.cancel(mayInterruptIfRunning);
        }

        @Override
        public boolean isCancelled() {
            return realFuture.isCancelled();
        }

        @Override
        public boolean isDone() {
            return realFuture.isDone();
        }

        @Override
        public RpcResult<? extends DataObject> get()
                throws InterruptedException, ExecutionException {
            RpcResult<CompositeNode> val = realFuture.get();
            return tranlate(val);
        }

        @Override
        public RpcResult<? extends DataObject> get(long timeout, TimeUnit unit)
                throws InterruptedException, ExecutionException,
                TimeoutException {
            RpcResult<CompositeNode> val = realFuture.get(timeout, unit);
            return tranlate(val);
        }

        private RpcResult<? extends DataObject> tranlate(
                RpcResult<CompositeNode> result) {
            CompositeNode outputNode = result.getResult();
            DataObject outputTO = null;
            if (outputNode != null) {
                Mapper<?> outputMapper = mapper.getOutputMapper();
                outputTO = outputMapper.objectFromDom(outputNode);
            }
            return Rpcs.getRpcResult(result.isSuccessful(), outputTO,
                    result.getErrors());
        }

    }
}
