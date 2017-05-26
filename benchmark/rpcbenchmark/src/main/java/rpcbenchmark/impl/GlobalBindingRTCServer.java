/*
 * Copyright (c) 2015 Cisco Systems and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package rpcbenchmark.impl;

import java.util.concurrent.Future;

import org.opendaylight.yang.gen.v1.rpcbench.payload.rev150702.GlobalRpcBenchInput;
import org.opendaylight.yang.gen.v1.rpcbench.payload.rev150702.GlobalRpcBenchOutput;
import org.opendaylight.yang.gen.v1.rpcbench.payload.rev150702.GlobalRpcBenchOutputBuilder;
import org.opendaylight.yang.gen.v1.rpcbench.payload.rev150702.RoutedRpcBenchInput;
import org.opendaylight.yang.gen.v1.rpcbench.payload.rev150702.RoutedRpcBenchOutput;
import org.opendaylight.yang.gen.v1.rpcbench.payload.rev150702.RoutedRpcBenchOutputBuilder;
import org.opendaylight.yang.gen.v1.rpcbench.payload.rev150702.RpcbenchPayloadService;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.util.concurrent.Futures;

public class GlobalBindingRTCServer implements RpcbenchPayloadService {

    private static final Logger LOG = LoggerFactory.getLogger(GlobalBindingRTCServer.class);
    private int numRpcs = 0;

    public GlobalBindingRTCServer() {
        LOG.info("GlobalBindingRTCServer created.");
    }

    @Override
    public Future<RpcResult<GlobalRpcBenchOutput>> globalRpcBench(
            GlobalRpcBenchInput input) {
        GlobalRpcBenchOutput output = new GlobalRpcBenchOutputBuilder(input).build();
        RpcResult<GlobalRpcBenchOutput> result = RpcResultBuilder.success(output).build();
        numRpcs++;
        return Futures.immediateFuture(result);
    }

    @Override
    public Future<RpcResult<RoutedRpcBenchOutput>> routedRpcBench(
            RoutedRpcBenchInput input) {
        RoutedRpcBenchOutput output = new RoutedRpcBenchOutputBuilder(input).build();
        RpcResult<RoutedRpcBenchOutput> result = RpcResultBuilder.success(output).build();
        numRpcs++;
        return Futures.immediateFuture(result);
    }

    public int getNumRpcs() {
        return numRpcs;
    }
}
