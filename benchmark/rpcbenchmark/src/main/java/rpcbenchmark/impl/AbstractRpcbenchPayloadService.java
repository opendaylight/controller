/*
 * Copyright (c) 2015 Cisco Systems and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package rpcbenchmark.impl;

import com.google.common.util.concurrent.ListenableFuture;
import org.opendaylight.yang.gen.v1.rpcbench.payload.rev150702.GlobalRpcBenchInput;
import org.opendaylight.yang.gen.v1.rpcbench.payload.rev150702.GlobalRpcBenchOutput;
import org.opendaylight.yang.gen.v1.rpcbench.payload.rev150702.GlobalRpcBenchOutputBuilder;
import org.opendaylight.yang.gen.v1.rpcbench.payload.rev150702.RoutedRpcBenchInput;
import org.opendaylight.yang.gen.v1.rpcbench.payload.rev150702.RoutedRpcBenchOutput;
import org.opendaylight.yang.gen.v1.rpcbench.payload.rev150702.RoutedRpcBenchOutputBuilder;
import org.opendaylight.yang.gen.v1.rpcbench.payload.rev150702.RpcbenchPayloadService;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;

abstract class AbstractRpcbenchPayloadService implements RpcbenchPayloadService {
    private int numRpcs = 0;

    @Override
    public final ListenableFuture<RpcResult<GlobalRpcBenchOutput>> globalRpcBench(final GlobalRpcBenchInput input) {
        numRpcs++;
        return RpcResultBuilder.success(new GlobalRpcBenchOutputBuilder(input).build()).buildFuture();
    }

    @Override
    public final ListenableFuture<RpcResult<RoutedRpcBenchOutput>> routedRpcBench(final RoutedRpcBenchInput input) {
        numRpcs++;
        return RpcResultBuilder.success(new RoutedRpcBenchOutputBuilder(input).build()).buildFuture();
    }

    final int getNumRpcs() {
        return numRpcs;
    }
}
