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
import java.util.concurrent.Future;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicLong;

import org.opendaylight.controller.sal.binding.api.RpcConsumerRegistry;
import org.opendaylight.yang.gen.v1.rpcbench.payload.rev150702.RoutedRpcBenchInput;
import org.opendaylight.yang.gen.v1.rpcbench.payload.rev150702.RoutedRpcBenchInputBuilder;
import org.opendaylight.yang.gen.v1.rpcbench.payload.rev150702.RoutedRpcBenchOutput;
import org.opendaylight.yang.gen.v1.rpcbench.payload.rev150702.RpcbenchPayloadService;
import org.opendaylight.yang.gen.v1.rpcbench.payload.rev150702.payload.Payload;
import org.opendaylight.yang.gen.v1.rpcbench.payload.rev150702.payload.PayloadBuilder;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RoutedBindingRTClient implements RTCClient {
    private static final Logger LOG = LoggerFactory.getLogger(GlobalBindingRTCClient.class);
    private final RpcbenchPayloadService service;
    private final AtomicLong rpcOk = new AtomicLong(0);
    private final AtomicLong rpcError = new AtomicLong(0);
    private final List<RoutedRpcBenchInput> inVal;
    private final int inSize;

    public long getRpcOk() {
        return rpcOk.get();
    }

    public long getRpcError() {
        return rpcError.get();
    }

    public RoutedBindingRTClient(RpcConsumerRegistry registry, int inSize, List<InstanceIdentifier<?>> routeIid) {
        if (registry != null) {
            this.service = registry.getRpcService(RpcbenchPayloadService.class);
        } else {
            this.service = null;
        }
        this.inSize = inSize;
        this.inVal = new ArrayList<>();

        List<Payload> listVals = new ArrayList<>();
        for (int i = 0; i < inSize; i++) {
            listVals.add(new PayloadBuilder().setId(i).build());
        }

        for (InstanceIdentifier<?> iid : routeIid) {
            inVal.add(new RoutedRpcBenchInputBuilder().setNode(iid).setPayload(listVals).build());
        }

    }

    public void runTest(int iterations) {
        int rpcOk = 0;
        int rpcError = 0;

        int rpcServerCnt = inVal.size();
        for (int i = 0; i < iterations; i++) {
            RoutedRpcBenchInput input = inVal.get(ThreadLocalRandom.current().nextInt(rpcServerCnt));
            Future<RpcResult<RoutedRpcBenchOutput>> output = service.routedRpcBench(input);
            try {
                RpcResult<RoutedRpcBenchOutput> rpcResult = output.get();

                if (rpcResult.isSuccessful()) {
                    List<Payload> retVal = rpcResult.getResult().getPayload();
                    if (retVal.size() == inSize) {
                        rpcOk++;
                    }
                    else {
                        rpcError++;
                    }
                }
            } catch (InterruptedException | ExecutionException e) {
                rpcError++;
                LOG.error("Execution failed: ", e);
            }
        }

        this.rpcOk.addAndGet(rpcOk);
        this.rpcError.addAndGet(rpcError);
    }

    @Override
    public void close() {
        // TODO Auto-generated method stub

    }

}
