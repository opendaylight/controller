/*
 * Copyright (c) 2015 Cisco Systems Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package rpcbenchmark.impl;

import static com.google.common.base.Verify.verifyNotNull;
import static java.util.Objects.requireNonNull;

import com.google.common.util.concurrent.ListenableFuture;
import java.util.ArrayList;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.opendaylight.mdsal.binding.api.RpcProviderService;
import org.opendaylight.mdsal.binding.api.RpcService;
import org.opendaylight.yang.gen.v1.rpcbench.payload.rev150702.RpcbenchRpcRoutes;
import org.opendaylight.yang.gen.v1.rpcbench.payload.rev150702.rpcbench.rpc.routes.RpcRoute;
import org.opendaylight.yang.gen.v1.rpcbench.payload.rev150702.rpcbench.rpc.routes.RpcRouteKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rpcbenchmark.rev150702.StartTest;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rpcbenchmark.rev150702.StartTestInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rpcbenchmark.rev150702.StartTestOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rpcbenchmark.rev150702.StartTestOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rpcbenchmark.rev150702.TestStatus;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rpcbenchmark.rev150702.TestStatusInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rpcbenchmark.rev150702.TestStatusOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rpcbenchmark.rev150702.TestStatusOutput.ExecStatus;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rpcbenchmark.rev150702.TestStatusOutputBuilder;
import org.opendaylight.yangtools.binding.DataObjectIdentifier;
import org.opendaylight.yangtools.concepts.Registration;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;
import org.opendaylight.yangtools.yang.common.Uint32;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.RequireServiceComponentRuntime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
@Component(service = {})
@RequireServiceComponentRuntime
public final class RpcbenchmarkProvider implements AutoCloseable {
    private static final Logger LOG = LoggerFactory.getLogger(RpcbenchmarkProvider.class);
    private static final int TEST_TIMEOUT = 5;

    private final AtomicReference<ExecStatus> execStatus = new AtomicReference<>(ExecStatus.Idle);
    private final RpcProviderService providerRegistry;
    private final RpcService consumerRegistry;
    private final GlobalBindingRTCServer globalServer;
    private final Registration reg;

    @Inject
    @Activate
    public RpcbenchmarkProvider(@Reference final RpcProviderService providerRegistry,
            @Reference final RpcService consumerRegistry) {
        this.providerRegistry = requireNonNull(providerRegistry);
        this.consumerRegistry = requireNonNull(consumerRegistry);
        globalServer = new GlobalBindingRTCServer(providerRegistry);
        reg = providerRegistry.registerRpcImplementations((TestStatus) this::testStatus, (StartTest) this::startTest);
        LOG.info("RpcbenchmarkProvider initiated");
    }

    @Override
    @Deactivate
    @PreDestroy
    public void close() {
        globalServer.close();
        reg.close();
        LOG.info("RpcbenchmarkProvider closed");
    }

    private ListenableFuture<RpcResult<StartTestOutput>> startTest(final StartTestInput input) {
        LOG.debug("startTest {}", input);

        final RTCClient client;
        RoutedBindingRTCServer routed = null;

        switch (input.getOperation()) {
            case ROUTEDRTC -> {
                final var routeIid = new ArrayList<DataObjectIdentifier<?>>();
                for (int i = 0; i < input.getNumServers().intValue(); i++) {
                    routeIid.add(DataObjectIdentifier.builder(RpcbenchRpcRoutes.class)
                        .child(RpcRoute.class, new RpcRouteKey(Integer.toString(i)))
                        .build());
                }

                routed = new RoutedBindingRTCServer(providerRegistry, Set.copyOf(routeIid));
                client = new RoutedBindingRTClient(consumerRegistry, input.getPayloadSize().intValue(), routeIid);
            }
            case GLOBALRTC -> {
                client = new GlobalBindingRTCClient(consumerRegistry, input.getPayloadSize().intValue());
            }
            default -> {
                LOG.error("Unsupported server/client type {}", input.getOperation());
                throw new IllegalArgumentException("Unsupported server/client type" + input.getOperation());
            }
        }

        try {
            ExecutorService executor = Executors.newFixedThreadPool(input.getNumClients().intValue());

            final Runnable testRun = () -> client.runTest(input.getIterations().intValue());

            LOG.info("Test Started");
            final long startTime = System.nanoTime();

            for (int i = 0; i < input.getNumClients().intValue(); i++) {
                // FIXME: fools RV_RETURN_VALUE_IGNORED_BAD_PRACTICE, but we should check more
                verifyNotNull(executor.submit(testRun));
            }

            executor.shutdown();
            try {
                executor.awaitTermination(TEST_TIMEOUT, TimeUnit.MINUTES);
            } catch (final InterruptedException e) {
                LOG.error("Out of time: test did not finish within the {} min deadline ", TEST_TIMEOUT);
            }

            long endTime = System.nanoTime();
            LOG.info("Test Done");

            long elapsedTime = endTime - startTime;

            StartTestOutput output = new StartTestOutputBuilder()
                                            .setRate(Uint32.ZERO)
                                            .setGlobalRtcClientError(Uint32.valueOf(client.getRpcError()))
                                            .setGlobalRtcClientOk(Uint32.valueOf(client.getRpcOk()))
                                            .setExecTime(Uint32.valueOf(TimeUnit.NANOSECONDS.toMillis(elapsedTime)))
                                            .setRate(Uint32.valueOf(
                                                (client.getRpcOk() + client.getRpcError()) * 1000000000 / elapsedTime))
                                            .build();
            return RpcResultBuilder.success(output).buildFuture();
        } finally {
            if (routed != null) {
                routed.close();
            }
        }
    }

    private ListenableFuture<RpcResult<TestStatusOutput>> testStatus(final TestStatusInput input) {
        LOG.info("testStatus");
        TestStatusOutput output = new TestStatusOutputBuilder()
                                        .setGlobalServerCnt(Uint32.valueOf(globalServer.getNumRpcs()))
                                        .setExecStatus(execStatus.get())
                                        .build();
        return RpcResultBuilder.success(output).buildFuture();
    }

}
