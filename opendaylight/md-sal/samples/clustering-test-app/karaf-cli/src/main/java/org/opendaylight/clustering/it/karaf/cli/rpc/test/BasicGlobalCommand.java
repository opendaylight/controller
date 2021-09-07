/*
 * Copyright (c) 2021 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.clustering.it.karaf.cli.rpc.test;

import com.google.common.util.concurrent.ListenableFuture;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.lifecycle.Reference;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.opendaylight.clustering.it.karaf.cli.AbstractRpcAction;
import org.opendaylight.yang.gen.v1.urn.opendaylight.controller.basic.rpc.test.rev160120.BasicGlobalInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.controller.basic.rpc.test.rev160120.BasicRpcTestService;
import org.opendaylight.yangtools.yang.common.RpcResult;

@Service
@Command(scope = "test-app", name = "global-basic", description = "Run a global-basic test")
public class BasicGlobalCommand extends AbstractRpcAction {
    @Reference
    private BasicRpcTestService basicRpcTestService;

    @Override
    protected ListenableFuture<? extends RpcResult<?>> invokeRpc() {
        return basicRpcTestService.basicGlobal(new BasicGlobalInputBuilder().build());
    }
}
