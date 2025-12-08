/*
 * Copyright (c) 2014, 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft.behaviors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.apache.pekko.actor.ActorRef;
import org.apache.pekko.protobufv3.internal.ByteString;
import org.eclipse.jdt.annotation.NonNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.opendaylight.controller.cluster.raft.AbstractActorTest;
import org.opendaylight.controller.cluster.raft.MessageCollector;
import org.opendaylight.controller.cluster.raft.MessageCollectorActor;
import org.opendaylight.controller.cluster.raft.MockCommand;
import org.opendaylight.controller.cluster.raft.MockRaftActorContext;
import org.opendaylight.controller.cluster.raft.MockRaftActorContext.Builder;
import org.opendaylight.controller.cluster.raft.MockRaftActorContext.SimpleReplicatedLog;
import org.opendaylight.controller.cluster.raft.RaftActorContext;
import org.opendaylight.controller.cluster.raft.ReplicatedLogEntry;
import org.opendaylight.controller.cluster.raft.TestActorFactory;
import org.opendaylight.controller.cluster.raft.messages.AppendEntries;
import org.opendaylight.controller.cluster.raft.messages.AppendEntriesReply;
import org.opendaylight.controller.cluster.raft.messages.Payload;
import org.opendaylight.controller.cluster.raft.messages.RaftRPC;
import org.opendaylight.controller.cluster.raft.messages.RequestVote;
import org.opendaylight.controller.cluster.raft.messages.RequestVoteReply;
import org.opendaylight.controller.cluster.raft.persisted.SimpleReplicatedLogEntry;
import org.opendaylight.controller.cluster.raft.spi.DefaultLogEntry;
import org.opendaylight.controller.cluster.raft.spi.LogEntry;
import org.opendaylight.raft.api.TermInfo;
import org.slf4j.LoggerFactory;

abstract class AbstractRaftActorBehaviorTest<T extends RaftActorBehavior> extends AbstractActorTest {
    final TestActorFactory actorFactory = new TestActorFactory(getSystem());

    private final MessageCollector behaviorActor =
        MessageCollector.of(getSystem(), actorFactory.generateActorId("behavior"));

    @TempDir
    Path stateDir;

    RaftActorBehavior behavior;

    @AfterEach
    void afterEach() {
        if (behavior != null) {
            behavior.close();
        }

        actorFactory.close();
    }

    /**
     * This test checks that when a new Raft RPC message is received with a newer
     * term the RaftActor gets into the Follower state.
     */
    @Test
    void testHandleRaftRPCWithNewerTerm() {
        MockRaftActorContext actorContext = createActorContext();

        assertStateChangesToFollowerWhenRaftRPCHasNewerTerm(actorContext, behaviorActor,
                createAppendEntriesWithNewerTerm());

        assertStateChangesToFollowerWhenRaftRPCHasNewerTerm(actorContext, behaviorActor,
                createAppendEntriesReplyWithNewerTerm());

        assertStateChangesToFollowerWhenRaftRPCHasNewerTerm(actorContext, behaviorActor,
                createRequestVoteWithNewerTerm());

        assertStateChangesToFollowerWhenRaftRPCHasNewerTerm(actorContext, behaviorActor,
                createRequestVoteReplyWithNewerTerm());
    }

    /**
     * This test verifies that when an AppendEntries is received with a term that
     * is less that the currentTerm of the RaftActor then the RaftActor does not
     * change it's state and it responds back with a failure.
     */
    @Test
    void testHandleAppendEntriesSenderTermLessThanReceiverTerm() {
        MockRaftActorContext context = createActorContext(5);

        // First set the receivers term to a high number (1000)
        context.setTermInfo(new TermInfo(1000, "test"));

        AppendEntries appendEntries = new AppendEntries(100, "leader-1", 0, 0, List.of(), 101, -1, (short) 4);

        behavior = createBehavior(context);

        assertSame(behavior, behavior.handleMessage(behaviorActor.actor(), appendEntries));

        // Also expect an AppendEntriesReply to be sent where success is false

        AppendEntriesReply reply = behaviorActor.expectFirstMatching(AppendEntriesReply.class);

        assertFalse(reply.isSuccess());
        assertEquals(5, reply.getPayloadVersion());
    }

    @Test
    void testHandleAppendEntriesAddSameEntryToLog() {
        MockRaftActorContext context = createActorContext();

        context.setTermInfo(new TermInfo(2, "test"));

        // Prepare the receivers log
        MockCommand payload = new MockCommand("zero");
        setLastLogEntry(context, 2, 0, payload);

        List<ReplicatedLogEntry> entries = new ArrayList<>();
        entries.add(new SimpleReplicatedLogEntry(0, 2, payload));

        final AppendEntries appendEntries = new AppendEntries(2, "leader-1", -1, -1, entries, 2, -1, (short)0);

        behavior = createBehavior(context);

        assertFalse(behavior instanceof Candidate);

        // Check that the behavior does not handle unknown message
        assertNull(behavior.handleMessage(behaviorActor, "unknown"));

        assertSame(behavior, behavior.handleMessage(behaviorActor, appendEntries));

        assertEquals(1, context.getReplicatedLog().size());

        handleAppendEntriesAddSameEntryToLogReply(behaviorActor);
    }

    protected void handleAppendEntriesAddSameEntryToLogReply(final ActorRef replyActor) {
        assertNull(MessageCollectorActor.getFirstMatching(replyActor, AppendEntriesReply.class));
    }

    /**
     * This test verifies that when a RequestVote is received by the RaftActor
     * with the senders' log is more up to date than the receiver that the receiver grants
     * the vote to the sender.
     */
    @Test
    void testHandleRequestVoteWhenSenderLogMoreUpToDate() {
        MockRaftActorContext context = createActorContext();

        behavior = createBehavior(context);

        context.setTermInfo(new TermInfo(1, "test"));

        behavior.handleMessage(behaviorActor, new RequestVote(context.currentTerm(), "test", 10000, 999));

        RequestVoteReply reply = MessageCollectorActor.expectFirstMatching(behaviorActor, RequestVoteReply.class);
        assertTrue(reply.isVoteGranted());
    }

    /**
     * This test verifies that when a RaftActor receives a RequestVote message
     * with a term that is greater than it's currentTerm but a less up-to-date
     * log then the receiving RaftActor will not grant the vote to the sender.
     */
    @Test
    void testHandleRequestVoteWhenSenderLogLessUptoDate() {
        MockRaftActorContext context = createActorContext();

        behavior = createBehavior(context);

        context.setTermInfo(new TermInfo(1, "test"));

        int index = 2000;
        setLastLogEntry(context, context.currentTerm(), index, new MockCommand(""));

        behavior.handleMessage(behaviorActor,
            new RequestVote(context.currentTerm(), "test", index - 1, context.currentTerm()));

        RequestVoteReply reply = MessageCollectorActor.expectFirstMatching(behaviorActor, RequestVoteReply.class);
        assertFalse(reply.isVoteGranted());
    }

    /**
     * This test verifies that the receiving RaftActor will not grant a vote
     * to a sender if the sender's term is lesser than the currentTerm of the
     * recipient RaftActor.
     */
    @Test
    void testHandleRequestVoteWhenSenderTermLessThanCurrentTerm() {
        MockRaftActorContext context = createActorContext();

        context.setTermInfo(new TermInfo(1000, null));

        behavior = createBehavior(context);

        behavior.handleMessage(behaviorActor, new RequestVote(999, "test", 10000, 999));

        RequestVoteReply reply = MessageCollectorActor.expectFirstMatching(behaviorActor, RequestVoteReply.class);
        assertFalse(reply.isVoteGranted());
    }

    @Test
    void testPerformSnapshot() {
        final var context = new MockRaftActorContext("test", stateDir, getSystem(), behaviorActor);
        RaftActorBehavior abstractBehavior = createBehavior(context);
        if (abstractBehavior instanceof Candidate) {
            return;
        }

        context.setTermInfo(new TermInfo(1, "test"));

        //log has 1 entry with replicatedToAllIndex = 0, does not do anything, returns the
        var log = new Builder().createEntries(0, 1, 1).build();
        log.setLastApplied(0);
        context.resetReplicatedLog(log);
        abstractBehavior.performSnapshotWithoutCapture(0);
        assertEquals(-1, abstractBehavior.getReplicatedToAllIndex());
        assertEquals(1, log.size());

        //2 entries, lastApplied still 0, no purging.
        log = new Builder().createEntries(0, 2, 1).build();
        log.setLastApplied(0);
        context.resetReplicatedLog(log);
        abstractBehavior.performSnapshotWithoutCapture(0);
        assertEquals(-1, abstractBehavior.getReplicatedToAllIndex());
        assertEquals(2, context.getReplicatedLog().size());

        //2 entries, lastApplied still 0, no purging.
        log = new Builder().createEntries(0, 2, 1).build();
        log.setLastApplied(1);
        context.resetReplicatedLog(log);
        abstractBehavior.performSnapshotWithoutCapture(0);
        assertEquals(0, abstractBehavior.getReplicatedToAllIndex());
        assertEquals(1, context.getReplicatedLog().size());

        // 5 entries, lastApplied =2 and replicatedIndex = 3, but since we want to keep the lastapplied, indices 0 and
        // 1 will only get purged
        log = new Builder().createEntries(0, 5, 1).build();
        log.setLastApplied(2);
        context.resetReplicatedLog(log);
        abstractBehavior.performSnapshotWithoutCapture(3);
        assertEquals(1, abstractBehavior.getReplicatedToAllIndex());
        assertEquals(3, context.getReplicatedLog().size());

        // scenario where Last applied > Replicated to all index (becoz of a slow follower)
        log = new Builder().createEntries(0, 3, 1).build();
        log.setLastApplied(2);
        context.resetReplicatedLog(log);
        abstractBehavior.performSnapshotWithoutCapture(1);
        assertEquals(1, abstractBehavior.getReplicatedToAllIndex());
        assertEquals(1, context.getReplicatedLog().size());
    }

    protected void assertStateChangesToFollowerWhenRaftRPCHasNewerTerm(final MockRaftActorContext actorContext,
            final MessageCollector collector, final RaftRPC rpc) {

        Payload payload = new MockCommand("");
        setLastLogEntry(actorContext, 1, 0, payload);
        actorContext.setTermInfo(new TermInfo(1, "test"));

        final var origBehavior = createBehavior(actorContext);
        final var raftBehavior = assertInstanceOf(Follower.class, origBehavior.handleMessage(collector.actor(), rpc));

        assertEquals(rpc.getTerm(), actorContext.currentTerm());

        origBehavior.close();
        raftBehavior.close();
    }

    protected static final @NonNull SimpleReplicatedLog setLastLogEntry(final MockRaftActorContext actorContext,
            final long term, final long index, final Payload data) {
        return setLastLogEntry(actorContext, new DefaultLogEntry(index, term, data));
    }

    protected static final @NonNull SimpleReplicatedLog setLastLogEntry(final MockRaftActorContext actorContext,
            final LogEntry logEntry) {
        final var log = new SimpleReplicatedLog();
        log.append(logEntry);
        actorContext.resetReplicatedLog(log);
        return log;
    }

    protected abstract T createBehavior(RaftActorContext actorContext);

    protected final T createBehavior(final MockRaftActorContext actorContext) {
        final var ret = createBehavior((RaftActorContext) actorContext);
        actorContext.setCurrentBehavior(ret);
        return ret;
    }

    protected RaftActorBehavior createBehavior() {
        return createBehavior(createActorContext());
    }

    protected @NonNull MockRaftActorContext createActorContext() {
        return createActorContext(0);
    }

    protected @NonNull MockRaftActorContext createActorContext(final int payloadVersion) {
        return new MockRaftActorContext(stateDir, payloadVersion);
    }

    protected final @NonNull MockRaftActorContext createActorContext(final ActorRef actor) {
        return new MockRaftActorContext("test", stateDir, getSystem(), actor, 0);
    }

    protected @NonNull MockRaftActorContext createActorContext(final ActorRef actor, final int payloadVersion) {
        return new MockRaftActorContext("test", stateDir, getSystem(), actor, payloadVersion);
    }

    protected AppendEntries createAppendEntriesWithNewerTerm() {
        return new AppendEntries(100, "leader-1", 0, 0, List.of(), 1, -1, (short)0);
    }

    protected AppendEntriesReply createAppendEntriesReplyWithNewerTerm() {
        return new AppendEntriesReply("follower-1", 100, false, 100, 100, (short)0);
    }

    protected RequestVote createRequestVoteWithNewerTerm() {
        return new RequestVote(100, "candidate-1", 10, 100);
    }

    protected RequestVoteReply createRequestVoteReplyWithNewerTerm() {
        return new RequestVoteReply(100, false);
    }

    protected ByteString toByteString(final Map<String, String> state) {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try (ObjectOutputStream oos = new ObjectOutputStream(bos)) {
            oos.writeObject(state);
            return ByteString.copyFrom(bos.toByteArray());
        } catch (IOException e) {
            throw new AssertionError("IOException occurred converting Map to Bytestring", e);
        }
    }

    protected void logStart(final String name) {
        LoggerFactory.getLogger(getClass()).info("Starting " + name);
    }

    protected static final void assertLogEntry(final @NonNull LogEntry expected, final LogEntry actual) {
        assertNotNull(actual);
        assertEquals(expected.index(), actual.index());
        assertEquals(expected.term(), actual.term());
        assertEquals(expected.command(), actual.command());
    }
}
