/*
 * Copyright (c) 2021 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.clustering.it.karaf.cli.command.rpc.test;

import org.apache.karaf.shell.api.action.Action;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.lifecycle.Reference;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.opendaylight.yang.gen.v1.urn.opendaylight.controller.basic.rpc.test.rev160120.BasicGlobalInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.controller.basic.rpc.test.rev160120.BasicGlobalInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.controller.basic.rpc.test.rev160120.BasicRpcTestService;

@Service
@Command(scope = "test-app", name = "global-basic", description = "Run a global-basic test")
public class BasicGlobalCommand implements Action {

    @Reference
    private BasicRpcTestService basicRpcTestService;

    @Override
    public Object execute() throws Exception {
        final BasicGlobalInput input = new BasicGlobalInputBuilder().build();
        return basicRpcTestService.basicGlobal(input);
    }
}
