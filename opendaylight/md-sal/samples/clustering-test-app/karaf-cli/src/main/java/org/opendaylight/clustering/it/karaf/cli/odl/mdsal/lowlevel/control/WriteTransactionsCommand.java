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
import org.opendaylight.mdsal.binding.api.RpcConsumerRegistry;
import org.opendaylight.yang.gen.v1.tag.opendaylight.org._2017.controller.yang.lowlevel.control.rev170215.OdlMdsalLowlevelControlService;
import org.opendaylight.yang.gen.v1.tag.opendaylight.org._2017.controller.yang.lowlevel.control.rev170215.WriteTransactionsInputBuilder;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.common.Uint32;

@Service
@Command(scope = "test-app", name = "write-transactions", description = "Run a write-transactions test")
public class WriteTransactionsCommand extends AbstractRpcAction {
    @Reference
    private RpcConsumerRegistry rpcService;
    @Argument(index = 0, name = "id", required = true)
    String id;
    @Argument(index = 1, name = "seconds", required = true)
    long seconds;
    @Argument(index = 2, name = "trasactions-per-second", required = true)
    long transactionsPerSecond;
    @Argument(index = 3, name = "chained-transations", required = true)
    boolean chainedTransactions;

    @Override
    protected ListenableFuture<? extends RpcResult<?>> invokeRpc() {
        return rpcService.getRpcService(OdlMdsalLowlevelControlService.class)
                .writeTransactions(new WriteTransactionsInputBuilder()
                        .setId(id)
                        .setSeconds(Uint32.valueOf(seconds))
                        .setTransactionsPerSecond(Uint32.valueOf(transactionsPerSecond))
                        .setChainedTransactions(chainedTransactions)
                        .build());
    }
}
