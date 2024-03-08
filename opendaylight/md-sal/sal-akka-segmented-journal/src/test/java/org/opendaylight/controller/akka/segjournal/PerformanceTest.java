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
import akka.testkit.CallingThreadDispatcher;
import akka.testkit.javadsl.TestKit;
import com.google.common.base.Stopwatch;
import io.atomix.storage.journal.StorageLevel;
import java.io.File;
import java.io.Serializable;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opendaylight.controller.akka.segjournal.SegmentedJournalActor.WriteMessages;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.concurrent.Future;

class PerformanceTest {
    private static final class Payload implements Serializable {
        @java.io.Serial
        private static final long serialVersionUID = 1L;

        final byte[] bytes = new byte[10240];

        Payload(final ThreadLocalRandom random) {
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
    private static final int SEGMENT_SIZE = 128 * 1024 * 1024;
    private static final int MESSAGE_SIZE = 16 * 1024 * 1024;

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
        actor = kit.childActorOf(SegmentedJournalActor.props("perf", DIRECTORY, StorageLevel.MAPPED, MESSAGE_SIZE,
            SEGMENT_SIZE).withDispatcher(CallingThreadDispatcher.Id()));
    }

    @AfterEach
    void after() {
        actor.tell(PoisonPill.getInstance(), ActorRef.noSender());
    }

    @Test
    void testMillionMessages() {
        final var random = ThreadLocalRandom.current();
        final var sw = Stopwatch.createStarted();
        final var payloads = new Payload[1_000];
        for (int i = 0; i < payloads.length; ++i) {
            payloads[i] = new Payload(random);
        }
        LOG.info("{} payloads created in {}", payloads.length, sw.stop());

        sw.reset().start();
        final var requests = new Request[100_000];
        for (int i = 0; i < requests.length; ++i) {
            requests[i] = new Request(AtomicWrite.apply(PersistentRepr.apply(payloads[random.nextInt(payloads.length)],
                i, "foo", null, false, kit.getRef(), "uuid")));
        }
        LOG.info("{} requests created in {}", requests.length, sw.stop());

        sw.reset().start();

        long started = System.nanoTime();
        long min = Long.MAX_VALUE;
        long max = Long.MIN_VALUE;
        for (var req : requests) {
            actor.tell(req.write, ActorRef.noSender());
            assertTrue(req.future.isCompleted());
            assertTrue(req.future.value().get().get().isEmpty());

            final long now = System.nanoTime();
            final long elapsed = now - started;
            started = now;
            if (elapsed < min) {
                min = elapsed;
            }
            if (elapsed > max) {
                max = elapsed;
            }
        }
        LOG.info("{} requests completed in {}, min={}ns max={}ns", requests.length, sw.stop(), min, max);
    }
}
