/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.clustering.it.listener;

import static java.util.Objects.requireNonNull;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.MoreExecutors;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.binding.api.NotificationService;
import org.opendaylight.mdsal.binding.api.WriteTransaction;
import org.opendaylight.mdsal.common.api.CommitInfo;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.sal.clustering.it.car.people.rev140818.CarPeople;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.sal.clustering.it.car.people.rev140818.car.people.CarPerson;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.sal.clustering.it.car.people.rev140818.car.people.CarPersonBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.sal.clustering.it.car.people.rev140818.car.people.CarPersonKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.sal.clustering.it.car.purchase.rev140818.CarBought;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.sal.clustering.it.car.purchase.rev140818.CarPurchaseListener;
import org.opendaylight.yangtools.concepts.Registration;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
@Component(service = { })
public final class PeopleCarListener implements CarPurchaseListener {
    private static final Logger LOG = LoggerFactory.getLogger(PeopleCarListener.class);

    private final DataBroker dataProvider;
    private final Registration reg;

    @Inject
    @Activate
    public PeopleCarListener(@Reference final DataBroker dataProvider,
            @Reference final NotificationService notifService) {
        this.dataProvider = requireNonNull(dataProvider);
        reg = notifService.registerNotificationListener(this);
    }

    @PreDestroy
    @Deactivate
    public void close() {
        reg.close();
    }

    @Override
    public void onCarBought(final CarBought notification) {

        final CarPerson carPerson = new CarPersonBuilder()
            .withKey(new CarPersonKey(notification.getCarId(), notification.getPersonId()))
            .build();

        LOG.info("Car bought, adding car-person entry: [{}]", carPerson);

        InstanceIdentifier<CarPerson> carPersonIId = InstanceIdentifier.builder(CarPeople.class)
                .child(CarPerson.class, carPerson.key()).build();


        WriteTransaction tx = dataProvider.newWriteOnlyTransaction();
        tx.put(LogicalDatastoreType.CONFIGURATION, carPersonIId, carPerson);

        tx.commit().addCallback(new FutureCallback<CommitInfo>() {
            @Override
            public void onSuccess(final CommitInfo result) {
                LOG.info("Successfully added car-person entry: [{}]", carPerson);
            }

            @Override
            public void onFailure(final Throwable ex) {
                LOG.error("Failed to add car-person entry: [{}]", carPerson, ex);
            }
        }, MoreExecutors.directExecutor());
    }
}
