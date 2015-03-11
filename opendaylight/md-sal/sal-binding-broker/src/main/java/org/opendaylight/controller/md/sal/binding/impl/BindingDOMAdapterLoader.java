/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.binding.impl;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import java.util.Map;
import org.opendaylight.controller.md.sal.binding.api.BindingService;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.NotificationPublishService;
import org.opendaylight.controller.md.sal.binding.api.NotificationService;
import org.opendaylight.controller.md.sal.binding.impl.BindingDOMAdapterBuilder.Factory;
import org.opendaylight.controller.md.sal.binding.spi.AdapterBuilder;
import org.opendaylight.controller.md.sal.binding.spi.AdapterLoader;
import org.opendaylight.controller.md.sal.dom.api.DOMService;
import org.opendaylight.controller.sal.binding.api.RpcConsumerRegistry;

public abstract class BindingDOMAdapterLoader extends AdapterLoader<BindingService, DOMService> {


    private static final Map<Class<?>,BindingDOMAdapterBuilder.Factory<?>> FACTORIES = ImmutableMap.<Class<?>,BindingDOMAdapterBuilder.Factory<?>>builder()
            .put(NotificationService.class,ForwardedNotificationService.BUILDER_FACTORY)
            .put(NotificationPublishService.class,ForwardedNotificationPublishService.BUILDER_FACTORY)
            .put(DataBroker.class,ForwardedBindingDataBroker.BUILDER_FACTORY)
            .put(RpcConsumerRegistry.class,BindingDOMRpcServiceAdapter.BUILDER_FACTORY)
            .build();

    private final BindingToNormalizedNodeCodec codec;

    public BindingDOMAdapterLoader(final BindingToNormalizedNodeCodec codec) {
        super();
        this.codec = codec;
    }

    @Override
    protected final AdapterBuilder<? extends BindingService, DOMService> createBuilder(final Class<? extends BindingService> key)
            throws IllegalArgumentException {
        final Factory<?> factory = FACTORIES.get(key);
        Preconditions.checkArgument(factory != null, "Unsupported service type %s", key);
        final BindingDOMAdapterBuilder<?> builder = factory.newBuilder();
        builder.setCodec(codec);
        return builder;
    }
}
