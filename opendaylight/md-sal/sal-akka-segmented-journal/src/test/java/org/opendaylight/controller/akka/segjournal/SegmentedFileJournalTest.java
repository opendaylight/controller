/*
 * Copyright (c) 2019 Pantheon Technologies, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.akka.segjournal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
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
import com.codahale.metrics.MetricRegistry;
import io.atomix.storage.journal.JournalSerdes;
import io.atomix.storage.journal.SegmentedJournal;
import io.atomix.storage.journal.StorageLevel;
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
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opendaylight.controller.akka.segjournal.DataJournalEntry.FromPersistence;
import org.opendaylight.controller.akka.segjournal.DataJournalEntry.ToPersistence;
import org.opendaylight.controller.akka.segjournal.SegmentedJournalActor.AsyncMessage;
import org.opendaylight.controller.akka.segjournal.SegmentedJournalActor.WriteMessages;
import org.opendaylight.controller.cluster.common.actor.MeteringBehavior;
import org.opendaylight.controller.cluster.reporting.MetricsReporter;
import scala.concurrent.Future;

@ExtendWith(MockitoExtension.class)
class SegmentedFileJournalTest {
    private static final File DIRECTORY = new File("target/sfj-test");
    private static final int SEGMENT_SIZE = 1024 * 1024;
    private static final int MESSAGE_SIZE = 512 * 1024;
    private static final int FLUSH_SIZE = 16 * 1024;
    // Number of messages to create 2 segments when using Large payload
    private static final int MESSAGE_COUNT = SEGMENT_SIZE * 3 / MESSAGE_SIZE;

    private static ActorSystem SYSTEM;

    @Mock
    private Consumer<PersistentRepr> firstCallback;

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
        actor = actor();
    }

    @AfterEach
    void after() {
        actor.tell(PoisonPill.getInstance(), ActorRef.noSender());
        FileUtils.deleteQuietly(DIRECTORY);
    }

    @Test
    void testDeleteAfterStop() {
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
    void testSegmentation() throws IOException {
        // We want to have roughly three segments
        final LargePayload payload = new LargePayload();

        final WriteMessages write = new WriteMessages();
        final List<Future<Optional<Exception>>> requests = new ArrayList<>();

        // Each payload is half of segment size, plus some overhead, should result in two segments being present
        for (int i = 1; i <= MESSAGE_COUNT; ++i) {
            requests.add(write.add(AtomicWrite.apply(PersistentRepr.apply(payload, i, "foo", null, false, kit.getRef(),
                "uuid"))));
        }

        actor.tell(write, ActorRef.noSender());
        requests.forEach(future -> assertFalse(getFuture(future).isPresent()));

        assertFileCount(2, 1, 1);

        // Delete all but the last entry
        deleteEntries(requests.size());

        assertFileCount(1, 1, 1);
    }

    @Test
    void testComplexDeletesAndPartialReplays() throws Exception {
        for (int i = 0; i <= 4; i++) {
            writeBigPaylod(false, new char[]{'u', 'u', 'i', 'd'}, 0);
        }

        assertFileCount(10, 1, 1);

        // delete including index 3, so get rid of the first segment
        deleteEntries(3);
        assertFileCount(9, 1, 1);

        // get rid of segments 2(index 4-6) and 3(index 7-9)
        deleteEntries(9);
        assertFileCount(7, 1, 1);

        // get rid of all segments except the last one
        deleteEntries(27);
        assertFileCount(1, 1, 1);

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

    @Test
    public void testUuidSegmentationAndDeletes() throws Exception {
        //  Large uuid to test segmentation with same size as LargePayload
        final char[] chars = new char[MESSAGE_SIZE / 2];
        for (int i = 0; i < MESSAGE_SIZE / 2 ; i++) {
            chars[i] = 'a';
        }

        // Create 2 segments of data and unique uuids
        writeBigPaylod(true, chars, 0);
        assertFileCount(2, 2, 1);
        // Create 2 segments of data but use same uuid as last message so no new uuid should be written
        writeBigPaylod(false, chars, 0);
        assertFileCount(4, 2, 1);
        // Create 2 more segments of data and unique uuids
        writeBigPaylod(true, chars, MESSAGE_COUNT);

        // 6 segments of data and 4 of uuides present
        assertFileCount(6, 4, 1);

        // delete including index 6, so get rid of the first 2 segments of data
        // there is 3 segments of uuids present 1 uuid used fo multiple messages + 2 segments of unique uuides
        deleteEntries(6);
        assertFileCount(4, 3, 1);

        // check if uuides are loaded correctly on actor restart
        assertReplayCount(12);
        restartActor();
        assertReplayCount(12);

        // delete 2 segments which use the same uuid, now only 2 segments of unique uuides are left
        deleteEntries(12);
        assertFileCount(2, 2, 1);

        // get rid of all segments except the last one
        deleteEntries(17);
        assertFileCount(1, 1, 1);

        assertReplayCount(1);
    }

    @Test
    public void testUpgrade() throws Exception {
        //  Large uuid to test segmentation with same size as LargePayload
        final char[] chars = new char[MESSAGE_SIZE / 4];
        for (int i = 0; i < MESSAGE_SIZE / 4 ; i++) {
            chars[i] = 'a';
        }
        // write data in to the old journal
        final LargePayload payload = new LargePayload();
        final WriteMessages write = new WriteMessages();

        // Each payload is half of segment size, plus some overhead, should result in two segments being present
        for (int i = 1; i <= MESSAGE_COUNT * 2; ++i) {
            chars[i] += 1;
            write.add(AtomicWrite.apply(PersistentRepr.apply(payload, i, "foo", null, false, kit.getRef(),
                    new String(chars))));
        }
        for (int i = 1; i <= MESSAGE_COUNT * 2; ++i) {
            write.add(AtomicWrite.apply(PersistentRepr.apply(payload, i, "foo", null, false, kit.getRef(),
                    new String(chars))));
        }

        // create V0 journal to create data for upgrade test
        final var entries = SegmentedJournal.<DataJournalEntry>builder()
                .withStorageLevel(StorageLevel.DISK).withDirectory(DIRECTORY).withName("data")
                .withNamespace(JournalSerdes.builder()
                        .register(new DataJournalEntrySerdes(SYSTEM), FromPersistence.class, ToPersistence.class)
                        .build())
                .withMaxEntrySize(MESSAGE_SIZE).withMaxSegmentSize(SEGMENT_SIZE)
                .build();
        final var registry = MetricsReporter.getInstance(MeteringBehavior.DOMAIN).getMetricsRegistry();
        var messageSize = registry.histogram(MetricRegistry.name("actorName", "messageSize"));
        var oldJournal =  new DataJournalV0("foo", messageSize, entries);

        // 6 segments written 4 data 2 uuids
        // no uuids and no delete files because old data journal was used directly without actor
        oldJournal.handleWriteMessages(write);
        assertFileCount(12, 0, 0);

        restartActor();
        assertReplayCount(24);
        assertFileCount(12, 2, 1);

        //test delete
        deleteEntries(12);
        assertReplayCount(12);
        assertFileCount(6, 1, 1);

        deleteEntries(23);
        assertReplayCount(1);
        assertFileCount(1, 1, 1);
    }

    private void restartActor() {
        actor.tell(PoisonPill.getInstance(), ActorRef.noSender());
        actor = actor();
    }

    private void writeBigPaylod(final boolean unique, final char[] uuid, final int offset) {
        final LargePayload payload = new LargePayload();

        final WriteMessages write = new WriteMessages();
        final List<Future<Optional<Exception>>> requests = new ArrayList<>();

        // Each payload is half of segment size, plus some overhead, should result in two segments being present
        for (int i = 1; i <= MESSAGE_COUNT; ++i) {
            if (unique) {
                uuid[i + offset] += 1;
            }
            requests.add(write.add(AtomicWrite.apply(PersistentRepr.apply(payload, i, "foo", null, false, kit.getRef(),
                    new String(uuid)))));
        }

        actor.tell(write, ActorRef.noSender());
        requests.forEach(future -> assertFalse(getFuture(future).isPresent()));
    }

    private ActorRef actor() {
        return kit.childActorOf(SegmentedJournalActor.props("foo", DIRECTORY, StorageLevel.DISK, MESSAGE_SIZE,
            SEGMENT_SIZE, FLUSH_SIZE, Integer.MAX_VALUE).withDispatcher(CallingThreadDispatcher.Id()));
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
        AsyncMessage<Void> replay = SegmentedJournalActor.replayMessages(0, Long.MAX_VALUE, Long.MAX_VALUE,
            firstCallback);
        actor.tell(replay, ActorRef.noSender());
        assertNull(get(replay));
        verify(firstCallback, times(expected)).accept(any(PersistentRepr.class));
    }

    private static void assertFileCount(final long dataFiles, final long uuideFiles,
            final long deleteFiles) throws IOException {
        List<File> contents = Files.list(DIRECTORY.toPath()).map(Path::toFile).collect(Collectors.toList());
        assertEquals(dataFiles, contents.stream().filter(file -> file.getName().startsWith("data-")).count());
        assertEquals(uuideFiles, contents.stream().filter(file -> file.getName().startsWith("uuids-")).count());
        assertEquals(deleteFiles, contents.stream().filter(file -> file.getName().startsWith("delete-")).count());
    }

    private static <T> T get(final AsyncMessage<T> message) {
        return getFuture(message.promise.future());
    }

    private static <T> T getFuture(final Future<T> future) {
        assertTrue(future.isCompleted());
        return future.value().get().get();
    }

    static final class LargePayload implements Serializable {
        @java.io.Serial
        private static final long serialVersionUID = 1L;

        final byte[] bytes = new byte[MESSAGE_SIZE / 2];
    }
}
