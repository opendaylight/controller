/*
 * Copyright (c) 2015 Cisco Systems Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package rpcbenchmark.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.opendaylight.controller.sal.binding.api.BindingAwareBroker.ProviderContext;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker.RoutedRpcRegistration;
import org.opendaylight.controller.sal.binding.api.BindingAwareProvider;
import org.opendaylight.controller.sal.binding.api.RpcConsumerRegistry;
import org.opendaylight.controller.sal.binding.api.RpcProviderRegistry;
import org.opendaylight.yang.gen.v1.rpcbench.payload.rev150702.NodeContext;
import org.opendaylight.yang.gen.v1.rpcbench.payload.rev150702.RpcbenchPayloadService;
import org.opendaylight.yang.gen.v1.rpcbench.payload.rev150702.RpcbenchRpcRoutes;
import org.opendaylight.yang.gen.v1.rpcbench.payload.rev150702.rpcbench.rpc.routes.RpcRoute;
import org.opendaylight.yang.gen.v1.rpcbench.payload.rev150702.rpcbench.rpc.routes.RpcRouteKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rpcbenchmark.rev150702.RpcbenchmarkService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rpcbenchmark.rev150702.StartTestInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rpcbenchmark.rev150702.StartTestOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rpcbenchmark.rev150702.StartTestOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rpcbenchmark.rev150702.TestStatusOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rpcbenchmark.rev150702.TestStatusOutput.ExecStatus;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rpcbenchmark.rev150702.TestStatusOutputBuilder;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.binding.KeyedInstanceIdentifier;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RpcbenchmarkProvider implements BindingAwareProvider, AutoCloseable, RpcbenchmarkService {

    private static final Logger LOG = LoggerFactory.getLogger(RpcbenchmarkProvider.class);
    private static final GlobalBindingRTCServer gServer = new GlobalBindingRTCServer();
    private static final int testTimeout = 5;
    private final AtomicReference<ExecStatus> execStatus = new AtomicReference<>(ExecStatus.Idle);
    private RpcConsumerRegistry consumerRegistry;
    private RpcProviderRegistry providerRegistry;

    @Override
    public void onSessionInitiated(ProviderContext session) {
        LOG.info("RpcbenchmarkProvider Session Initiated");
        consumerRegistry = session.getSALService(RpcConsumerRegistry.class);
        providerRegistry = session.getSALService(RpcProviderRegistry.class);

        // Register the benchmark Global RPC
        session.addRpcImplementation(RpcbenchPayloadService.class, gServer);
        // Register RPC Benchmark's control REST API
        session.addRpcImplementation(RpcbenchmarkService.class, this);
    }

    @Override
    public void close() throws Exception {
        LOG.info("RpcbenchmarkProvider Closed");
    }

    @Override
    public Future<RpcResult<StartTestOutput>> startTest(final StartTestInput input) {
        LOG.info("startTest {}", input);

        final RTCClient client;
        final List<RoutedRpcRegistration<?>> rpcRegs = new ArrayList<>();

        switch (input.getOperation()) {
        case ROUTEDRTC:
            List<InstanceIdentifier<?>> routeIid = new ArrayList<>();
            for (int i = 0; i < input.getNumServers().intValue(); i++) {
                GlobalBindingRTCServer server = new GlobalBindingRTCServer();
                RoutedRpcRegistration<RpcbenchPayloadService> routedReg =
                        providerRegistry.addRoutedRpcImplementation(RpcbenchPayloadService.class, server);

                KeyedInstanceIdentifier<RpcRoute, RpcRouteKey> iid =
                        InstanceIdentifier
                            .create(RpcbenchRpcRoutes.class)
                            .child(RpcRoute.class, new RpcRouteKey(Integer.toString(i)));
                routeIid.add(iid);
                routedReg.registerPath(NodeContext.class, iid);
                rpcRegs.add(routedReg);
            }

            client = new RoutedBindingRTClient(consumerRegistry, input.getPayloadSize().intValue(), routeIid);
            break;

        case GLOBALRTC:
            client = new GlobalBindingRTCClient(consumerRegistry, input.getPayloadSize().intValue());
            break;

        default:
            LOG.error("Unsupported server/client type {}", input.getOperation());
            throw new IllegalArgumentException("Unsupported server/client type" + input.getOperation());
        }

        try {
            ExecutorService executor = Executors.newFixedThreadPool(input.getNumClients().intValue());

            final Runnable testRun = new Runnable() {
                @Override
                public void run() {
                    client.runTest(input.getIterations().intValue());
                }
            };

            LOG.info("Test Started");
            long startTime = System.nanoTime();

            for (int i = 0; i < input.getNumClients().intValue(); i++ ) {
                executor.submit(testRun);
            }

            executor.shutdown();
            try {
                executor.awaitTermination(testTimeout, TimeUnit.MINUTES);
            } catch (InterruptedException e) {
                LOG.error("Out of time: test did not finish within the {} min deadline ", testTimeout); 
            }

            long endTime = System.nanoTime();
            LOG.info("Test Done");

            long elapsedTime = endTime - startTime;

            StartTestOutput output = new StartTestOutputBuilder()
                                            .setRate((long)0)
                                            .setGlobalRtcClientError(client.getRpcError())
                                            .setGlobalRtcClientOk(client.getRpcOk())
                                            .setExecTime(TimeUnit.NANOSECONDS.toMillis(elapsedTime))
                                            .setRate(((client.getRpcOk() + client.getRpcError()) * 1000000000) / elapsedTime)
                                            .build();
            return RpcResultBuilder.success(output).buildFuture();
        } finally {
            for (RoutedRpcRegistration<?> routedRpcRegistration : rpcRegs) {
                routedRpcRegistration.close();
            }
        }
    }

    @Override
    public Future<RpcResult<TestStatusOutput>> testStatus() {
        LOG.info("testStatus");
        TestStatusOutput output = new TestStatusOutputBuilder()
                                        .setGlobalServerCnt((long)gServer.getNumRpcs())
                                        .setExecStatus(execStatus.get())
                                        .build();
        return RpcResultBuilder.success(output).buildFuture();
    }

}
