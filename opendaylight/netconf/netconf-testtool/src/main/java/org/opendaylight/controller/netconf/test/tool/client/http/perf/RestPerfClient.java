/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.netconf.test.tool.client.http.perf;


import com.google.common.base.Charsets;
import com.google.common.base.Stopwatch;
import com.google.common.io.Files;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import net.sourceforge.argparse4j.inf.ArgumentParser;
import net.sourceforge.argparse4j.inf.ArgumentParserException;
import org.opendaylight.controller.netconf.test.tool.TestToolUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RestPerfClient {

    private static final Logger LOG = LoggerFactory.getLogger(RestPerfClient.class);

    private static final String HOST_KEY = "{HOST}";
    private static final String PORT_KEY = "{PORT}";
    private static final String DEVICE_PORT_KEY = "{DEVICE_PORT}";

    private static final String PEER_KEY = "{PEERID}";
    private static final String INT_LEAF_KEY = "{INTLEAF}";

    private static final String PHYS_ADDR_PLACEHOLDER = "{PHYS_ADDR}";

    private static final String dest = "http://{HOST}:{PORT}";

    private static long macStart = 0xAABBCCDD0000L;

    static int throttle;

    static final class DestToPayload {

        private final String destination;
        private final String payload;

        public DestToPayload(String destination, String payload) {
            this.destination = destination;
            this.payload = payload;
        }

        public String getDestination() {
            return destination;
        }

        public String getPayload() {
            return payload;
        }
    }

    public static void main(String[] args) throws IOException {

        Parameters parameters = parseArgs(args, Parameters.getParser());
        parameters.validate();
        throttle = parameters.throttle / parameters.threadAmount;

        if (parameters.async && parameters.threadAmount > 1) {
            LOG.info("Throttling per thread: {}", throttle);
        }

        final String editContentString;
        try {
            editContentString = Files.toString(parameters.editContent, Charsets.UTF_8);
        } catch (final IOException e) {
            throw new IllegalArgumentException("Cannot read content of " + parameters.editContent);
        }

        final int threadAmount = parameters.threadAmount;
        LOG.info("thread amount: {}", threadAmount);
        final int requestsPerThread = parameters.editCount / parameters.threadAmount;
        LOG.info("requestsPerThread: {}", requestsPerThread);
        final int leftoverRequests = parameters.editCount % parameters.threadAmount;
        LOG.info("leftoverRequests: {}", leftoverRequests);

        final ArrayList<ArrayList<DestToPayload>> allThreadsPayloads = new ArrayList<>();
        for (int i = 0; i < threadAmount; i++) {
            final ArrayList<DestToPayload> payloads = new ArrayList<>();
            for (int j = 0; j < requestsPerThread; j++) {
                final int devicePort = parameters.sameDevice ? parameters.devicePortRangeStart : parameters.devicePortRangeStart + i;
                final StringBuilder destBuilder = new StringBuilder(dest);
                destBuilder.replace(destBuilder.indexOf(HOST_KEY), destBuilder.indexOf(HOST_KEY) + HOST_KEY.length(), parameters.ip)
                        .replace(destBuilder.indexOf(PORT_KEY), destBuilder.indexOf(PORT_KEY) + PORT_KEY.length(), parameters.port + "");
                final StringBuilder suffixBuilder = new StringBuilder(parameters.destination);
                if (suffixBuilder.indexOf(DEVICE_PORT_KEY) != -1) {
                    suffixBuilder.replace(suffixBuilder.indexOf(DEVICE_PORT_KEY), suffixBuilder.indexOf(DEVICE_PORT_KEY) + DEVICE_PORT_KEY.length(), devicePort + "");
                }
                destBuilder.append(suffixBuilder);

                payloads.add(new DestToPayload(destBuilder.toString(), prepareMessage(i, j, editContentString)));
            }
            allThreadsPayloads.add(payloads);
        }

        for (int i = 0; i < leftoverRequests; i++) {
            ArrayList<DestToPayload> payloads = allThreadsPayloads.get(allThreadsPayloads.size() - 1);

            final int devicePort = parameters.sameDevice ? parameters.devicePortRangeStart : parameters.devicePortRangeStart + threadAmount - 1;
            final StringBuilder destBuilder = new StringBuilder(dest);
            destBuilder.replace(destBuilder.indexOf(HOST_KEY), destBuilder.indexOf(HOST_KEY) + HOST_KEY.length(), parameters.ip)
                    .replace(destBuilder.indexOf(PORT_KEY), destBuilder.indexOf(PORT_KEY) + PORT_KEY.length(), parameters.port + "");
            final StringBuilder suffixBuilder = new StringBuilder(parameters.destination);
            if (suffixBuilder.indexOf(DEVICE_PORT_KEY) != -1) {
                suffixBuilder.replace(suffixBuilder.indexOf(DEVICE_PORT_KEY), suffixBuilder.indexOf(DEVICE_PORT_KEY) + DEVICE_PORT_KEY.length(), devicePort + "");
            }
            destBuilder.append(suffixBuilder);
            payloads.add(new DestToPayload(destBuilder.toString(), prepareMessage(threadAmount - 1, requestsPerThread + i, editContentString)));
        }

        final ArrayList<PerfClientCallable> callables = new ArrayList<>();
        for (ArrayList<DestToPayload> payloads : allThreadsPayloads) {
            callables.add(new PerfClientCallable(parameters, payloads));
        }

        final ExecutorService executorService = Executors.newFixedThreadPool(threadAmount);

        LOG.info("Starting performance test");
        final Stopwatch started = Stopwatch.createStarted();
        try {
            final List<Future<Void>> futures = executorService.invokeAll(callables, parameters.timeout, TimeUnit.MINUTES);
            for (int i = 0; i < futures.size(); i++) {
                Future<Void> future = futures.get(i);
                if (future.isCancelled()) {
                    LOG.info("{}. thread timed out.", i + 1);
                } else {
                    try {
                        future.get();
                    } catch (final ExecutionException e) {
                        LOG.info("{}. thread failed.", i + 1, e);
                    }
                }
            }
        } catch (final InterruptedException e) {
            LOG.warn("Unable to execute requests", e);
        }
        executorService.shutdownNow();
        started.stop();

        LOG.info("FINISHED. Execution time: {}", started);
        LOG.info("Requests per second: {}", (parameters.editCount * 1000.0 / started.elapsed(TimeUnit.MILLISECONDS)));

        System.exit(0);
    }

    private static Parameters parseArgs(final String[] args, final ArgumentParser parser) {
        final Parameters opt = new Parameters();
        try {
            parser.parseArgs(args, opt);
            return opt;
        } catch (final ArgumentParserException e) {
            parser.handleError(e);
        }

        System.exit(1);
        return null;
    }

    private static String prepareMessage(final int idi, final int idj, final String editContentString) {
        StringBuilder messageBuilder = new StringBuilder(editContentString);
        if (editContentString.contains(PEER_KEY)) {
            messageBuilder.replace(messageBuilder.indexOf(PEER_KEY), messageBuilder.indexOf(PEER_KEY) + PEER_KEY.length(), Integer.toString(idi))
                    .replace(messageBuilder.indexOf(INT_LEAF_KEY), messageBuilder.indexOf(INT_LEAF_KEY) + INT_LEAF_KEY.length(), Integer.toString(idj));
        }

        int idx = messageBuilder.indexOf(PHYS_ADDR_PLACEHOLDER);

        while (idx != -1) {
            messageBuilder.replace(idx, idx + PHYS_ADDR_PLACEHOLDER.length(), TestToolUtils.getMac(macStart++));
            idx = messageBuilder.indexOf(PHYS_ADDR_PLACEHOLDER);
        }

        return messageBuilder.toString();
    }
}
