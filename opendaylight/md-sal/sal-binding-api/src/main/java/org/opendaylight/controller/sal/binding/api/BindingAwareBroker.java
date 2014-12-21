/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.binding.api;

import org.opendaylight.controller.md.sal.common.api.routing.RoutedRegistration;
import org.opendaylight.yangtools.concepts.ObjectRegistration;
import org.opendaylight.yangtools.yang.binding.BaseIdentity;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.binding.RpcService;
import org.osgi.framework.BundleContext;

/**
 * Binding-aware core of the SAL layer responsible for wiring the SAL consumers.
 *
 * The responsibility of the broker is to maintain registration of SAL
 * functionality {@link Consumer}s and {@link Provider}s, store provider and
 * consumer specific context and functionality registration via
 * {@link ConsumerContext} and provide access to infrastructure services, which
 * removes direct dependencies between providers and consumers.
 *
 * The Binding-aware broker is also responsible for translation from Java
 * classes modeling the functionality and data to binding-independent form which
 * is used in SAL Core.
 *
 *
 * <h3>Infrastructure services</h3> Some examples of infrastructure services:
 *
 * <ul>
 * <li>YANG Module service - see {@link ConsumerContext#getRpcService(Class)},
 * {@link ProviderContext}
 * <li>Notification Service - see {@link NotificationService} and
 * {@link NotificationProviderService}
 * <li>Functionality and Data model
 * <li>Data Store access and modification - see {@link org.opendaylight.controller.sal.binding.api.data.DataBrokerService} and
 * {@link org.opendaylight.controller.sal.binding.api.data.DataProviderService}
 * </ul>
 *
 * The services are exposed via session.
 *
 * <h3>Session-based access</h3>
 *
 * The providers and consumers needs to register in order to use the
 * binding-independent SAL layer and to expose functionality via SAL layer.
 *
 * For more information about session-based access see {@link ConsumerContext}
 * and {@link ProviderContext}
 */
public interface BindingAwareBroker {
    /*
     * @deprecated Use registerConsumer(BindingAwareConsumer cons) instead (BundleContext is no longer used)
     */
    @Deprecated
    ConsumerContext registerConsumer(BindingAwareConsumer consumer, BundleContext ctx);

    /**
     * Registers the {@link BindingAwareConsumer}, which will use the SAL layer.
     *
     * <p>
     * Note that consumer could register additional functionality at later point
     * by using service and functionality specific APIs.
     *
     * <p>
     * The consumer is required to use returned session for all communication
     * with broker or one of the broker services. The session is announced to
     * the consumer by invoking
     * {@link Consumer#onSessionInitiated(ConsumerContext)}.
     *
     * @param cons
     *            Consumer to be registered.
     * @return a session specific to consumer registration
     * @throws IllegalArgumentException
     *             If the consumer is <code>null</code>.
     * @throws IllegalStateException
     *             If the consumer is already registered.
     */
    ConsumerContext registerConsumer(BindingAwareConsumer consumer);

    /*
     * @deprecated Use registerProvider(BindingAwareProvider prov) instead (BundleContext is no longer used)
     */
    @Deprecated
    ProviderContext registerProvider(BindingAwareProvider provider, BundleContext ctx);

    /**
     * Registers the {@link BindingAwareProvider}, which will use the SAL layer.
     *
     * <p>
     * During the registration, the broker obtains the initial functionality
     * from consumer, using the
     * {@link BindingAwareProvider#getImplementations()}, and register that
     * functionality into system and concrete infrastructure services.
     *
     * <p>
     * Note that provider could register additional functionality at later point
     * by using service and functionality specific APIs.
     *
     * <p>
     * The consumer is <b>required to use</b> returned session for all
     * communication with broker or one of the broker services. The session is
     * announced to the consumer by invoking
     * {@link BindingAwareProvider#onSessionInitiated(ProviderContext)}.
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
    ProviderContext registerProvider(BindingAwareProvider provider);

    /**
     * {@link BindingAwareConsumer} specific access to the SAL functionality.
     *
     * <p>
     * ConsumerSession is {@link BindingAwareConsumer}-specific access to the
     * SAL functionality and infrastructure services.
     *
     * <p>
     * The session serves to store SAL context (e.g. registration of
     * functionality) for the consumer and provides access to the SAL
     * infrastructure services and other functionality provided by
     * {@link Provider}s.
     */
    public interface ConsumerContext extends RpcConsumerRegistry {

        /**
         * Returns a session specific instance (implementation) of requested
         * binding-aware infrastructural service
         *
         * @param service
         *            Broker service
         * @return Session specific implementation of service
         */
        <T extends BindingAwareService> T getSALService(Class<T> service);
    }

    /**
     * {@link BindingAwareProvider} specific access to the SAL functionality.
     *
     * <p>
     * ProviderSession is {@link BindingAwareProvider}-specific access to the
     * SAL functionality and infrastructure services, which also allows for
     * exposing the provider's functionality to the other
     * {@link BindingAwareConsumer}s.
     *
     * <p>
     * The session serves to store SAL context (e.g. registration of
     * functionality) for the providers and exposes access to the SAL
     * infrastructure services, dynamic functionality registration and any other
     * functionality provided by other {@link BindingAwareConsumer}s.
     *
     */
    public interface ProviderContext extends ConsumerContext, RpcProviderRegistry {

    }

    /**
     * Represents an RPC implementation registration. Users should call the
     * {@link ObjectRegistration#close close} method when the registration is no longer needed.
     *
     * @param <T> the implemented RPC service interface
     */
    public interface RpcRegistration<T extends RpcService> extends ObjectRegistration<T> {

        /**
         * Returns the implemented RPC service interface.
         */
        Class<T> getServiceType();

        @Override
        void close();
    }

    /**
     * Represents a routed RPC implementation registration. Users should call the
     * {@link RoutedRegistration#close close} method when the registration is no longer needed.
     *
     * @param <T> the implemented RPC service interface
     */
    public interface RoutedRpcRegistration<T extends RpcService> extends RpcRegistration<T>,
            RoutedRegistration<Class<? extends BaseIdentity>, InstanceIdentifier<?>, T> {

        /**
         * Register particular instance identifier to be processed by this
         * RpcService
         *
         * Deprecated in favor of {@link RoutedRegistration#registerPath(Object, Object)}.
         *
         * @param context
         * @param instance
         */
        @Deprecated
        void registerInstance(Class<? extends BaseIdentity> context, InstanceIdentifier<?> instance);

        /**
         * Unregister particular instance identifier to be processed by this
         * RpcService
         *
         * Deprecated in favor of {@link RoutedRegistration#unregisterPath(Object, Object)}.
         *
         * @param context
         * @param instance
         */
        @Deprecated
        void unregisterInstance(Class<? extends BaseIdentity> context, InstanceIdentifier<?> instance);
    }
}
