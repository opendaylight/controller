/*
 * Copyright (c) 2015 Cisco Systems and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package rpcbenchmark.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicLong;
import org.opendaylight.mdsal.binding.api.RpcService;
import org.opendaylight.yang.gen.v1.rpcbench.payload.rev150702.RoutedRpcBench;
import org.opendaylight.yang.gen.v1.rpcbench.payload.rev150702.RoutedRpcBenchInput;
import org.opendaylight.yang.gen.v1.rpcbench.payload.rev150702.RoutedRpcBenchInputBuilder;
import org.opendaylight.yang.gen.v1.rpcbench.payload.rev150702.RoutedRpcBenchOutput;
import org.opendaylight.yang.gen.v1.rpcbench.payload.rev150702.payload.Payload;
import org.opendaylight.yang.gen.v1.rpcbench.payload.rev150702.payload.PayloadBuilder;
import org.opendaylight.yang.gen.v1.rpcbench.payload.rev150702.payload.PayloadKey;
import org.opendaylight.yangtools.binding.DataObjectIdentifier;
import org.opendaylight.yangtools.binding.util.BindingMap;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RoutedBindingRTClient implements RTCClient {
    private static final Logger LOG = LoggerFactory.getLogger(RoutedBindingRTClient.class);
    private final RoutedRpcBench routedRpcBench;
    private final AtomicLong rpcOk = new AtomicLong(0);
    private final AtomicLong rpcError = new AtomicLong(0);
    private final List<RoutedRpcBenchInput> inVal = new ArrayList<>();
    private final int inSize;

    public RoutedBindingRTClient(final RpcService rpcService, final int inSize,
            final List<DataObjectIdentifier<?>> routeIid) {
        routedRpcBench = rpcService.getRpc(RoutedRpcBench.class);
        this.inSize = inSize;

        final var listVals = BindingMap.<PayloadKey, Payload>orderedBuilder(inSize);
        for (int i = 0; i < inSize; i++) {
            listVals.add(new PayloadBuilder().withKey(new PayloadKey(i)).build());
        }

        for (var iid : routeIid) {
            inVal.add(new RoutedRpcBenchInputBuilder()
                .setNode(iid)
                .setPayload(listVals.build())
                .build());
        }

    }

    @Override
    public long getRpcOk() {
        return rpcOk.get();
    }

    @Override
    public long getRpcError() {
        return rpcError.get();
    }

    @Override
    public void runTest(final int iterations) {
        int ok = 0;
        int error = 0;

        int rpcServerCnt = inVal.size();
        for (int i = 0; i < iterations; i++) {
            final var input = inVal.get(ThreadLocalRandom.current().nextInt(rpcServerCnt));
            final var future = routedRpcBench.invoke(input);
            final RpcResult<RoutedRpcBenchOutput> rpcResult;
            try {
                rpcResult = future.get();
            } catch (InterruptedException | ExecutionException e) {
                error++;
                LOG.error("Execution failed: ", e);
                continue;
            }

            if (rpcResult.isSuccessful()) {
                final var retVal = rpcResult.getResult().getPayload();
                if (retVal.size() == inSize) {
                    ok++;
                }
                else {
                    error++;
                }
            }
        }

        rpcOk.addAndGet(ok);
        rpcError.addAndGet(error);
    }

    @Override
    public void close() {
        // TODO Auto-generated method stub

    }
}
