/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.restconf.broker.client;

import java.net.URL;

import javassist.ClassPool;

import org.opendaylight.controller.sal.binding.api.BindingAwareBroker.ConsumerContext;
import org.opendaylight.controller.sal.binding.api.BindingAwareConsumer;
import org.opendaylight.controller.sal.restconf.broker.SalRemoteServiceBroker;
import org.opendaylight.yangtools.restconf.client.RestconfClientFactory;
import org.opendaylight.yangtools.restconf.client.api.RestconfClientContext;
import org.opendaylight.yangtools.restconf.client.api.UnsupportedProtocolException;
import org.opendaylight.yangtools.sal.binding.generator.impl.ModuleInfoBackedContext;
import org.opendaylight.yangtools.sal.binding.generator.impl.RuntimeGeneratedMappingServiceImpl;
import org.opendaylight.yangtools.yang.binding.Augmentation;
import org.opendaylight.yangtools.yang.binding.util.BindingReflections;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;

class SalRemoteClientImpl implements SalRemoteClient {

    private static final Logger logger = LoggerFactory.getLogger(SalRemoteClientImpl.class);

    private final RestconfClientContext restconfClientContext;
    private final SalRemoteServiceBroker salRemoteBroker;
    private final RuntimeGeneratedMappingServiceImpl mappingService;

    public SalRemoteClientImpl(final URL url) {
        Preconditions.checkNotNull(url);

        this.mappingService = new RuntimeGeneratedMappingServiceImpl();
        this.mappingService.setPool(ClassPool.getDefault());
        this.mappingService.init();

        final ModuleInfoBackedContext moduleInfo = ModuleInfoBackedContext.create();
        moduleInfo.addModuleInfos(BindingReflections.loadModuleInfos());
        this.mappingService.onGlobalContextUpdated(moduleInfo.tryToCreateSchemaContext().get());

        try {
            this.restconfClientContext = new RestconfClientFactory().getRestconfClientContext(url, this.mappingService,
                    this.mappingService);

            this.salRemoteBroker = new SalRemoteServiceBroker("remote-broker", restconfClientContext);
            this.salRemoteBroker.start();
        } catch (UnsupportedProtocolException e) {
            logger.error("Unsupported protocol {}.", url.getProtocol(), e);
            throw new IllegalArgumentException("Unsupported protocol.", e);
        }
    }

    @Override
    public ConsumerContext registerConsumer() {
        return this.salRemoteBroker.registerConsumer(new BindingAwareConsumer() {

            @Override
            public void onSessionInitialized(ConsumerContext session) {
            }
        }, null);
    }

    @Override
    public void close() throws Exception {
        this.restconfClientContext.close();
        this.salRemoteBroker.close();
    }

}
