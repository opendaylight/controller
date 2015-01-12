/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.restconf.broker;


import com.google.common.collect.ImmutableClassToInstanceMap;
import org.opendaylight.controller.md.sal.binding.util.BindingContextUtils;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker;
import org.opendaylight.controller.sal.binding.api.BindingAwareConsumer;
import org.opendaylight.controller.sal.binding.api.BindingAwareProvider;
import org.opendaylight.controller.sal.binding.api.BindingAwareService;
import org.opendaylight.controller.sal.binding.api.NotificationService;
import org.opendaylight.controller.sal.binding.api.RpcConsumerRegistry;
import org.opendaylight.controller.sal.binding.api.data.DataBrokerService;
import org.opendaylight.controller.sal.restconf.broker.impl.RemoteServicesFactory;
import org.opendaylight.yangtools.restconf.client.api.RestconfClientContext;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import static com.google.common.base.Preconditions.checkState;

public class SalRemoteServiceBroker implements BindingAwareBroker,AutoCloseable {


    private static final Logger logger = LoggerFactory.getLogger(SalRemoteServiceBroker.class.toString());
    private ImmutableClassToInstanceMap<BindingAwareService> supportedConsumerServices;

    private final String identifier;

    private RpcConsumerRegistry rpcBroker;
    private NotificationService notificationBroker;
    private DataBrokerService dataBroker;
    private final RemoteServicesFactory servicesFactory;

    public SalRemoteServiceBroker(String instanceName,RestconfClientContext clientContext){
        this.identifier = instanceName;
        this.servicesFactory = new RemoteServicesFactory(clientContext);
    }

    public void start() {
        logger.info("Starting Binding Aware Broker: {}", identifier);

        supportedConsumerServices = ImmutableClassToInstanceMap.<BindingAwareService> builder()
                .put(NotificationService.class, servicesFactory.getNotificationService()) //
                .put(DataBrokerService.class,servicesFactory.getDataBrokerService() ) //
                .put(RpcConsumerRegistry.class,servicesFactory.getRpcConsumerRegistry() ).build();
    }

    public ProviderContext registerProvider(BindingAwareProvider provider) {
        throw new UnsupportedOperationException();
    }
    @Override
    public void close() throws Exception {
        //TODO decide if serviceFactory should close clientContext or it has to be closed by consumer
    }
    @Override
    public ConsumerContext registerConsumer(BindingAwareConsumer consumer) {
        checkState(supportedConsumerServices != null, "Broker is not initialized.");
        return BindingContextUtils.createConsumerContextAndInitialize(consumer, supportedConsumerServices);
    }

    @Override
    public ConsumerContext registerConsumer(BindingAwareConsumer consumer,
            BundleContext ctx) {
        return registerConsumer(consumer);
    }

    @Override
    public ProviderContext registerProvider(BindingAwareProvider provider,
            BundleContext ctx) {
        return registerProvider(provider);
    }

}
