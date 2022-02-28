/*
 * Copyright (c) 2022 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.clustering.it.provider;

import akka.actor.AbstractActor;
import akka.actor.Props;
import akka.cluster.Cluster;
import org.opendaylight.clustering.it.karaf.cli.InstanceIdentifierSupport;
import org.opendaylight.mdsal.binding.api.RpcConsumerRegistry;
import org.opendaylight.mdsal.binding.api.RpcProviderService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.sal.clustering.it.car.purchase.rev140818.BuyCarInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.sal.clustering.it.car.purchase.rev140818.CarPurchaseService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.sal.clustering.it.car.rev140818.CarId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.sal.clustering.it.people.rev140818.AddPersonInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.sal.clustering.it.people.rev140818.AddPersonInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.sal.clustering.it.people.rev140818.PersonId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.sal.clustering.it.people.rev140818.PersonRef;
import org.opendaylight.yangtools.yang.common.Uint32;

public class InvokerActor extends AbstractActor {

    public static final class AddPerson {
        private final String id;
        private final String gender;
        private final long age;
        private final String address;
        private final String contactNo;

        public AddPerson(String id, String gender, long age, String address, String contactNo) {
            this.id = id;
            this.gender = gender;
            this.age = age;
            this.address = address;
            this.contactNo = contactNo;
        }
    }

    public static final class PurchaseCar {
        private final String personRef;
        private final String carId;
        private final String personId;

        public PurchaseCar(String personRef, String carId, String personId) {
            this.personRef = personRef;
            this.carId = carId;
            this.personId = personId;
        }
    }

    private final RpcConsumerRegistry rpcConsumerRegistry;
    private final InstanceIdentifierSupport iidSupport;

    public InvokerActor(RpcConsumerRegistry rpcConsumerRegistry, InstanceIdentifierSupport iidSupport) {
        this.rpcConsumerRegistry = rpcConsumerRegistry;
        this.iidSupport = iidSupport;
    }

    static Props props(RpcConsumerRegistry rpcConsumerRegistry,
                       InstanceIdentifierSupport iidSupport) {
        return Props.create(InvokerActor.class, rpcConsumerRegistry, iidSupport);
    }

    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(AddPerson.class, this::handleAddPersonMessages)
                .match(PurchaseCar.class, this::handleCarPurchaseMessages)
                .build();
    }

    private void handleAddPersonMessages(final AddPerson message) {
        rpcConsumerRegistry.getRpcService(PeopleProvider.class).addPerson(new AddPersonInputBuilder()
                .setId(new PersonId(message.id))
                .setGender(message.gender)
                .setAge(Uint32.valueOf(message.age))
                .setAddress(message.address)
                .setContactNo(message.contactNo)
                .build());
    }

    private void handleCarPurchaseMessages(final PurchaseCar message) {
        rpcConsumerRegistry.getRpcService(CarPurchaseService.class).buyCar(new BuyCarInputBuilder()
                .setPerson(new PersonRef(iidSupport.parseArgument(message.personRef)))
                .setCarId(new CarId(message.carId))
                .setPersonId(new PersonId(message.personId))
                .build());
    }
}
