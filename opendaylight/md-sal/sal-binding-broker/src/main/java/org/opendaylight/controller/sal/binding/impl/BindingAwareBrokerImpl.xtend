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
import org.opendaylight.controller.sal.binding.api.RpcProviderRegistry
import org.opendaylight.yangtools.concepts.ListenerRegistration
import org.opendaylight.yangtools.concepts.util.ListenerRegistry

class BindingAwareBrokerImpl extends RpcProviderRegistryImpl implements BindingAwareBroker, AutoCloseable {
    private static val log = LoggerFactory.getLogger(BindingAwareBrokerImpl)

    private InstanceIdentifier<? extends DataObject> root = InstanceIdentifier.builder().toInstance();

    @Property
    private var NotificationProviderService notifyBroker

    @Property
    private var DataProviderService dataBroker

    @Property
    var BundleContext brokerBundleContext

    public new(BundleContext bundleContext) {
        _brokerBundleContext = bundleContext;
    }

    def start() {
        log.info("Starting MD-SAL: Binding Aware Broker");
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
    
    override close() throws Exception {
        
    }

}