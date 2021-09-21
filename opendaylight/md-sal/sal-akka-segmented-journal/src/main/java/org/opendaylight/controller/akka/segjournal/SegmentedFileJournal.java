/*
 * Copyright (c) 2019 Pantheon Technologies, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.akka.segjournal;

import static akka.actor.ActorRef.noSender;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;

import akka.actor.ActorRef;
import akka.dispatch.Futures;
import akka.persistence.AtomicWrite;
import akka.persistence.PersistentRepr;
import akka.persistence.journal.japi.AsyncWriteJournal;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigMemorySize;
import io.atomix.storage.StorageLevel;
import io.atomix.storage.journal.SegmentedJournal;
import java.io.File;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import org.opendaylight.controller.akka.segjournal.SegmentedJournalActor.AsyncMessage;
import org.opendaylight.controller.akka.segjournal.SegmentedJournalActor.WriteMessages;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.concurrent.Future;

/**
 * An Akka persistence journal implementation on top of {@link SegmentedJournal}. This actor represents aggregation
 * of multiple journals and performs a receptionist job between Akka and invidual per-persistenceId actors. See
 * {@link SegmentedJournalActor} for details on how the persistence works.
 *
 * @author Robert Varga
 */
public class SegmentedFileJournal extends AsyncWriteJournal {
    public static final String STORAGE_ROOT_DIRECTORY = "root-directory";
    public static final String STORAGE_MAX_ENTRY_SIZE = "max-entry-size";
    public static final int STORAGE_MAX_ENTRY_SIZE_DEFAULT = 16 * 1024 * 1024;
    public static final String STORAGE_MAX_SEGMENT_SIZE = "max-segment-size";
    public static final int STORAGE_MAX_SEGMENT_SIZE_DEFAULT = STORAGE_MAX_ENTRY_SIZE_DEFAULT * 8;
    public static final String STORAGE_MEMORY_MAPPED = "memory-mapped";

    private static final Logger LOG = LoggerFactory.getLogger(SegmentedFileJournal.class);

    private final Map<String, ActorRef> handlers = new HashMap<>();
    private final File rootDir;
    private final StorageLevel storage;
    private final int maxEntrySize;
    private final int maxSegmentSize;

    public SegmentedFileJournal(final Config config) {
        rootDir = new File(config.getString(STORAGE_ROOT_DIRECTORY));
        if (!rootDir.exists()) {
            LOG.debug("Creating directory {}", rootDir);
            checkState(rootDir.mkdirs(), "Failed to create root directory %s", rootDir);
        }
        checkArgument(rootDir.isDirectory(), "%s is not a directory", rootDir);

        maxEntrySize = getBytes(config, STORAGE_MAX_ENTRY_SIZE, STORAGE_MAX_ENTRY_SIZE_DEFAULT);
        maxSegmentSize = getBytes(config, STORAGE_MAX_SEGMENT_SIZE, STORAGE_MAX_SEGMENT_SIZE_DEFAULT);

        if (config.hasPath(STORAGE_MEMORY_MAPPED)) {
            storage = config.getBoolean(STORAGE_MEMORY_MAPPED) ? StorageLevel.MAPPED : StorageLevel.DISK;
        } else {
            storage = StorageLevel.DISK;
        }

        LOG.info("Initialized with root directory {} with storage {}", rootDir, storage);
    }

    @Override
    public Future<Iterable<Optional<Exception>>> doAsyncWriteMessages(final Iterable<AtomicWrite> messages) {
        final Map<ActorRef, WriteMessages> map = new HashMap<>();
        final List<Future<Optional<Exception>>> result = new ArrayList<>();

        for (AtomicWrite message : messages) {
            final String persistenceId = message.persistenceId();
            final ActorRef handler = handlers.computeIfAbsent(persistenceId, this::createHandler);
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
        final String directoryName = URLEncoder.encode(persistenceId, StandardCharsets.UTF_8);
        final File directory = new File(rootDir, directoryName);
        LOG.debug("Creating handler for {} in directory {}", persistenceId, directory);

        final ActorRef handler = context().actorOf(SegmentedJournalActor.props(persistenceId, directory, storage,
            maxEntrySize, maxSegmentSize));
        LOG.debug("Directory {} handled by {}", directory, handler);
        return handler;
    }

    private <T> Future<T> delegateMessage(final String persistenceId, final AsyncMessage<T> message) {
        final ActorRef handler = handlers.get(persistenceId);
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
        final ConfigMemorySize value = config.getMemorySize(path);
        final long result = value.toBytes();
        checkArgument(result <= Integer.MAX_VALUE, "Size %s exceeds maximum allowed %s", Integer.MAX_VALUE);
        return (int) result;
    }
}
