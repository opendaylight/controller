/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.clustering.it.listener;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.sal.clustering.it.car.people.rev140818.CarPeople;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.sal.clustering.it.car.people.rev140818.car.people.CarPerson;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.sal.clustering.it.car.people.rev140818.car.people.CarPersonBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.sal.clustering.it.car.people.rev140818.car.people.CarPersonKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.sal.clustering.it.car.purchase.rev140818.CarBought;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.sal.clustering.it.car.purchase.rev140818.CarPurchaseListener;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class PeopleCarListener implements CarPurchaseListener {

  private static final Logger log = LoggerFactory.getLogger(PeopleCarListener.class);

  private DataBroker dataProvider;



  public void setDataProvider(final DataBroker salDataProvider) {
    this.dataProvider = salDataProvider;
  }

  @Override
  public void onCarBought(CarBought notification) {

    final CarPersonBuilder carPersonBuilder = new CarPersonBuilder();
    carPersonBuilder.setCarId(notification.getCarId());
    carPersonBuilder.setPersonId(notification.getPersonId());
    CarPersonKey key = new CarPersonKey(notification.getCarId(), notification.getPersonId());
    carPersonBuilder.setKey(key);
    final CarPerson carPerson = carPersonBuilder.build();

    log.info("Car bought, adding car-person entry: [{}]", carPerson);

    InstanceIdentifier<CarPerson> carPersonIId =
        InstanceIdentifier.<CarPeople>builder(CarPeople.class).child(CarPerson.class, carPerson.getKey()).build();


    WriteTransaction tx = dataProvider.newWriteOnlyTransaction();
    tx.put(LogicalDatastoreType.CONFIGURATION, carPersonIId, carPerson);

    Futures.addCallback(tx.submit(), new FutureCallback<Void>() {
      @Override
      public void onSuccess(final Void result) {
        log.info("Successfully added car-person entry: [{}]", carPerson);
      }

      @Override
      public void onFailure(final Throwable t) {
        log.error(String.format("Failed to add car-person entry: [%s]", carPerson), t);
      }
    });

  }
}
