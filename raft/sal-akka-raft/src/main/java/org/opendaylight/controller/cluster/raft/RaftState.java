/*
 * Copyright (c) 2026 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft;

import static java.util.Objects.requireNonNull;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.opendaylight.controller.cluster.common.actor.ExecuteInSelfActor;
import org.opendaylight.controller.cluster.raft.spi.EntryJournal;
import org.opendaylight.controller.cluster.raft.spi.RaftStorageCompleter;
import org.opendaylight.raft.spi.CompressionType;
import org.opendaylight.raft.spi.FileBackedOutputStream.Configuration;
import org.opendaylight.raft.spi.RestrictedObjectStreams;

/**
 * The RAFT state for local member as manager by {@link RaftActor}.
 */
@NonNullByDefault
abstract sealed class RaftState {
    static final class PersistentRaft extends StartedRaft {
        private PersistentRaft(final TransientRaft prev) {
            super(prev);
        }

        private PersistentRaft(final UnstartedRaft prev) {
            super(prev);
        }

        @Override
        PersistentRaft toPersistent() {
            return this;
        }

        @Override
        TransientRaft toTransient() {
            return new TransientRaft(this);
        }

        @Override
        UnstartedRaft toUnstarted() {
            persistenceControl.stop();
            return new UnstartedRaft(this);
        }
    }

    static final class TransientRaft extends StartedRaft {
        private TransientRaft(final PersistentRaft prev) {
            super(prev);
        }

        private TransientRaft(final UnstartedRaft prev) {
            super(prev);
        }

        @Override
        PersistentRaft toPersistent() {
            return new PersistentRaft(this);
        }

        @Override
        TransientRaft toTransient() {
            return this;
        }

        @Override
        UnstartedRaft toUnstarted() {
            persistenceControl.stop();
            return new UnstartedRaft(this);
        }
    }

    abstract static sealed class StartedRaft extends RaftState {
        StartedRaft(final StartedRaft prev) {
            super(prev);
        }

        StartedRaft(final UnstartedRaft prev) {
            super(prev);
        }

        /**
         * {@return the underlying EntryJournal, if available}
         */
        // FIXME: specialize by inlining ParsistenceControl
        final @Nullable EntryJournal journal() {
            return persistenceControl.journal();
        }
    }

    static final class UnstartedRaft extends RaftState {
        private final boolean persistent;

        private UnstartedRaft(final Path stateDir, final RestrictedObjectStreams objectStreams, final String memberId,
                final Map<String, String> peerAddresses, final ExecuteInSelfActor actor,
                final CompressionType preferredCompression, final Configuration streamConfig) {
            super(stateDir, objectStreams, memberId, peerAddresses, actor, preferredCompression, streamConfig);
            persistent = false;
        }

        private UnstartedRaft(final RaftState prev, final boolean persistent) {
            super(prev);
            this.persistent = persistent;
        }

        UnstartedRaft(final PersistentRaft prev) {
            this(prev, true);
        }

        UnstartedRaft(final TransientRaft prev) {
            this(prev, false);
        }

        @Override
        UnstartedRaft toPersistent() {
            return persistent ? this : new UnstartedRaft(this, true);
        }

        @Override
        UnstartedRaft toTransient() {
            return persistent ? new UnstartedRaft(this, false) : this;
        }

        @Override
        UnstartedRaft toUnstarted() {
            return this;
        }

        StartedRaft toStarted() throws IOException {
            persistenceControl.start();
            return persistent ? new PersistentRaft(this) : new TransientRaft(this);
        }
    }

    final RestrictedObjectStreams objectStreams;
    final PersistenceControl persistenceControl;
    final CompressionType preferredCompression;
    final RaftStorageCompleter completer;
    final Configuration streamConfig;
    // This context should NOT be passed directly to any other actor it is only to be consumed by RaftActorBehaviors.
    final LocalAccess localAccess;
    final PeerInfos peerInfos;
    final String memberId;

    RaftState(final Path stateDir, final RestrictedObjectStreams objectStreams, final String memberId,
            final Map<String, String> peerAddresses, final ExecuteInSelfActor actor,
            final CompressionType preferredCompression, final Configuration streamConfig) {
        this.objectStreams = requireNonNull(objectStreams);
        this.memberId = requireNonNull(memberId);
        this.preferredCompression = requireNonNull(preferredCompression);
        this.streamConfig = requireNonNull(streamConfig);
        localAccess = new LocalAccess(memberId, stateDir.resolve(memberId));
        completer = new RaftStorageCompleter(memberId, actor);
        peerInfos = new PeerInfos(memberId, peerAddresses);
        persistenceControl = new PersistenceControl(completer, localAccess.stateDir(), preferredCompression,
            streamConfig);
    }

    RaftState(final RaftState prev) {
        objectStreams = prev.objectStreams;
        persistenceControl = prev.persistenceControl;
        preferredCompression = prev.preferredCompression;
        completer = prev.completer;
        streamConfig = prev.streamConfig;
        localAccess = prev.localAccess;
        peerInfos = prev.peerInfos;
        memberId = prev.memberId;
    }

    static UnstartedRaft unstartedOf(final Path stateDir, final RestrictedObjectStreams objectStreams,
            final String memberId, final Map<String, String> peerAddresses, final ExecuteInSelfActor actor,
            final CompressionType preferredCompression, final Configuration streamConfig) {
        return new UnstartedRaft(stateDir, objectStreams, memberId, peerAddresses, actor, preferredCompression,
            streamConfig);
    }

    abstract RaftState toPersistent() throws IOException;

    abstract RaftState toTransient() throws IOException;

    abstract UnstartedRaft toUnstarted();
}
