/*
 * Copyright (c) 2019 Pantheon Technologies, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.akka.segjournal;

import static com.google.common.base.Preconditions.checkArgument;
import static org.apache.pekko.actor.ActorRef.noSender;

import com.typesafe.config.Config;
import io.atomix.storage.journal.SegmentedJournal;
import io.atomix.storage.journal.StorageLevel;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import org.apache.pekko.actor.ActorRef;
import org.apache.pekko.dispatch.Futures;
import org.apache.pekko.persistence.AtomicWrite;
import org.apache.pekko.persistence.PersistentRepr;
import org.apache.pekko.persistence.journal.japi.AsyncWriteJournal;
import org.opendaylight.controller.akka.segjournal.SegmentedJournalActor.AsyncMessage;
import org.opendaylight.controller.akka.segjournal.SegmentedJournalActor.WriteMessages;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.concurrent.Future;

/**
 * An Pekko persistence journal implementation on top of {@link SegmentedJournal}. This actor represents aggregation
 * of multiple journals and performs a receptionist job between Pekko and invidual per-persistenceId actors. See
 * {@link SegmentedJournalActor} for details on how the persistence works.
 */
public class SegmentedFileJournal extends AsyncWriteJournal {
    public static final String STORAGE_ROOT_DIRECTORY = "root-directory";
    public static final String STORAGE_MAX_ENTRY_SIZE = "max-entry-size";
    public static final int STORAGE_MAX_ENTRY_SIZE_DEFAULT = 16 * 1024 * 1024;
    public static final String STORAGE_MAX_SEGMENT_SIZE = "max-segment-size";
    public static final int STORAGE_MAX_SEGMENT_SIZE_DEFAULT = STORAGE_MAX_ENTRY_SIZE_DEFAULT * 8;
    public static final String STORAGE_MAX_UNFLUSHED_BYTES = "max-unflushed-bytes";
    public static final String STORAGE_MEMORY_MAPPED = "memory-mapped";

    private static final Logger LOG = LoggerFactory.getLogger(SegmentedFileJournal.class);

    private final Map<String, ActorRef> handlers = new HashMap<>();
    private final Path rootDir;
    private final StorageLevel storage;
    private final int maxEntrySize;
    private final int maxSegmentSize;
    private final int maxUnflushedBytes;

    public SegmentedFileJournal(final Config config) {
        rootDir = Path.of(config.getString(STORAGE_ROOT_DIRECTORY));
        if (!Files.exists(rootDir)) {
            LOG.debug("Creating directory {}", rootDir);
            try {
                Files.createDirectories(rootDir);
            } catch (IOException e) {
                throw new IllegalStateException("Failed to create root directory " + rootDir, e);
            }
        }
        if (!Files.isDirectory(rootDir)) {
            throw new IllegalArgumentException(rootDir + " is not a directory");
        }

        maxEntrySize = getBytes(config, STORAGE_MAX_ENTRY_SIZE, STORAGE_MAX_ENTRY_SIZE_DEFAULT);
        maxSegmentSize = getBytes(config, STORAGE_MAX_SEGMENT_SIZE, STORAGE_MAX_SEGMENT_SIZE_DEFAULT);
        maxUnflushedBytes = getBytes(config, STORAGE_MAX_UNFLUSHED_BYTES, maxEntrySize);

        if (config.hasPath(STORAGE_MEMORY_MAPPED)) {
            storage = config.getBoolean(STORAGE_MEMORY_MAPPED) ? StorageLevel.MAPPED : StorageLevel.DISK;
        } else {
            storage = StorageLevel.DISK;
        }

        LOG.info("Initialized with root directory {} with storage {}", rootDir, storage);
    }

    @Override
    public Future<Iterable<Optional<Exception>>> doAsyncWriteMessages(final Iterable<AtomicWrite> messages) {
        final var map = new HashMap<ActorRef, WriteMessages>();
        final var result = new ArrayList<Future<Optional<Exception>>>();

        for (var message : messages) {
            final var persistenceId = message.persistenceId();
            final var handler = handlers.computeIfAbsent(persistenceId, this::createHandler);
            result.add(map.computeIfAbsent(handler, key -> new WriteMessages()).add(message));
        }

        // Send requests to actors and zip the futures back
        map.forEach((handler, message) -> {
            LOG.trace("Sending {} to {}", message, handler);
            handler.tell(message, noSender());
        });
        return Futures.sequence(result, context().dispatcher());
    }

    @Override
    public Future<Void> doAsyncDeleteMessagesTo(final String persistenceId, final long toSequenceNr) {
        return delegateMessage(persistenceId, SegmentedJournalActor.deleteMessagesTo(toSequenceNr));
    }

    @Override
    public Future<Void> doAsyncReplayMessages(final String persistenceId, final long fromSequenceNr,
            final long toSequenceNr, final long max, final Consumer<PersistentRepr> replayCallback) {
        return delegateMessage(persistenceId,
            SegmentedJournalActor.replayMessages(fromSequenceNr, toSequenceNr, max, replayCallback));
    }

    @Override
    public Future<Long> doAsyncReadHighestSequenceNr(final String persistenceId, final long fromSequenceNr) {
        return delegateMessage(handlers.computeIfAbsent(persistenceId, this::createHandler),
            SegmentedJournalActor.readHighestSequenceNr(fromSequenceNr));
    }

    private ActorRef createHandler(final String persistenceId) {
        final var directoryName = URLEncoder.encode(persistenceId, StandardCharsets.UTF_8);
        final var directory = rootDir.resolve(directoryName);
        LOG.debug("Creating handler for {} in directory {}", persistenceId, directory);

        final var handler = context().actorOf(SegmentedJournalActor.props(persistenceId, directory, storage,
            maxEntrySize, maxSegmentSize, maxUnflushedBytes));
        LOG.debug("Directory {} handled by {}", directory, handler);
        return handler;
    }

    private <T> Future<T> delegateMessage(final String persistenceId, final AsyncMessage<T> message) {
        final var handler = handlers.get(persistenceId);
        if (handler == null) {
            return Futures.failed(new IllegalStateException("Cannot find handler for " + persistenceId));
        }

        return delegateMessage(handler, message);
    }

    private static <T> Future<T> delegateMessage(final ActorRef handler, final AsyncMessage<T> message) {
        LOG.trace("Delegating {} to {}", message, handler);
        handler.tell(message, noSender());
        return message.promise.future();
    }

    private static int getBytes(final Config config, final String path, final int defaultValue) {
        if (!config.hasPath(path)) {
            return defaultValue;
        }
        final long value = config.getBytes(path);
        checkArgument(value <= Integer.MAX_VALUE, "Size %s exceeds maximum allowed %s", Integer.MAX_VALUE);
        return (int) value;
    }
}
