/*
 * Copyright (c) 2021 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.clustering.it.karaf.cli.car;

import com.google.common.util.concurrent.ListenableFuture;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.lifecycle.Reference;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.opendaylight.clustering.it.karaf.cli.AbstractRpcAction;
import org.opendaylight.mdsal.binding.api.RpcConsumerRegistry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.sal.clustering.it.car.rev140818.CarService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.sal.clustering.it.car.rev140818.UnregisterCommitCohortInputBuilder;
import org.opendaylight.yangtools.yang.common.RpcResult;

@Service
@Command(scope = "test-app", name = "unregister-commit-cohort", description = "Run a unregister-commit-cohort test")
public class UnregisterCommitCohortCommand extends AbstractRpcAction {
    @Reference
    private RpcConsumerRegistry rpcService;

    @Override
    protected ListenableFuture<? extends RpcResult<?>> invokeRpc() {
        return rpcService.getRpcService(CarService.class)
            .unregisterCommitCohort(new UnregisterCommitCohortInputBuilder().build());
    }
}
