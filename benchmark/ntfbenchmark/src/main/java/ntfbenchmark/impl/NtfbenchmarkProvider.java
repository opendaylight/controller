/*
 * Copyright (c) 2015 Cisco Systems Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package ntfbenchmark.impl;

import static com.google.common.base.Verify.verifyNotNull;
import static java.util.Objects.requireNonNull;

import com.google.common.util.concurrent.ListenableFuture;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.opendaylight.mdsal.binding.api.NotificationPublishService;
import org.opendaylight.mdsal.binding.api.NotificationService;
import org.opendaylight.mdsal.binding.api.RpcProviderService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ntfbenchmark.rev150105.NtfbenchmarkService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ntfbenchmark.rev150105.StartTestInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ntfbenchmark.rev150105.StartTestInput.ProducerType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ntfbenchmark.rev150105.StartTestOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ntfbenchmark.rev150105.StartTestOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ntfbenchmark.rev150105.TestStatusInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ntfbenchmark.rev150105.TestStatusOutput;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.concepts.Registration;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;
import org.opendaylight.yangtools.yang.common.Uint32;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
@Component(service = {})
public final class NtfbenchmarkProvider implements AutoCloseable, NtfbenchmarkService {
    private static final Logger LOG = LoggerFactory.getLogger(NtfbenchmarkProvider.class);
    private static final int TEST_TIMEOUT = 5;

    private final NotificationService listenService;
    private final NotificationPublishService publishService;
    private final Registration reg;

    @Inject
    @Activate
    public NtfbenchmarkProvider(@Reference final NotificationService listenService,
            @Reference final NotificationPublishService publishService,
            @Reference final RpcProviderService rpcService) {
        this.listenService = requireNonNull(listenService);
        this.publishService = requireNonNull(publishService);
        reg = rpcService.registerRpcImplementation(NtfbenchmarkService.class, this);
        LOG.debug("NtfbenchmarkProvider initiated");
    }

    @Override
    @PreDestroy
    @Deactivate
    public void close() {
        reg.close();
        LOG.info("NtfbenchmarkProvider closed");
    }

    @Override
    public ListenableFuture<RpcResult<StartTestOutput>> startTest(final StartTestInput input) {
        final int producerCount = input.getProducers().intValue();
        final int listenerCount = input.getListeners().intValue();
        final int iterations = input.getIterations().intValue();
        final int payloadSize = input.getIterations().intValue();

        final List<AbstractNtfbenchProducer> producers = new ArrayList<>(producerCount);
        final List<ListenerRegistration<NtfbenchTestListener>> listeners = new ArrayList<>(listenerCount);
        for (int i = 0; i < producerCount; i++) {
            producers.add(new NtfbenchBlockingProducer(publishService, iterations, payloadSize));
        }
        int expectedCntPerListener = producerCount * iterations;

        for (int i = 0; i < listenerCount; i++) {
            final NtfbenchTestListener listener;
            if (input.getProducerType() == ProducerType.BLOCKING) {
                listener = new NtfbenchWTCListener(payloadSize, expectedCntPerListener);
            } else {
                listener = new NtfbenchTestListener(payloadSize);
            }
            listeners.add(listenService.registerNotificationListener(listener));
        }

        try {
            final ExecutorService executor = Executors.newFixedThreadPool(input.getProducers().intValue());

            LOG.info("Test Started");
            final long startTime = System.nanoTime();

            for (int i = 0; i < input.getProducers().intValue(); i++) {
                // FIXME: fools RV_RETURN_VALUE_IGNORED_BAD_PRACTICE for now, but we should check some more
                verifyNotNull(executor.submit(producers.get(i)));
            }
            executor.shutdown();
            try {
                executor.awaitTermination(TEST_TIMEOUT, TimeUnit.MINUTES);
                for (ListenerRegistration<NtfbenchTestListener> listenerRegistration : listeners) {
                    listenerRegistration.getInstance().getAllDone().get();
                }
            } catch (final InterruptedException | ExecutionException e) {
                LOG.error("Out of time: test did not finish within the {} min deadline ", TEST_TIMEOUT);
            }

            final long producerEndTime = System.nanoTime();
            final long producerElapsedTime = producerEndTime - startTime;

            long allListeners = 0;
            long allProducersOk = 0;
            long allProducersError = 0;

            for (final ListenerRegistration<NtfbenchTestListener> listenerRegistration : listeners) {
                allListeners += listenerRegistration.getInstance().getReceived();
            }

            final long listenerElapsedTime = producerEndTime - startTime;

            LOG.info("Test Done");

            for (final AbstractNtfbenchProducer abstractNtfbenchProducer : producers) {
                allProducersOk += abstractNtfbenchProducer.getNtfOk();
                allProducersError += abstractNtfbenchProducer.getNtfError();
            }

            final StartTestOutput output = new StartTestOutputBuilder()
                .setProducerElapsedTime(Uint32.valueOf(producerElapsedTime / 1000000))
                .setListenerElapsedTime(Uint32.valueOf(listenerElapsedTime / 1000000))
                .setListenerOk(Uint32.valueOf(allListeners))
                .setProducerOk(Uint32.valueOf(allProducersOk))
                .setProducerError(Uint32.valueOf(allProducersError))
                .setProducerRate(
                    Uint32.valueOf((allProducersOk + allProducersError) * 1000000000/ producerElapsedTime))
                .setListenerRate(Uint32.valueOf(allListeners * 1000000000 / listenerElapsedTime))
                .build();
            return RpcResultBuilder.success(output).buildFuture();
        } finally {
            for (final ListenerRegistration<NtfbenchTestListener> listenerRegistration : listeners) {
                listenerRegistration.close();
            }
        }
    }

    @Override
    public ListenableFuture<RpcResult<TestStatusOutput>> testStatus(final TestStatusInput input) {
        throw new UnsupportedOperationException("Not implemented");
    }
}
