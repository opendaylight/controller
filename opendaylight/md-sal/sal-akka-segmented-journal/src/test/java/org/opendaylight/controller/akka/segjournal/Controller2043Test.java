/*
 * Copyright (c) 2022 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.akka.segjournal;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.lessThan;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

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
import java.util.concurrent.TimeUnit;
import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.opendaylight.controller.akka.segjournal.SegmentedJournalActor.WriteMessages;
import scala.concurrent.Future;

public class Controller2043Test {
    private static final File DIRECTORY = new File("target/controller-2043-test");
    private static final int SEGMENT_SIZE = 1024;
    private static final int MESSAGE_SIZE = 1024;
    private static ActorSystem SYSTEM;
    private TestKit kit;
    private ActorRef actor;

    @BeforeClass
    public static void beforeClass() {
        SYSTEM = ActorSystem.create("test");
    }

    @AfterClass
    public static void afterClass() {
        TestKit.shutdownActorSystem(SYSTEM);
        SYSTEM = null;
    }

    @Before
    public void before() {
        kit = new TestKit(SYSTEM);
        FileUtils.deleteQuietly(DIRECTORY);
        actor = actor();
    }

    @After
    public void after() {
        actor.tell(PoisonPill.getInstance(), ActorRef.noSender());
    }

    @Test
    public void controller2043Test() {
        for (int i = 1; i < 10; ++i) {
            final LargePayload payload = new LargePayload();
            final WriteMessages write = new WriteMessages();
            final Future<Optional<Exception>> requests = write.add(AtomicWrite.apply(PersistentRepr
                    .apply(payload, i, "foo", null, false, kit.getRef(), "uuid")));
            final Stopwatch timer = Stopwatch.createStarted();
            actor.tell(write, ActorRef.noSender());
            assertFalse(getFuture(requests).isPresent());
            assertThat(timer.elapsed(TimeUnit.SECONDS), lessThan(5L));
        }
    }

    private ActorRef actor() {
        return kit.childActorOf(SegmentedJournalActor.props("foo", DIRECTORY, StorageLevel.DISK, MESSAGE_SIZE,
                SEGMENT_SIZE).withDispatcher(CallingThreadDispatcher.Id()));
    }

    private static <T> T getFuture(final Future<T> future) {
        assertTrue(future.isCompleted());
        return future.value().get().get();
    }

    private static final class LargePayload implements Serializable {
        private static final long serialVersionUID = 1L;
        final byte[] bytes = new byte[8];
    }
}