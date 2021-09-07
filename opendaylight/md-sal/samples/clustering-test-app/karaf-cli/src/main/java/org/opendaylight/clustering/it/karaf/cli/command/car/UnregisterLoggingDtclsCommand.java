/*
 * Copyright (c) 2021 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.clustering.it.karaf.cli.command.car;

import org.apache.karaf.shell.api.action.Action;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.lifecycle.Reference;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.sal.clustering.it.car.rev140818.CarService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.sal.clustering.it.car.rev140818.UnregisterLoggingDtclsInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.sal.clustering.it.car.rev140818.UnregisterLoggingDtclsInputBuilder;

@Service
@Command(scope = "test-app", name = "unregister-logging-dtcls", description = "Run and unregister-logging-dtcls test")
public class UnregisterLoggingDtclsCommand implements Action {

    @Reference
    private CarService carService;

    @Override
    public Object execute() throws Exception {
        final UnregisterLoggingDtclsInput input = new UnregisterLoggingDtclsInputBuilder().build();
        return carService.unregisterLoggingDtcls(input);
    }
}
