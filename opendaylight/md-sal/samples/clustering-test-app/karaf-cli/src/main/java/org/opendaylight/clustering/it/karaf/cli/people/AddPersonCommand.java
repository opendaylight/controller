/*
 * Copyright (c) 2021 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.clustering.it.karaf.cli.people;

import com.google.common.util.concurrent.ListenableFuture;
import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.lifecycle.Reference;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.opendaylight.clustering.it.karaf.cli.AbstractRpcAction;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.sal.clustering.it.people.rev140818.AddPersonInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.sal.clustering.it.people.rev140818.PeopleService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.sal.clustering.it.people.rev140818.PersonId;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.common.Uint32;

@Service
@Command(scope = "test-app", name = "add-person", description = " Run an add-person test")
public class AddPersonCommand extends AbstractRpcAction {
    @Reference
    private PeopleService peopleService;
    @Argument(index = 0, name = "id", required = true)
    PersonId id;
    @Argument(index = 1, name = "gender", required = true)
    String gender;
    @Argument(index = 2, name = "age", required = true)
    long age;
    @Argument(index = 3, name = "address", required = true)
    String address;
    @Argument(index = 4, name = "contactNo", required = true)
    String contactNo;

    @Override
    protected ListenableFuture<? extends RpcResult<?>> invokeRpc() {
        return peopleService.addPerson(new AddPersonInputBuilder()
            .setId(id)
            .setGender(gender)
            .setAge(Uint32.valueOf(age))
            .setAddress(address)
            .setContactNo(contactNo)
            .build());
    }
}
