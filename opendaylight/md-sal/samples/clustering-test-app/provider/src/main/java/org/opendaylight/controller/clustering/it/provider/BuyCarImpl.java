/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.clustering.it.provider;

import static java.util.Objects.requireNonNull;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import java.util.Set;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.opendaylight.mdsal.binding.api.NotificationPublishService;
import org.opendaylight.mdsal.binding.api.RpcProviderService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.sal.clustering.it.car.purchase.rev140818.BuyCar;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.sal.clustering.it.car.purchase.rev140818.BuyCarInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.sal.clustering.it.car.purchase.rev140818.BuyCarOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.sal.clustering.it.car.purchase.rev140818.BuyCarOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.sal.clustering.it.car.purchase.rev140818.CarBoughtBuilder;
import org.opendaylight.yangtools.concepts.Registration;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
@Component(service = { })
public final class BuyCarImpl implements BuyCar, AutoCloseable {
    private static final Logger LOG = LoggerFactory.getLogger(BuyCarImpl.class);

    private final NotificationPublishService notificationProvider;
    private final RpcProviderService rpcProviderService;
    private final Registration reg;

    @Inject
    @Activate
    public BuyCarImpl(@Reference final NotificationPublishService notificationProvider,
            @Reference final RpcProviderService rpcProviderService) {
        this.notificationProvider = requireNonNull(notificationProvider);
        this.rpcProviderService = requireNonNull(rpcProviderService);
        // Add global registration
        reg = rpcProviderService.registerRpcImplementation(this);
    }

    public Registration registerTo(final Set<InstanceIdentifier<?>> paths) {
        return rpcProviderService.registerRpcImplementation(this, paths);
    }

    @Override
    @PreDestroy
    @Deactivate
    public void close() {
        reg.close();
    }

    @Override
    public ListenableFuture<RpcResult<BuyCarOutput>> invoke(final BuyCarInput input) {
        LOG.info("Routed RPC buyCar : generating notification for buying car [{}]", input);
        final var carBought = new CarBoughtBuilder()
            .setCarId(input.getCarId())
            .setPersonId(input.getPersonId())
            .build();
        return Futures.transform(notificationProvider.offerNotification(carBought),
            result -> RpcResultBuilder.success(new BuyCarOutputBuilder().build()).build(),
            MoreExecutors.directExecutor());
    }
}
