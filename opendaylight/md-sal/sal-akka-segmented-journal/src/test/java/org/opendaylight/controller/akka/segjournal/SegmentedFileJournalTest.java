/*
 * Copyright (c) 2019 Pantheon Technologies, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.akka.segjournal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.PoisonPill;
import akka.persistence.AtomicWrite;
import akka.persistence.PersistentRepr;
import akka.testkit.CallingThreadDispatcher;
import akka.testkit.javadsl.TestKit;
import io.atomix.storage.StorageLevel;
import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.opendaylight.controller.akka.segjournal.SegmentedJournalActor.AsyncMessage;
import org.opendaylight.controller.akka.segjournal.SegmentedJournalActor.WriteMessages;
import scala.concurrent.Future;

@RunWith(MockitoJUnitRunner.StrictStubs.class)
public class SegmentedFileJournalTest {
    private static final File DIRECTORY = new File("target/sfj-test");
    private static final int SEGMENT_SIZE = 1024 * 1024;
    private static final int MESSAGE_SIZE = 512 * 1024;

    private static ActorSystem SYSTEM;

    @Mock
    private Consumer<PersistentRepr> firstCallback;

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
    public void testDeleteAfterStop() {
        // Preliminary setup
        final WriteMessages write = new WriteMessages();
        final Future<Optional<Exception>> first = write.add(AtomicWrite.apply(PersistentRepr.apply("first", 1, "foo",
            null, false, kit.getRef(), "uuid")));
        final Future<Optional<Exception>> second = write.add(AtomicWrite.apply(PersistentRepr.apply("second", 2, "foo",
            null, false, kit.getRef(), "uuid")));
        actor.tell(write, ActorRef.noSender());
        assertFalse(getFuture(first).isPresent());
        assertFalse(getFuture(second).isPresent());

        assertHighestSequenceNr(2);
        assertReplayCount(2);

        deleteEntries(1);

        assertHighestSequenceNr(2);
        assertReplayCount(1);

        // Restart actor
        actor.tell(PoisonPill.getInstance(), ActorRef.noSender());
        actor = actor();

        // Check if state is retained
        assertHighestSequenceNr(2);
        assertReplayCount(1);
    }

    @Test
    public void testSegmentation() throws IOException {
        // We want to have roughly three segments
        final LargePayload payload = new LargePayload();

        final WriteMessages write = new WriteMessages();
        final List<Future<Optional<Exception>>> requests = new ArrayList<>();

        // Each payload is half of segment size, plus some overhead, should result in two segments being present
        for (int i = 1; i <= SEGMENT_SIZE * 3 / MESSAGE_SIZE; ++i) {
            requests.add(write.add(AtomicWrite.apply(PersistentRepr.apply(payload, i, "foo", null, false, kit.getRef(),
                "uuid"))));
        }

        actor.tell(write, ActorRef.noSender());
        requests.forEach(future -> assertFalse(getFuture(future).isPresent()));

        assertFileCount(2, 1);

        // Delete all but the last entry
        deleteEntries(requests.size());

        assertFileCount(1, 1);
    }

    @Test
    public void testComplexDeletesAndPartialReplays() throws Exception {
        for (int i = 0; i <= 4; i++) {
            writeBigPaylod();
        }

        assertFileCount(10, 1);

        // delete including index 3, so get rid of the first segment
        deleteEntries(3);
        assertFileCount(9, 1);

        // get rid of segments 2(index 4-6) and 3(index 7-9)
        deleteEntries(9);
        assertFileCount(7, 1);

        // get rid of all segments except the last one
        deleteEntries(27);
        assertFileCount(1, 1);

        restartActor();

        // Check if state is retained
        assertHighestSequenceNr(30);
        // 28,29,30 replayed
        assertReplayCount(3);


        deleteEntries(28);
        restartActor();

        assertHighestSequenceNr(30);
        // 29,30 replayed
        assertReplayCount(2);

        deleteEntries(29);
        restartActor();

        // 30 replayed
        assertReplayCount(1);

        deleteEntries(30);
        restartActor();

        // nothing replayed
        assertReplayCount(0);
    }

    private void restartActor() {
        actor.tell(PoisonPill.getInstance(), ActorRef.noSender());
        actor = actor();
    }

    private void writeBigPaylod() {
        final LargePayload payload = new LargePayload();

        final WriteMessages write = new WriteMessages();
        final List<Future<Optional<Exception>>> requests = new ArrayList<>();

        // Each payload is half of segment size, plus some overhead, should result in two segments being present
        for (int i = 1; i <= SEGMENT_SIZE * 3 / MESSAGE_SIZE; ++i) {
            requests.add(write.add(AtomicWrite.apply(PersistentRepr.apply(payload, i, "foo", null, false, kit.getRef(),
                    "uuid"))));
        }

        actor.tell(write, ActorRef.noSender());
        requests.forEach(future -> assertFalse(getFuture(future).isPresent()));
    }

    private ActorRef actor() {
        return kit.childActorOf(SegmentedJournalActor.props("foo", DIRECTORY, StorageLevel.DISK, MESSAGE_SIZE,
            SEGMENT_SIZE).withDispatcher(CallingThreadDispatcher.Id()));
    }

    private void deleteEntries(final long deleteTo) {
        final AsyncMessage<Void> delete = SegmentedJournalActor.deleteMessagesTo(deleteTo);
        actor.tell(delete, ActorRef.noSender());
        assertNull(get(delete));
    }

    private void assertHighestSequenceNr(final long expected) {
        AsyncMessage<Long> highest = SegmentedJournalActor.readHighestSequenceNr(0);
        actor.tell(highest, ActorRef.noSender());
        assertEquals(expected, (long) get(highest));
    }

    private void assertReplayCount(final int expected) {
        // Cast fixes an Eclipse warning 'generic array created'
        reset((Object) firstCallback);
        doNothing().when(firstCallback).accept(any(PersistentRepr.class));
        AsyncMessage<Void> replay = SegmentedJournalActor.replayMessages(0, Long.MAX_VALUE, Long.MAX_VALUE,
            firstCallback);
        actor.tell(replay, ActorRef.noSender());
        assertNull(get(replay));
        verify(firstCallback, times(expected)).accept(any(PersistentRepr.class));
    }

    private static void assertFileCount(final long dataFiles, final long deleteFiles) throws IOException {
        List<File> contents = Files.list(DIRECTORY.toPath()).map(Path::toFile).collect(Collectors.toList());
        assertEquals(dataFiles, contents.stream().filter(file -> file.getName().startsWith("data-")).count());
        assertEquals(deleteFiles, contents.stream().filter(file -> file.getName().startsWith("delete-")).count());
    }

    private static <T> T get(final AsyncMessage<T> message) {
        return getFuture(message.promise.future());
    }

    private static <T> T getFuture(final Future<T> future) {
        assertTrue(future.isCompleted());
        return future.value().get().get();
    }

    private static final class LargePayload implements Serializable {
        private static final long serialVersionUID = 1L;

        final byte[] bytes = new byte[MESSAGE_SIZE / 2];

    }
}
