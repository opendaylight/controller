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
import org.opendaylight.controller.md.sal.binding.impl.BindingAdapterBuilder.Factory;
import org.opendaylight.controller.md.sal.binding.spi.AdapterBuilder;
import org.opendaylight.controller.md.sal.binding.spi.AdapterLoader;
import org.opendaylight.controller.sal.binding.api.RpcConsumerRegistry;

public abstract class BindingAdapterLoader
        extends AdapterLoader<BindingService, org.opendaylight.mdsal.binding.api.BindingService> {
    private static final Map<Class<?>,BindingAdapterBuilder.Factory<?>> FACTORIES =
        ImmutableMap.<Class<?>, BindingAdapterBuilder.Factory<?>>builder()
            .put(NotificationService.class, BindingNotificationServiceAdapter.BUILDER_FACTORY)
            .put(NotificationPublishService.class, BindingNotificationPublishServiceAdapter.BUILDER_FACTORY)
            .put(DataBroker.class, BindingDataBrokerAdapter.BUILDER_FACTORY)
            .put(RpcConsumerRegistry.class, BindingRpcServiceAdapter.BUILDER_FACTORY)
            .build();

    @Override
    protected final AdapterBuilder<? extends BindingService, org.opendaylight.mdsal.binding.api.BindingService>
            createBuilder(final Class<? extends BindingService> key) {
        final Factory<?> factory = FACTORIES.get(key);
        Preconditions.checkArgument(factory != null, "Unsupported service type %s", key);
        final BindingAdapterBuilder<?> builder = factory.newBuilder();
        return builder;
    }
}
