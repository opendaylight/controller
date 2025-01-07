/*
 * Copyright (c) 2024 PANTHEON.tech s.r.o. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.akka.segjournal;

import static org.opendaylight.controller.akka.segjournal.BenchmarkUtils.buildConfig;
import static org.opendaylight.controller.akka.segjournal.BenchmarkUtils.formatBytes;
import static org.opendaylight.controller.akka.segjournal.BenchmarkUtils.formatNanos;
import static org.opendaylight.controller.akka.segjournal.BenchmarkUtils.toMetricId;

import com.google.common.base.Stopwatch;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import java.io.Serializable;
import java.nio.file.Files;
import java.util.Optional;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import org.apache.commons.io.FileUtils;
import org.apache.pekko.actor.ActorRef;
import org.apache.pekko.actor.ActorSystem;
import org.apache.pekko.persistence.AtomicWrite;
import org.apache.pekko.persistence.PersistentRepr;
import org.opendaylight.controller.akka.segjournal.BenchmarkUtils.BenchmarkConfig;
import org.opendaylight.controller.akka.segjournal.SegmentedJournalActor.WriteMessages;
import org.opendaylight.controller.cluster.common.actor.MeteringBehavior;
import org.opendaylight.controller.cluster.reporting.MetricsReporter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.concurrent.Future;

public final class BenchmarkMain {
    private static final String BENCHMARK = "benchmark";
    private static final Logger LOG = LoggerFactory.getLogger("benchmark");

    public static void main(final String[] args) {
        final var config = buildConfig(args);
        final var benchmark = new BenchmarkMain(config);
        Runtime.getRuntime().addShutdownHook(new Thread(benchmark::shutdown));
        benchmark.execute();
        System.exit(0);
    }

    private final BenchmarkConfig config;
    private final ActorSystem system;
    private final ScheduledExecutorService executor;
    private ActorRef actor;

    private BenchmarkMain(final BenchmarkConfig config) {
        this.config = config;
        system = ActorSystem.create(BENCHMARK);
        executor = Executors.newSingleThreadScheduledExecutor(
            new ThreadFactoryBuilder().setNameFormat("progress-check-%d").build());
    }

    void execute() {
        LOG.info("Starting with settings");
        LOG.info("\tstorage            : {}", config.storage());
        LOG.info("\tworking dir        : {}", config.workingDir().toAbsolutePath());
        LOG.info("\tmaxEntrySize       : {}", formatBytes(config.maxEntrySize()));
        LOG.info("\tmaxSegmentSize     : {}", formatBytes(config.maxSegmentSize()));
        LOG.info("\tmaxUnflushedBytes  : {}", formatBytes(config.maxUnflushedBytes()));

        final var minLoadSize = Math.round(config.payloadSize() * 0.8f);
        final var maxLoadSize = Math.min(Math.round(config.payloadSize() * 1.2f), config.maxEntrySize());
        LOG.info("Preparing load");
        LOG.info("\tnumber of messages : {}", config.messagesNum());
        LOG.info("\tpayload size       : {} .. {}", formatBytes(minLoadSize), formatBytes(maxLoadSize));

        // reset metrics
        final var metricsRegistry = MetricsReporter.getInstance(MeteringBehavior.DOMAIN).getMetricsRegistry();
        final var keys = metricsRegistry.getMetrics().keySet();
        keys.forEach(metricsRegistry::remove);

        // get actor
        actor = system.actorOf(
            SegmentedJournalActor.props("perf", config.workingDir(), config.storage(),
                config.maxEntrySize(), config.maxSegmentSize(), config.maxUnflushedBytes()));

        // randomize payloads
        final var random = ThreadLocalRandom.current();
        final var payloads = new Payload[1_000];
        for (int i = 0; i < payloads.length; ++i) {
            final var bytes = new byte[random.nextInt(minLoadSize, maxLoadSize)];
            random.nextBytes(bytes);
            payloads[i] = new Payload(bytes);
        }

        // enable periodic check for completed writes
        final var results = new ConcurrentLinkedQueue<Future<Optional<Exception>>>();
        final var progressReporter =
            new ProgressReporter(executor, results, config.messagesNum(), 10, TimeUnit.SECONDS);

        // start async message writing
        final var sw = Stopwatch.createStarted();
        for (int i = 0; i < config.messagesNum(); ++i) {
            results.add(writeMessage(i, payloads[random.nextInt(payloads.length)]));
        }
        LOG.info("{} Messages sent to akka in {}", config.messagesNum(), sw);

        // await completion
        try {
            progressReporter.awaitCompletion();
        } catch (InterruptedException e) {
            LOG.error("Interrupted", e);
        }
        LOG.info("Messages written in {}", sw.stop());

        // report
        LOG.info("Following metrics collected");
        // meters
        metricsRegistry.getMeters().forEach((key, meter) -> {
            LOG.info("Meter '{}'", toMetricId(key));
            LOG.info("\tCount       = {}", meter.getCount());
            LOG.info("\tMean Rate   = {}", meter.getMeanRate());
            LOG.info("\t1 Min Rate  = {}", meter.getOneMinuteRate());
            LOG.info("\t5 Min Rate  = {}", meter.getFiveMinuteRate());
            LOG.info("\t15 Min Rate = {}", meter.getFifteenMinuteRate());
        });
        // timers
        metricsRegistry.getTimers().forEach((key, timer) -> {
            LOG.info("Timer '{}'", toMetricId(key));
            final var snap = timer.getSnapshot();
            LOG.info("\tMin         = {}", formatNanos(snap.getMin()));
            LOG.info("\tMax         = {}", formatNanos(snap.getMax()));
            LOG.info("\tMean        = {}", formatNanos(snap.getMean()));
            LOG.info("\tStdDev      = {}", formatNanos(snap.getStdDev()));
            LOG.info("\tMedian      = {}", formatNanos(snap.getMedian()));
            LOG.info("\t75th        = {}", formatNanos(snap.get75thPercentile()));
            LOG.info("\t95th        = {}", formatNanos(snap.get95thPercentile()));
            LOG.info("\t98th        = {}", formatNanos(snap.get98thPercentile()));
            LOG.info("\t99th        = {}", formatNanos(snap.get99thPercentile()));
            LOG.info("\t99.9th      = {}", formatNanos(snap.get999thPercentile()));
        });
        // histograms
        metricsRegistry.getHistograms().forEach((key, histogram) -> {
            LOG.info("Histogram '{}'", toMetricId(key));
            final var snap = histogram.getSnapshot();
            LOG.info("\tMin         = {}", snap.getMin());
            LOG.info("\tMax         = {}", snap.getMax());
            LOG.info("\tMean        = {}", snap.getMean());
            LOG.info("\tStdDev      = {}", snap.getStdDev());
            LOG.info("\tMedian      = {}", snap.getMedian());
            LOG.info("\t75th        = {}", snap.get75thPercentile());
            LOG.info("\t95th        = {}", snap.get95thPercentile());
            LOG.info("\t98th        = {}", snap.get98thPercentile());
            LOG.info("\t99th        = {}", snap.get99thPercentile());
            LOG.info("\t99.9th      = {}", snap.get999thPercentile());
        });
    }

    Future<Optional<Exception>> writeMessage(final long seqNum, final Payload payload) {
        final var writeMessage = new WriteMessages();
        final var result = writeMessage.add(AtomicWrite.apply(
            PersistentRepr.apply(payload, seqNum, BENCHMARK, null, false, ActorRef.noSender(), "uuid")));
        actor.tell(writeMessage, ActorRef.noSender());
        return result;
    }

    void shutdown() {
        LOG.info("shutting down ...");
        executor.shutdown();
        if (actor != null) {
            system.stop(actor);
        }
        final var workDir = config.workingDir();
        if (Files.exists(workDir)) {
            FileUtils.deleteQuietly(workDir.toFile());
        }
        system.terminate();
        LOG.info("Done.");
    }

    private static final class Payload implements Serializable {
        @java.io.Serial
        private static final long serialVersionUID = 1L;
        final byte[] bytes;

        Payload(final byte[] bytes) {
            this.bytes = bytes;
        }
    }

    private static final class ProgressReporter implements Runnable {
        final ScheduledExecutorService executor;
        final CountDownLatch latch = new CountDownLatch(1);
        final Queue<Future<Optional<Exception>>> queue;
        final long total;
        final int checkInterval;
        final TimeUnit timeUnit;
        long completed;
        long errors;

        ProgressReporter(final ScheduledExecutorService executor, final Queue<Future<Optional<Exception>>> queue,
            final long total, final int checkInterval, final TimeUnit timeUnit) {
            this.executor = executor;
            this.queue = queue;
            this.total = total;
            this.checkInterval = checkInterval;
            this.timeUnit = timeUnit;
            scheduleNextCheck();
        }

        @Override
        public void run() {
            // release completed from the beginning of the queue
            while (!queue.isEmpty() && queue.peek().isCompleted()) {
                final var future = queue.poll();
                completed++;
                if (!future.value().get().get().isEmpty()) {
                    errors++;
                }
            }
            LOG.info("{} of {} = {}% messages written, {} in queue",
                completed, total, completed * 100 / total, queue.size());
            if (total == completed) {
                LOG.info("Check completed, errors found : {}", errors);
                latch.countDown();
                return;
            }
            scheduleNextCheck();
        }

        void scheduleNextCheck() {
            executor.schedule(this, checkInterval, timeUnit);
        }

        void awaitCompletion() throws InterruptedException {
            latch.await();
        }
    }
}
