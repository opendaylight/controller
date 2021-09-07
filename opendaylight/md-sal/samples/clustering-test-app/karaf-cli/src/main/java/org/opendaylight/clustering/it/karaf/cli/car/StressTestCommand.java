/*
 * Copyright (c) 2021 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.clustering.it.karaf.cli.car;

import com.google.common.util.concurrent.ListenableFuture;
import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.lifecycle.Reference;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.opendaylight.clustering.it.karaf.cli.AbstractRpcAction;
import org.opendaylight.mdsal.binding.api.RpcConsumerRegistry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.sal.clustering.it.car.rev140818.CarService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.sal.clustering.it.car.rev140818.StressTestInputBuilder;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.common.Uint16;
import org.opendaylight.yangtools.yang.common.Uint32;

@Service
@Command(scope = "test-app" , name = "stress-test", description = "Run a stress-test")
public class StressTestCommand extends AbstractRpcAction {
    @Reference
    private RpcConsumerRegistry rpcService;
    @Argument(index = 0, name = "rate", required = true)
    int rate;
    @Argument(index = 1, name = "count", required = true)
    long count;

    @Override
    protected ListenableFuture<? extends RpcResult<?>> invokeRpc() {
        return rpcService.getRpcService(CarService.class).stressTest(new StressTestInputBuilder()
            .setRate(Uint16.valueOf(rate))
            .setCount(Uint32.valueOf(count))
            .build());
    }
}
