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
import java.util.concurrent.Future;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.sal.clustering.it.car.purchase.rev140818.CarPurchaseService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.sal.clustering.it.people.rev140818.AddPersonInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.sal.clustering.it.people.rev140818.People;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.sal.clustering.it.people.rev140818.PeopleService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.sal.clustering.it.people.rev140818.PersonContext;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.sal.clustering.it.people.rev140818.people.Person;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.sal.clustering.it.people.rev140818.people.PersonBuilder;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.RpcError;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CarProvider implements CarService, AutoCloseable {

  private static final Logger log = LoggerFactory.getLogger(CarProvider.class);

  private DataBroker dataProvider;

  private BindingAwareBroker.RoutedRpcRegistration<CarPurchaseService> rpcRegistration;

  public void setDataProvider(final DataBroker salDataProvider) {
    this.dataProvider = salDataProvider;
  }


  public void setRpcRegistration(BindingAwareBroker.RoutedRpcRegistration<CarPurchaseService> rpcRegistration) {
    this.rpcRegistration = rpcRegistration;
  }

  @Override
  public Future<RpcResult<Void>> addCar-entry(AddCar-entryInput input) {
    log.info("RPC addCar-entry : adding car-entry [{}]", input);

    Car-entryBuilder builder = new Car-entryBuilder(input);
    final Car-entry car-entry = builder.build();
    final SettableFuture<RpcResult<Void>> futureResult = SettableFuture.create();

    // Each entry will be identifiable by a unique key, we have to create that identifier
    final InstanceIdentifier.InstanceIdentifierBuilder<Car-entry> car-entryIdBuilder =
        InstanceIdentifier.<Car>builder(Car.class)
            .child(Car-entry.class, Car-entry.getKey());
    final InstanceIdentifier car-entryId = car-entryIdBuilder.build();
    // Place entry in data store tree
    WriteTransaction tx = dataProvider.newWriteOnlyTransaction();
    tx.put(LogicalDatastoreType.CONFIGURATION, car-entryId, car-entry);

    Futures.addCallback(tx.submit(), new FutureCallback<Void>() {
      @Override
      public void onSuccess(final Void result) {
        log.info("RPC addCar-entry : car-entry added successfully [{}]", car-entry);
        rpcRegistration.registerPath(Car-entryContext.class, car-entryId);
        log.info("RPC addCar-entry : routed rpc registered for instance ID [{}]", car-entryId);
        futureResult.set(RpcResultBuilder.<Void>success().build());
      }

      @Override
      public void onFailure(final Throwable t) {
        log.error(String.format("RPC addCar-entry : car-entry addition failed [%s]", car-entry), t);
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
