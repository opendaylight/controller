/*
 * Copyright (c) 2021 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.clustering.it.karaf.cli.command.odl.mdsal.lowlevel.control;

import org.apache.karaf.shell.api.action.Action;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.lifecycle.Reference;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.opendaylight.yang.gen.v1.tag.opendaylight.org._2017.controller.yang.lowlevel.control.rev170215.OdlMdsalLowlevelControlService;
import org.opendaylight.yang.gen.v1.tag.opendaylight.org._2017.controller.yang.lowlevel.control.rev170215.UnregisterFlappingSingletonInput;
import org.opendaylight.yang.gen.v1.tag.opendaylight.org._2017.controller.yang.lowlevel.control.rev170215.UnregisterFlappingSingletonInputBuilder;

@Service
@Command(scope = "test-app", name = "unregister-flapping-singleton",
        description = "Run an unregister-flapping-singleton test")
public class UnregisterFlappingSingletonCommand implements Action {

    @Reference
    private OdlMdsalLowlevelControlService controlService;

    @Override
    public Object execute() throws Exception {
        final UnregisterFlappingSingletonInput input = new UnregisterFlappingSingletonInputBuilder().build();
        return controlService.unregisterFlappingSingleton(input);
    }
}
