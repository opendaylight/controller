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
import org.opendaylight.yangtools.restconf.client.api.auth.AuthenticationHolder;
import org.opendaylight.yangtools.restconf.client.api.auth.RestAuthType;
import org.opendaylight.yangtools.sal.binding.generator.impl.ModuleInfoBackedContext;
import org.opendaylight.yangtools.sal.binding.generator.impl.RuntimeGeneratedMappingServiceImpl;
import org.opendaylight.yangtools.yang.binding.util.BindingReflections;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;

public class SalRemoteClientImpl implements SalRemoteClient {

	private static final Logger logger = LoggerFactory.getLogger(SalRemoteClientImpl.class);
	
	private final RestconfClientContext restconfClientContext;
	private final SalRemoteServiceBroker salRemoteBroker;
	private ConsumerContext consumerContext;
		
	public SalRemoteClientImpl(final URL url, final Optional<AuthenticationHolder> authHolderMayBe) {
		Preconditions.checkNotNull(url);
		
        final RuntimeGeneratedMappingServiceImpl mappingService = new RuntimeGeneratedMappingServiceImpl();
        mappingService.setPool(new ClassPool());
        mappingService.init();

        final ModuleInfoBackedContext moduleInfo = ModuleInfoBackedContext.create();
        moduleInfo.addModuleInfos(BindingReflections.loadModuleInfos());
        mappingService.onGlobalContextUpdated(moduleInfo.tryToCreateSchemaContext().get());

        try {
            this.restconfClientContext = new RestconfClientFactory().getRestconfClientContext(url, mappingService,
                    mappingService, authHolderMayBe.or(new DummyAuthProvider()));

            this.salRemoteBroker = new SalRemoteServiceBroker("remote-broker", restconfClientContext);
            this.salRemoteBroker.start();
            this.salRemoteBroker.registerConsumer(new BindingAwareConsumer() {

                @Override
                public void onSessionInitialized(ConsumerContext session) {
                	consumerContext = session;
                }
            }, null);
        } catch (UnsupportedProtocolException e) {
            logger.error("Unsupported protocol {}.", url.getProtocol(), e);
            throw new IllegalArgumentException("Unsupported protocol.", e);
        }	
	}
	
	@Override
	public ConsumerContext getConsumerContext() {
		return this.consumerContext;
	}

	@Override
	public void close() throws Exception {
		this.restconfClientContext.close();
		this.salRemoteBroker.close();
	}
	
	private static final class DummyAuthProvider implements AuthenticationHolder {

		@Override
		public RestAuthType getAuthType() {
			return null;
		}

		@Override
		public String getUserName() {
			return null;
		}

		@Override
		public String getPassword() {
			return null;
		}

		@Override
		public boolean authenticationRequired() {
			return false;
		}
		
	}

}
