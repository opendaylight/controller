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
import org.osgi.framework.BundleContext
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker
import org.opendaylight.controller.sal.binding.api.NotificationProviderService
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker.ProviderContext
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier
import org.opendaylight.yangtools.yang.binding.DataObject
import org.opendaylight.controller.md.sal.common.api.routing.RouteChangeListener
import org.opendaylight.controller.sal.binding.spi.RpcContextIdentifier
import org.opendaylight.controller.sal.binding.api.data.DataProviderService
import org.slf4j.LoggerFactory

class BindingAwareBrokerImpl extends RpcProviderRegistryImpl implements BindingAwareBroker, AutoCloseable {
    private static val log = LoggerFactory.getLogger(BindingAwareBrokerImpl)

    private InstanceIdentifier<? extends DataObject> root = InstanceIdentifier.builder().toInstance();

    @Property
    private var NotificationProviderService notifyBroker

    @Property
    private var DataProviderService dataBroker

    @Property
    var BundleContext brokerBundleContext

    public new(String name,BundleContext bundleContext) {
        super(name);
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

    override <L extends RouteChangeListener<RpcContextIdentifier, InstanceIdentifier<?>>> registerRouteChangeListener(L listener) {
        super.<L>registerRouteChangeListener(listener)
    }
    
    override close() throws Exception {
        
    }
}