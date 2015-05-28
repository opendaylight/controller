/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.clustering.it.provider;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.SettableFuture;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.sal.clustering.it.car.purchase.rev140818.CarPurchaseService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.sal.clustering.it.cars.rev140818.AddCarInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.sal.clustering.it.cars.rev140818.Cars;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.sal.clustering.it.cars.rev140818.CarsService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.sal.clustering.it.cars.rev140818.CarContext;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.sal.clustering.it.cars.rev140818.cars.Car;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.sal.clustering.it.cars.rev140818.cars.CarBuilder;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.RpcError;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Future;

public class CarsProvider implements CarsService, AutoCloseable {

  private static final Logger log = LoggerFactory.getLogger(CarsProvider.class);

  private DataBroker dataProvider;

  private BindingAwareBroker.RoutedRpcRegistration<CarPurchaseService> rpcRegistration;

  public void setDataProvider(final DataBroker salDataProvider) {
    this.dataProvider = salDataProvider;
  }


  public void setRpcRegistration(BindingAwareBroker.RoutedRpcRegistration<CarPurchaseService> rpcRegistration) {
    this.rpcRegistration = rpcRegistration;
  }

  @Override
  public Future<RpcResult<Void>> addCar(AddCarInput input) {
    log.info("RPC addCar : adding car [{}]", input);

    CarBuilder builder = new CarBuilder(input);
    final Car car = builder.build();
    final SettableFuture<RpcResult<Void>> futureResult = SettableFuture.create();

    // Each entry will be identifiable by a unique key, we have to create that identifier
    final InstanceIdentifier.InstanceIdentifierBuilder<Car> carIdBuilder =
        InstanceIdentifier.<Cars>builder(Cars.class)
            .child(Car.class, car.getKey());
    final InstanceIdentifier carId = carIdBuilder.build();
    // Place entry in data store tree
    WriteTransaction tx = dataProvider.newWriteOnlyTransaction();
    tx.put(LogicalDatastoreType.CONFIGURATION, carId, car);

    Futures.addCallback(tx.submit(), new FutureCallback<Void>() {
      @Override
      public void onSuccess(final Void result) {
        log.info("RPC addCar : car added successfully [{}]", car);
        rpcRegistration.registerPath(CarContext.class, carId);
        log.info("RPC addCar : routed rpc registered for instance ID [{}]", carId);
        futureResult.set(RpcResultBuilder.<Void>success().build());
      }

      @Override
      public void onFailure(final Throwable t) {
        log.error(String.format("RPC addCar : car addition failed [%s]", car), t);
        futureResult.set(RpcResultBuilder.<Void>failed()
            .withError(RpcError.ErrorType.APPLICATION, t.getMessage()).build());
      }
    });
    return futureResult;
  }

  @Override
  public void close() throws Exception {

  }
}
