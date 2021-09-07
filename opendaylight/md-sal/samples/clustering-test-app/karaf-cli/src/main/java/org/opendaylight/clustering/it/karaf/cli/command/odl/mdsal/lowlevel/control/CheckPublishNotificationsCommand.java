/*
 * Copyright (c) 2021 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.clustering.it.karaf.cli.command.odl.mdsal.lowlevel.control;

import org.apache.karaf.shell.api.action.Action;
import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.lifecycle.Reference;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.opendaylight.yang.gen.v1.tag.opendaylight.org._2017.controller.yang.lowlevel.control.rev170215.CheckPublishNotificationsInput;
import org.opendaylight.yang.gen.v1.tag.opendaylight.org._2017.controller.yang.lowlevel.control.rev170215.CheckPublishNotificationsInputBuilder;
import org.opendaylight.yang.gen.v1.tag.opendaylight.org._2017.controller.yang.lowlevel.control.rev170215.OdlMdsalLowlevelControlService;

@Service
@Command(scope = "test-app", name = "check-publish-notifications",
         description = "Run a check-publish-notifications test")
public class CheckPublishNotificationsCommand implements Action {

    @Reference
    private OdlMdsalLowlevelControlService controlService;

    @Argument(index = 0, name = "id", required = true)
    String id;

    @Override
    public Object execute() throws Exception {
        final CheckPublishNotificationsInput input = new CheckPublishNotificationsInputBuilder()
                .setId(id)
                .build();

        return controlService.checkPublishNotifications(input);
    }
}
