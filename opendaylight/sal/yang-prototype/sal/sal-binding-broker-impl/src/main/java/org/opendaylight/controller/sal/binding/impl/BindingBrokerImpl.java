package org.opendaylight.controller.sal.binding.impl;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.opendaylight.controller.sal.binding.api.BindingAwareBroker;
import org.opendaylight.controller.sal.binding.api.BindingAwareConsumer;
import org.opendaylight.controller.sal.binding.api.BindingAwareProvider;
import org.opendaylight.controller.sal.binding.api.BindingAwareService;
import org.opendaylight.controller.sal.binding.spi.SALBindingModule;
import org.opendaylight.controller.yang.binding.RpcService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BindingBrokerImpl implements BindingAwareBroker {

    private static Logger log = LoggerFactory
            .getLogger(BindingBrokerImpl.class);

    private Set<ConsumerSessionImpl> sessions = new HashSet<ConsumerSessionImpl>();
    private Set<ProviderSessionImpl> providerSessions = new HashSet<ProviderSessionImpl>();

    private Set<SALBindingModule> modules = new HashSet<SALBindingModule>();
    private Map<Class<? extends BindingAwareService>, SALBindingModule> salServiceProviders = new HashMap<Class<? extends BindingAwareService>, SALBindingModule>();

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

    private class ConsumerSessionImpl implements
            BindingAwareBroker.ConsumerSession {

        private final BindingAwareConsumer consumer;
        private Map<Class<? extends BindingAwareService>, BindingAwareService> sessionSalServices = new HashMap<Class<? extends BindingAwareService>, BindingAwareService>();

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
        public <T extends RpcService> T getRpcService(Class<T> module) {
            // TODO Implement this method
            throw new UnsupportedOperationException("Not implemented");
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
            // TODO Implement this method
            throw new UnsupportedOperationException("Not implemented");
        }

        @Override
        public void removeRpcImplementation(RpcService implementation) {
            // TODO Implement this method
            throw new UnsupportedOperationException("Not implemented");
        }

        public BindingAwareProvider getProvider() {
            return this.provider;
        }

    }

}
