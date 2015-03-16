/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.core.api;

import java.util.Set;
import java.util.concurrent.Future;

import org.opendaylight.controller.md.sal.common.api.routing.RoutedRegistration;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.concepts.ObjectRegistration;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.data.api.CompositeNode;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.osgi.framework.BundleContext;

/**
 * Core component of the SAL layer responsible for wiring the SAL consumers.
 *
 * The responsibility of the broker is to maintain registration of SAL
 * functionality {@link Consumer}s and {@link Provider}s, store provider and
 * consumer specific context and functionality registration via
 * {@link ConsumerSession} and provide access to infrastructure services, which
 * removes direct dependencies between providers and consumers.
 *
 *
 * <h3>Infrastructure services</h3> Some examples of infrastructure services:
 *
 * <ul>
 * <li>RPC Invocation - see {@link ConsumerSession#rpc(QName, CompositeNode)},
 * {@link ProviderSession#addRpcImplementation(QName, RpcImplementation)} and
 * {@link RpcImplementation}
 * <li>Notification Service - see {@link org.opendaylight.controller.sal.core.api.notify.NotificationService} and
 * {@link org.opendaylight.controller.sal.core.api.notify.NotificationPublishService}
 * <li>Functionality and Data model
 * <li>Data Store access and modification - see {@link org.opendaylight.controller.sal.core.api.data.DataBrokerService} and
 * {@link org.opendaylight.controller.sal.core.api.data.DataProviderService}
 * </ul>
 *
 * The services are exposed via session.
 *
 * <h3>Session-based access</h3>
 *
 * The providers and consumers needs to register in order to use the
 * binding-independent SAL layer and to expose functionality via SAL layer.
 *
 * For more information about session-based access see {@link ConsumerSession}
 * and {@link ProviderSession}
 *
 *
 *
 */
public interface Broker {

    /**
     * Registers the {@link Consumer}, which will use the SAL layer.
     *
     * <p>
     * During the registration, the broker obtains the initial functionality
     * from consumer, using the {@link Consumer#getConsumerFunctionality()}, and
     * register that functionality into system and concrete infrastructure
     * services.
     *
     * <p>
     * Note that consumer could register additional functionality at later point
     * by using service and functionality specific APIs.
     *
     * <p>
     * The consumer is required to use returned session for all communication
     * with broker or one of the broker services. The session is announced to
     * the consumer by invoking
     * {@link Consumer#onSessionInitiated(ConsumerSession)}.
     *
     * @param cons
     *            Consumer to be registered.
     * @return a session specific to consumer registration
     * @throws IllegalArgumentException
     *             If the consumer is <code>null</code>.
     * @throws IllegalStateException
     *             If the consumer is already registered.
     */
    ConsumerSession registerConsumer(Consumer cons);

    /*
     * @deprecated Use registerConsumer(Consumer cons) instead (BundleContext is no longer used)
     */
    @Deprecated
    ConsumerSession registerConsumer(Consumer cons, BundleContext context);

    /**
     * Registers the {@link Provider}, which will use the SAL layer.
     *
     * <p>
     * During the registration, the broker obtains the initial functionality
     * from consumer, using the {@link Provider#getProviderFunctionality()}, and
     * register that functionality into system and concrete infrastructure
     * services.
     *
     * <p>
     * Note that consumer could register additional functionality at later point
     * by using service and functionality specific APIs (e.g.
     * {@link ProviderSession#addRpcImplementation(QName, RpcImplementation)}
     *
     * <p>
     * The consumer is <b>required to use</b> returned session for all
     * communication with broker or one of the broker services. The session is
     * announced to the consumer by invoking
     * {@link Provider#onSessionInitiated(ProviderSession)}.
     *
     *
     * @param prov
     *            Provider to be registered.
     * @return a session unique to the provider registration.
     * @throws IllegalArgumentException
     *             If the provider is <code>null</code>.
     * @throws IllegalStateException
     *             If the consumer is already registered.
     */
    ProviderSession registerProvider(Provider prov);

    /*
     * @deprecated Use registerProvider(Provider cons) instead (BundleContext is no longer used)
     */
    @Deprecated
    ProviderSession registerProvider(Provider prov, BundleContext context);

    /**
     * {@link Consumer} specific access to the SAL functionality.
     *
     * <p>
     * ConsumerSession is {@link Consumer}-specific access to the SAL
     * functionality and infrastructure services.
     *
     * <p>
     * The session serves to store SAL context (e.g. registration of
     * functionality) for the consumer and provides access to the SAL
     * infrastructure services and other functionality provided by
     * {@link Provider}s.
     *
     *
     *
     */
    public interface ConsumerSession {

        boolean isClosed();

        /**
         * Returns a session specific instance (implementation) of requested
         * service
         *
         * @param service
         *            Broker service
         * @return Session specific implementation of service
         */
        <T extends BrokerService> T getService(Class<T> service);

        /**
         * Closes a session between consumer and broker.
         *
         * <p>
         * The close operation unregisters a consumer and remove all registered
         * functionality of the consumer from the system.
         *
         */
        void close();
    }

    /**
     * {@link Provider} specific access to the SAL functionality.
     *
     * <p>
     * ProviderSession is {@link Provider}-specific access to the SAL
     * functionality and infrastructure services, which also allows for exposing
     * the provider's functionality to the other {@link Consumer}s.
     *
     * <p>
     * The session serves to store SAL context (e.g. registration of
     * functionality) for the providers and exposes access to the SAL
     * infrastructure services, dynamic functionality registration and any other
     * functionality provided by other {@link Provider}s.
     *
     */
    public interface ProviderSession extends ConsumerSession {
        /**
         * Closes a session between provider and SAL.
         *
         * <p>
         * The close operation unregisters a provider and remove all registered
         * functionality of the provider from the system.
         */
        @Override
        void close();

        @Override
        boolean isClosed();
    }

    public interface RpcRegistration extends ObjectRegistration<RpcImplementation> {
        QName getType();

        @Override
        void close();
    }

    public interface RoutedRpcRegistration extends RpcRegistration, RoutedRegistration<QName, YangInstanceIdentifier, RpcImplementation> {
    }
}
