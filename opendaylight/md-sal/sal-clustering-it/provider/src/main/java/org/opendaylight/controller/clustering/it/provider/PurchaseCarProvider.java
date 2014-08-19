/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.clustering.it.provider;

import com.google.common.util.concurrent.SettableFuture;
import org.opendaylight.controller.sal.binding.api.NotificationProviderService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.sal.clustering.it.car.purchase.rev140818.BuyCarInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.sal.clustering.it.car.purchase.rev140818.CarBoughtBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.sal.clustering.it.car.purchase.rev140818.CarPurchaseService;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Future;


public class PurchaseCarProvider implements CarPurchaseService, AutoCloseable{

  private static final Logger log = LoggerFactory.getLogger(PurchaseCarProvider.class);

  private NotificationProviderService notificationProvider;


  public void setNotificationProvider(final NotificationProviderService salService) {
    this.notificationProvider = salService;
  }


  @Override
  public Future<RpcResult<Void>> buyCar(BuyCarInput input) {
    log.info("Routed RPC buyCar : generating notification for buying car [{}]", input);
    SettableFuture<RpcResult<Void>> futureResult = SettableFuture.create();
    CarBoughtBuilder carBoughtBuilder = new CarBoughtBuilder();
    carBoughtBuilder.setCarId(input.getCarId());
    carBoughtBuilder.setPersonId(input.getPersonId());
    notificationProvider.publish(carBoughtBuilder.build());
    futureResult.set(RpcResultBuilder.<Void>success().build());
    return futureResult;
  }

  @Override
  public void close() throws Exception {

  }
}
