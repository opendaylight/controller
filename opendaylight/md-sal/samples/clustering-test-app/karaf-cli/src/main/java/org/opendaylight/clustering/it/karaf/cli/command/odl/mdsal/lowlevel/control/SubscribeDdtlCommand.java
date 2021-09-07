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
import org.opendaylight.yang.gen.v1.tag.opendaylight.org._2017.controller.yang.lowlevel.control.rev170215.SubscribeDdtlInput;
import org.opendaylight.yang.gen.v1.tag.opendaylight.org._2017.controller.yang.lowlevel.control.rev170215.SubscribeDdtlInputBuilder;

@Service
@Command(scope = "test-app", name = "subscribe-ddtl", description = "Run a subscribe-ddtl test")
public class SubscribeDdtlCommand implements Action {

    @Reference
    private OdlMdsalLowlevelControlService controlService;

    @Override
    public Object execute() throws Exception {
        final SubscribeDdtlInput input = new SubscribeDdtlInputBuilder().build();
        return controlService.subscribeDdtl(input);
    }
}
