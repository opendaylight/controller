/*
 * Copyright (c) 2023 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.akka.segjournal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.PoisonPill;
import akka.persistence.PersistentRepr;
import akka.testkit.CallingThreadDispatcher;
import akka.testkit.javadsl.TestKit;
import io.atomix.storage.journal.StorageLevel;
import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import scala.concurrent.Future;

public class Controller2072Test {
    private static final File EXISTING_DIRECTORY = new File("src/test/resources/ExistingJournal");
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
        actor = actor();
    }

    @After
    public void after() {
        actor.tell(PoisonPill.getInstance(), ActorRef.noSender());
    }


    @Test
    public void legacyJournalReadTest() {
        var callback = new TestConsumer<PersistentRepr>();
        SegmentedJournalActor.AsyncMessage<Void> replay = SegmentedJournalActor
                .replayMessages(0, Long.MAX_VALUE, Long.MAX_VALUE, callback);
        actor.tell(replay, ActorRef.noSender());
        assertNull(get(replay));
        assertEquals(9, callback.accepted.size());
        callback.accepted.forEach(input -> {
            assertTrue(input.payload() instanceof LargePayload);
            assertNull(input.manifest());
            assertEquals("uuid", input.writerUuid());
        });
    }

    private ActorRef actor() {
        return kit.childActorOf(SegmentedJournalActor.props("foo", EXISTING_DIRECTORY, StorageLevel.DISK, MESSAGE_SIZE,
                SEGMENT_SIZE).withDispatcher(CallingThreadDispatcher.Id()));
    }

    private static <T> T get(final SegmentedJournalActor.AsyncMessage<T> message) {
        return getFuture(message.promise.future());
    }

    private static <T> T getFuture(final Future<T> future) {
        assertTrue(future.isCompleted());
        return future.value().get().get();
    }

    private static final class LargePayload implements Serializable {
        private static final long serialVersionUID = 1L;
        final byte[] bytes = new byte[8];
    }

    private static class TestConsumer<T> implements Consumer<T> {
        List<T> accepted = new ArrayList<>();

        @Override
        public void accept(T input) {
            accepted.add(input);
        }
    }
}