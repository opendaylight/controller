/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.clustering.it.provider;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import org.opendaylight.mdsal.binding.api.NotificationPublishService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.sal.clustering.it.car.purchase.rev140818.BuyCarInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.sal.clustering.it.car.purchase.rev140818.BuyCarOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.sal.clustering.it.car.purchase.rev140818.BuyCarOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.sal.clustering.it.car.purchase.rev140818.CarBoughtBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.sal.clustering.it.car.purchase.rev140818.CarPurchaseService;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PurchaseCarProvider implements CarPurchaseService, AutoCloseable {

    private static final Logger LOG = LoggerFactory.getLogger(PurchaseCarProvider.class);

    private NotificationPublishService notificationProvider;


    public void setNotificationProvider(final NotificationPublishService salService) {
        this.notificationProvider = salService;
    }


    @Override
    public ListenableFuture<RpcResult<BuyCarOutput>> buyCar(final BuyCarInput input) {
        LOG.info("Routed RPC buyCar : generating notification for buying car [{}]", input);

        return Futures.transform(notificationProvider.offerNotification(new CarBoughtBuilder()
            .setCarId(input.getCarId())
            .setPersonId(input.getPersonId())
            .build()),
            result -> RpcResultBuilder.success(new BuyCarOutputBuilder().build()).build(),
            MoreExecutors.directExecutor());
    }

    @Override
    public void close() {
    }
}
