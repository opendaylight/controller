/*
 * Copyright (c) 2021 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.clustering.it.karaf.cli.command.car;

import org.apache.karaf.shell.api.action.Action;
import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.lifecycle.Reference;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.sal.clustering.it.car.rev140818.CarService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.sal.clustering.it.car.rev140818.UnregisterOwnershipInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.sal.clustering.it.car.rev140818.UnregisterOwnershipInputBuilder;


@Service
@Command(scope = "test-app", name = "unregister-ownership", description = "Run an unregister-ownership test")
public class UnregisterOwnershipCommand implements Action {

    @Reference
    private CarService carService;

    @Argument(index = 0, name = "car-id", required = true)
    String carId;

    @Override
    public Object execute() throws Exception {
        final UnregisterOwnershipInput input = new UnregisterOwnershipInputBuilder()
                .setCarId(carId)
                .build();
        return carService.unregisterOwnership(input);
    }
}
