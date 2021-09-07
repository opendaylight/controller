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
import org.opendaylight.yang.gen.v1.tag.opendaylight.org._2017.controller.yang.lowlevel.control.rev170215.UnregisterDefaultConstantInput;
import org.opendaylight.yang.gen.v1.tag.opendaylight.org._2017.controller.yang.lowlevel.control.rev170215.UnregisterDefaultConstantInputBuilder;

@Service
@Command(scope = "test-app", name = "unregister-default-constant", description = "Run an unregister-default-constant test")
public class UnregisterDefaultConstantCommand implements Action {

    @Reference
    private OdlMdsalLowlevelControlService controlService;

    @Override
    public Object execute() throws Exception {
        final UnregisterDefaultConstantInput input = new UnregisterDefaultConstantInputBuilder().build();
        return controlService.unregisterDefaultConstant(input);
    }
}
