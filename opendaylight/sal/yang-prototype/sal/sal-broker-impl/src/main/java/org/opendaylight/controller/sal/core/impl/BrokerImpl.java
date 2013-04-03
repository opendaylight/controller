
/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.core.impl;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Future;

import org.opendaylight.controller.sal.core.api.Broker;
import org.opendaylight.controller.sal.core.api.BrokerService;
import org.opendaylight.controller.sal.core.api.Consumer;
import org.opendaylight.controller.sal.core.api.Provider;
import org.opendaylight.controller.sal.core.spi.BrokerModule;
import org.opendaylight.controller.yang.common.QName;
import org.opendaylight.controller.yang.common.RpcResult;
import org.opendaylight.controller.yang.data.api.CompositeNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class BrokerImpl implements Broker {
    private static Logger log = LoggerFactory.getLogger(BrokerImpl.class);

    private Set<ConsumerSessionImpl> sessions = new HashSet<ConsumerSessionImpl>();
    private Set<ProviderSessionImpl> providerSessions = new HashSet<ProviderSessionImpl>();
    // private ExecutorService executor;
    private Set<BrokerModule> modules = new HashSet<BrokerModule>();

    private Map<Class<? extends BrokerService>, BrokerModule> serviceProviders = new HashMap<Class<? extends BrokerService>, BrokerModule>();

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

    public <T extends BrokerService> T serviceFor(Class<T> service,
            ConsumerSessionImpl session) {
        BrokerModule prov = serviceProviders.get(service);
        if (prov == null) {
            log.warn("Service " + service.toString() + " is not supported");
            return null;
        }
        return prov.getServiceForSession(service, session);
    }

    public Future<RpcResult<CompositeNode>> invokeRpc(QName rpc,
            CompositeNode input) {
        // TODO Implement this method
        throw new UnsupportedOperationException("Not implemented");
    }

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

    private ConsumerSessionImpl newSessionFor(Consumer cons) {
        return new ConsumerSessionImpl(this, cons);
    }

    private ProviderSessionImpl newSessionFor(Provider provider) {
        return new ProviderSessionImpl(this, provider);
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

    public void consumerSessionClosed(ConsumerSessionImpl consumerSessionImpl) {
        sessions.remove(consumerSessionImpl);
        providerSessions.remove(consumerSessionImpl);
    }

}

