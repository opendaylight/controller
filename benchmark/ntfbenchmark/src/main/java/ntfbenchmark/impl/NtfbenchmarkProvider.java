/*
 * Copyright (c) 2015 Cisco Systems Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package ntfbenchmark.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.opendaylight.controller.md.sal.binding.api.NotificationPublishService;
import org.opendaylight.controller.md.sal.binding.api.NotificationService;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker.ProviderContext;
import org.opendaylight.controller.sal.binding.api.BindingAwareProvider;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ntfbenchmark.rev150105.NtfbenchmarkService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ntfbenchmark.rev150105.StartTestInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ntfbenchmark.rev150105.StartTestInput.ProducerType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ntfbenchmark.rev150105.StartTestOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ntfbenchmark.rev150105.StartTestOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ntfbenchmark.rev150105.TestStatusOutput;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NtfbenchmarkProvider implements BindingAwareProvider, AutoCloseable, NtfbenchmarkService {

    private static final Logger LOG = LoggerFactory.getLogger(NtfbenchmarkProvider.class);
    private NotificationService listenService;
    private NotificationPublishService publishService;
    private static final int testTimeout = 5;

    public NtfbenchmarkProvider(NotificationService listenServiceDependency,
            NotificationPublishService publishServiceDependency) {
        LOG.info("NtfbenchmarkProvider Constructor");
        listenService = listenServiceDependency;
        publishService = publishServiceDependency;
    }

    @Override
    public void onSessionInitiated(final ProviderContext session) {
        LOG.info("NtfbenchmarkProvider Session Initiated");
        session.addRpcImplementation(NtfbenchmarkService.class, this);
    }

    @Override
    public void close() throws Exception {
        LOG.info("NtfbenchmarkProvider Closed");
    }

    @Override
    public Future<RpcResult<StartTestOutput>> startTest(final StartTestInput input) {
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
                executor.submit(producers.get(i));
            }
            executor.shutdown();
            try {
                executor.awaitTermination(testTimeout, TimeUnit.MINUTES);
                for (ListenerRegistration<NtfbenchTestListener> listenerRegistration : listeners) {
                    listenerRegistration.getInstance().getAllDone().get();
                }
            } catch (final InterruptedException | ExecutionException e) {
                LOG.error("Out of time: test did not finish within the {} min deadline ", testTimeout);
            }

            final long producerEndTime = System.nanoTime();
            final long producerElapsedTime = producerEndTime - startTime;

            long allListeners = 0;
            long allProducersOk = 0;
            long allProducersError = 0;

            for (final ListenerRegistration<NtfbenchTestListener> listenerRegistration : listeners) {
                allListeners += listenerRegistration.getInstance().getReceived();
            }

            final long listenerEndTime = System.nanoTime();
            final long listenerElapsedTime = producerEndTime - startTime;

            LOG.info("Test Done");

            for (final AbstractNtfbenchProducer abstractNtfbenchProducer : producers) {
                allProducersOk += abstractNtfbenchProducer.getNtfOk();
                allProducersError += abstractNtfbenchProducer.getNtfError();
            }

            final StartTestOutput output =
                    new StartTestOutputBuilder()
                            .setProducerElapsedTime(producerElapsedTime / 1000000)
                            .setListenerElapsedTime(listenerElapsedTime / 1000000)
                            .setListenerOk(allListeners)
                            .setProducerOk(allProducersOk)
                            .setProducerError(allProducersError)
                            .setProducerRate(((allProducersOk + allProducersError) * 1000000000) / producerElapsedTime)
                            .setListenerRate((allListeners * 1000000000) / listenerElapsedTime)
                           .build();
            return RpcResultBuilder.success(output).buildFuture();
        } finally {
            for (final ListenerRegistration<NtfbenchTestListener> listenerRegistration : listeners) {
                listenerRegistration.close();
            }
        }
    }

    @Override
    public Future<RpcResult<TestStatusOutput>> testStatus() {
        // TODO Auto-generated method stub
        return null;
    }

}
