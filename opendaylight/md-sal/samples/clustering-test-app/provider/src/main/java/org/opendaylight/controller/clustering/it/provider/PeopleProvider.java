/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.clustering.it.provider;

import static java.util.Objects.requireNonNull;

import com.google.common.collect.ImmutableSet;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.common.util.concurrent.SettableFuture;
import java.util.HashSet;
import java.util.Set;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.binding.api.RpcProviderService;
import org.opendaylight.mdsal.binding.api.WriteTransaction;
import org.opendaylight.mdsal.common.api.CommitInfo;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.sal.clustering.it.car.purchase.rev140818.CarPurchaseService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.sal.clustering.it.people.rev140818.AddPersonInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.sal.clustering.it.people.rev140818.AddPersonOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.sal.clustering.it.people.rev140818.AddPersonOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.sal.clustering.it.people.rev140818.People;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.sal.clustering.it.people.rev140818.PeopleService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.sal.clustering.it.people.rev140818.people.Person;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.sal.clustering.it.people.rev140818.people.PersonBuilder;
import org.opendaylight.yangtools.concepts.ObjectRegistration;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.ErrorType;
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
public class PeopleProvider implements PeopleService, AutoCloseable {

    private static final Logger LOG = LoggerFactory.getLogger(PeopleProvider.class);

    private final Set<ObjectRegistration<?>> regs = new HashSet<>();
    private final DataBroker dataProvider;
    private final RpcProviderService rpcProviderService;
    private final CarPurchaseService rpcImplementation;

    @Inject
    @Activate
    public PeopleProvider(@Reference final DataBroker dataProvider,
            @Reference final RpcProviderService rpcProviderService,
            @Reference final CarPurchaseService rpcImplementation) {
        this.dataProvider = requireNonNull(dataProvider);
        this.rpcProviderService = requireNonNull(rpcProviderService);
        this.rpcImplementation = requireNonNull(rpcImplementation);

        // Add global registration
        regs.add(rpcProviderService.registerRpcImplementation(CarPurchaseService.class, rpcImplementation));
    }

    @Override
    public ListenableFuture<RpcResult<AddPersonOutput>> addPerson(final AddPersonInput input) {
        LOG.info("RPC addPerson : adding person [{}]", input);

        PersonBuilder builder = new PersonBuilder(input);
        final Person person = builder.build();
        final SettableFuture<RpcResult<AddPersonOutput>> futureResult = SettableFuture.create();

        // Each entry will be identifiable by a unique key, we have to create that identifier
        final InstanceIdentifier<Person> personId = InstanceIdentifier.builder(People.class)
                .child(Person.class, person.key()).build();
        // Place entry in data store tree
        WriteTransaction tx = dataProvider.newWriteOnlyTransaction();
        tx.put(LogicalDatastoreType.CONFIGURATION, personId, person);

        tx.commit().addCallback(new FutureCallback<CommitInfo>() {
            @Override
            public void onSuccess(final CommitInfo result) {
                LOG.info("RPC addPerson : person added successfully [{}]", person);
                regs.add(rpcProviderService.registerRpcImplementation(CarPurchaseService.class, rpcImplementation,
                    ImmutableSet.of(personId)));
                LOG.info("RPC addPerson : routed rpc registered for instance ID [{}]", personId);
                futureResult.set(RpcResultBuilder.success(new AddPersonOutputBuilder().build()).build());
            }

            @Override
            public void onFailure(final Throwable ex) {
                LOG.error("RPC addPerson : person addition failed [{}]", person, ex);
                futureResult.set(RpcResultBuilder.<AddPersonOutput>failed()
                        .withError(ErrorType.APPLICATION, ex.getMessage()).build());
            }
        }, MoreExecutors.directExecutor());
        return futureResult;
    }

    @PreDestroy
    @Deactivate
    @Override
    public void close() {
        regs.forEach(ObjectRegistration::close);
        regs.clear();
    }
}
