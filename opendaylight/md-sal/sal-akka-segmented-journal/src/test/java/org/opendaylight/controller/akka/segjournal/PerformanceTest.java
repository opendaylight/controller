/*
 * Copyright (c) 2024 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.akka.segjournal;

import static org.junit.jupiter.api.Assertions.assertTrue;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.PoisonPill;
import akka.persistence.AtomicWrite;
import akka.persistence.PersistentRepr;
import akka.testkit.javadsl.TestKit;
import com.google.common.base.Stopwatch;
import com.google.common.base.Ticker;
import io.atomix.storage.journal.StorageLevel;
import java.io.File;
import java.io.Serializable;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;
import org.apache.commons.io.FileUtils;
import org.awaitility.Awaitility;
import org.awaitility.Durations;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.opendaylight.controller.akka.segjournal.SegmentedJournalActor.WriteMessages;
import org.opendaylight.controller.cluster.common.actor.MeteringBehavior;
import org.opendaylight.controller.cluster.reporting.MetricsReporter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.concurrent.Future;

class PerformanceTest {
    private static final class Payload implements Serializable {
        @java.io.Serial
        private static final long serialVersionUID = 1L;

        final byte[] bytes;

        Payload(final int size, final ThreadLocalRandom random) {
            bytes = new byte[size];
            random.nextBytes(bytes);
        }
    }

    private static final class Request {
        final WriteMessages write = new WriteMessages();
        final Future<Optional<Exception>> future;

        Request(final AtomicWrite atomicWrite) {
            future = write.add(atomicWrite);
        }
    }

    private static final Logger LOG = LoggerFactory.getLogger(PerformanceTest.class);
    private static final File DIRECTORY = new File("target/sfj-perf");

    private static ActorSystem SYSTEM;

    private TestKit kit;
    private ActorRef actor;

    @BeforeAll
    static void beforeClass() {
        SYSTEM = ActorSystem.create("test");
    }

    @AfterAll
    static void afterClass() {
        TestKit.shutdownActorSystem(SYSTEM);
        SYSTEM = null;
    }

    @BeforeEach
    void before() {
        kit = new TestKit(SYSTEM);
        FileUtils.deleteQuietly(DIRECTORY);
    }

    @AfterEach
    void after() {
        if (actor != null) {
            actor.tell(PoisonPill.getInstance(), ActorRef.noSender());
        }
        FileUtils.deleteQuietly(DIRECTORY);
    }

    @Disabled("Disable due to being an extensive time hog")
    @ParameterizedTest
    @MethodSource
    void writeRequests(final StorageLevel storage, final int maxEntrySize, final int maxSegmentSize,
            final int payloadSize, final int requestCount) {
        LOG.info("Test {} entrySize={} segmentSize={} payload={} count={}", storage, maxEntrySize, maxSegmentSize,
            payloadSize, requestCount);

        // reset metrics
        final var metricsRegistry = MetricsReporter.getInstance(MeteringBehavior.DOMAIN).getMetricsRegistry();
        final var keys = metricsRegistry.getMetrics().keySet();
        keys.forEach(metricsRegistry::remove);

        actor = kit.childActorOf(
            SegmentedJournalActor.props("perf", DIRECTORY, storage, maxEntrySize, maxSegmentSize, maxEntrySize * 8));

        final var random = ThreadLocalRandom.current();
        final var sw = Stopwatch.createStarted();
        final var payloads = new Payload[1_000];
        for (int i = 0; i < payloads.length; ++i) {
            payloads[i] = new Payload(payloadSize, random);
        }
        LOG.info("{} payloads created in {}", payloads.length, sw.stop());

        sw.reset().start();
        final var requests = new Request[requestCount];
        for (int i = 0; i < requests.length; ++i) {
            requests[i] = new Request(AtomicWrite.apply(PersistentRepr.apply(payloads[random.nextInt(payloads.length)],
                i, "foo", null, false, kit.getRef(), "uuid")));
        }
        LOG.info("{} requests created in {}", requests.length, sw.stop());

        // send all requests asynchronously
        sw.reset().start();
        for (var req : requests) {
            actor.tell(req.write, ActorRef.noSender());
        }
        LOG.info("All requests sent in {}", sw.stop());

        // retrieve results
        sw.reset().start();
        for (var req : requests) {
            Awaitility.await().atMost(Durations.FIVE_HUNDRED_MILLISECONDS).until(req.future::isCompleted);
            assertTrue(req.future.value().get().get().isEmpty());
        }
        LOG.info("All results gathered in {}", sw.stop());

        // Log metrics collected
        // meters
        metricsRegistry.getMeters().forEach((key, meter) -> {
            final var meterId = toMetricId(key);
            LOG.info("{}: Count =       {}", meterId, meter.getCount());
            LOG.info("{}: Mean Rate =   {}", meterId, meter.getMeanRate());
            LOG.info("{}: 1 Min Rate =  {}", meterId, meter.getOneMinuteRate());
            LOG.info("{}: 5 Min Rate =  {}", meterId, meter.getFiveMinuteRate());
            LOG.info("{}: 15 Min Rate = {}", meterId, meter.getFifteenMinuteRate());
        });
        // timers
        metricsRegistry.getTimers().forEach((key, timer) -> {
            final var meterId = toMetricId(key);
            final var snap = timer.getSnapshot();
            LOG.info("{}: Min =    {}", meterId, formatNanos(snap.getMin()));
            LOG.info("{}: Max =    {}", meterId, formatNanos(snap.getMax()));
            LOG.info("{}: Mean =   {}", meterId, formatNanos(snap.getMean()));
            LOG.info("{}: StdDev = {}", meterId, formatNanos(snap.getStdDev()));
            LOG.info("{}: Median = {}", meterId, formatNanos(snap.getMedian()));
            LOG.info("{}: 75th =   {}", meterId, formatNanos(snap.get75thPercentile()));
            LOG.info("{}: 95th:    {}", meterId, formatNanos(snap.get95thPercentile()));
            LOG.info("{}: 98th:    {}", meterId, formatNanos(snap.get98thPercentile()));
            LOG.info("{}: 99th:    {}", meterId, formatNanos(snap.get99thPercentile()));
            LOG.info("{}: 99.9th:  {}", meterId, formatNanos(snap.get999thPercentile()));
        });
        // histograms
        metricsRegistry.getHistograms().forEach((key, histogram) -> {
            final var meterId = toMetricId(key);
            final var snap = histogram.getSnapshot();
            LOG.info("{}: Min =    {}", meterId, snap.getMin());
            LOG.info("{}: Max =    {}", meterId, snap.getMax());
            LOG.info("{}: Mean =   {}", meterId, snap.getMean());
            LOG.info("{}: StdDev = {}", meterId, snap.getStdDev());
            LOG.info("{}: Median = {}", meterId, snap.getMedian());
            LOG.info("{}: 75th =   {}", meterId, snap.get75thPercentile());
            LOG.info("{}: 95th:    {}", meterId, snap.get95thPercentile());
            LOG.info("{}: 98th:    {}", meterId, snap.get98thPercentile());
            LOG.info("{}: 99th:    {}", meterId, snap.get99thPercentile());
            LOG.info("{}: 99.9th:  {}", meterId, snap.get999thPercentile());
        });
    }

    private static List<Arguments> writeRequests() {
        return List.of(
            // DISK:
            // 100K requests, 10K each, 16M max, 128M segment
            Arguments.of(StorageLevel.DISK, 16 * 1024 * 1024, 128 * 1024 * 1024,    10_000,  100_000),
            // 100K requests, 10K each, 1M max, 16M segment
            Arguments.of(StorageLevel.DISK,      1024 * 1024,  16 * 1024 * 1024,    10_000,  100_000),
            // 10K requests, 100K each, 1M max, 16M segment
            Arguments.of(StorageLevel.DISK,      1024 * 1024,  16 * 1024 * 1024,   100_000,   10_000),
            // 1K requests, 1M each, 1M max, 16M segment
            Arguments.of(StorageLevel.DISK,      1024 * 1024,  16 * 1024 * 1024, 1_000_000,    1_000),

            // MAPPED:
            // 100K requests, 10K each, 16M max, 128M segment
            Arguments.of(StorageLevel.MAPPED, 16 * 1024 * 1024, 128 * 1024 * 1024,    10_000,  100_000),
            // 100K requests, 10K each, 1M max, 16M segment
            Arguments.of(StorageLevel.MAPPED,      1024 * 1024,  16 * 1024 * 1024,    10_000,  100_000),
            // 10K requests, 100K each, 1M max, 16M segment
            Arguments.of(StorageLevel.MAPPED,      1024 * 1024,  16 * 1024 * 1024,   100_000,   10_000),
            // 1K requests, 1M each, 1M max, 16M segment
            Arguments.of(StorageLevel.MAPPED,      1024 * 1024,  16 * 1024 * 1024, 1_000_000,    1_000));

    }

    private static String formatNanos(final double nanos) {
        return formatNanos(Math.round(nanos));
    }

    private static String formatNanos(final long nanos) {
        return Stopwatch.createStarted(new Ticker() {
            boolean started;

            @Override
            public long read() {
                if (started) {
                    return nanos;
                }
                started = true;
                return 0;
            }
        }).toString();
    }

    private static String toMetricId(final String metricKey) {
        return metricKey.substring(metricKey.lastIndexOf('.') + 1);
    }
}
