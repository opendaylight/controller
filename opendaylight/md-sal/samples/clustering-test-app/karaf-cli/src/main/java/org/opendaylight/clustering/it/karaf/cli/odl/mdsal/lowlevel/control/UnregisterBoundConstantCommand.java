/*
 * Copyright (c) 2021 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.clustering.it.karaf.cli.odl.mdsal.lowlevel.control;

import com.google.common.util.concurrent.ListenableFuture;
import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.lifecycle.Reference;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.opendaylight.clustering.it.karaf.cli.AbstractRpcAction;
import org.opendaylight.clustering.it.karaf.cli.InstanceIdentifierSupport;
import org.opendaylight.mdsal.binding.api.RpcConsumerRegistry;
import org.opendaylight.yang.gen.v1.tag.opendaylight.org._2017.controller.yang.lowlevel.control.rev170215.OdlMdsalLowlevelControlService;
import org.opendaylight.yang.gen.v1.tag.opendaylight.org._2017.controller.yang.lowlevel.control.rev170215.UnregisterBoundConstantInputBuilder;
import org.opendaylight.yangtools.yang.common.RpcResult;

@Service
@Command(scope = "test-app", name = "unregister-bound-constant", description = "Run an unregister-bound-constant test")
public class UnregisterBoundConstantCommand extends AbstractRpcAction {
    @Reference
    private RpcConsumerRegistry rpcService;
    @Reference
    private InstanceIdentifierSupport iidSupport;
    @Argument(index = 0, name = "context", required = true)
    String context;

    @Override
    protected ListenableFuture<? extends RpcResult<?>> invokeRpc() {
        return rpcService.getRpcService(OdlMdsalLowlevelControlService.class)
                .unregisterBoundConstant(new UnregisterBoundConstantInputBuilder()
                        .setContext(iidSupport.parseArgument(context))
                        .build());
    }
}
