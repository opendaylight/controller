/*
 * Copyright (c) 2020 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.messagebus.app.impl;

import com.google.common.annotations.Beta;
import org.opendaylight.controller.messagebus.spi.EventSource;
import org.opendaylight.controller.messagebus.spi.EventSourceRegistration;
import org.opendaylight.controller.messagebus.spi.EventSourceRegistry;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.binding.api.RpcConsumerRegistry;
import org.opendaylight.mdsal.binding.api.RpcProviderService;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Beta
@Component(immediate = true)
@Deprecated(forRemoval = true)
public final class OSGiEventSourceRegistry implements EventSourceRegistry {
    private static final Logger LOG = LoggerFactory.getLogger(OSGiEventSourceRegistry.class);

    @Reference
    DataBroker dataBroker;
    @Reference
    RpcConsumerRegistry rpcConsumerRegistry;
    @Reference
    RpcProviderService rpcProviderService;

    private EventSourceTopology delegate;

    @Override
    public <T extends EventSource> EventSourceRegistration<T> registerEventSource(final T eventSource) {
        return delegate.registerEventSource(eventSource);
    }

    @Override
    public void close() {
        // Intentiational no-op
    }

    @Activate
    void activate() {
        delegate = new EventSourceTopology(dataBroker, rpcProviderService, rpcConsumerRegistry);
        LOG.info("Event Source Registry started");
    }

    @Deactivate
    void deactivate() {
        LOG.info("Event Source Registry stopping");
        delegate.close();
        LOG.info("Event Source Registry stopped");
    }
}
