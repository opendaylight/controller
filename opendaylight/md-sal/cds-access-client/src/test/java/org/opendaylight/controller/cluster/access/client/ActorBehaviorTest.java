/*
 * Copyright (c) 2017 Pantheon Technologies s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.access.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.persistence.Persistence;
import akka.persistence.SelectedSnapshot;
import akka.persistence.SnapshotMetadata;
import akka.testkit.TestProbe;
import akka.testkit.javadsl.TestKit;
import com.typesafe.config.ConfigFactory;
import java.io.File;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opendaylight.controller.cluster.access.concepts.ClientIdentifier;
import org.opendaylight.controller.cluster.access.concepts.FrontendIdentifier;
import org.opendaylight.controller.cluster.access.concepts.FrontendType;
import org.opendaylight.controller.cluster.access.concepts.MemberName;
import scala.concurrent.duration.FiniteDuration;

@ExtendWith(MockitoExtension.class)
class ActorBehaviorTest {
    private static FrontendIdentifier FRONTEND_ID = FrontendIdentifier.create(
        MemberName.forName("member-1"), FrontendType.forName("type-1"));
    private static FrontendIdentifier OTHER_FRONTEND_ID = FrontendIdentifier.create(
        MemberName.forName("member-2"), FrontendType.forName("type-2"));
    private static final String PERSISTENCE_ID = FRONTEND_ID.toPersistentId();
    private static final FiniteDuration TIMEOUT = FiniteDuration.create(5, TimeUnit.SECONDS);
    private static final long GEN_0 = 0;
    private static final long GEN_1 = 1;
    private static final long GEN_10 = 10;
    private static final long GEN_11 = 11;

    @Mock
    private InternalCommand<BackendInfo> cmd;
    @Mock(answer = Answers.CALLS_REAL_METHODS)
    private ClientActorBehavior<BackendInfo> initialBehavior;
    @Mock
    private AbstractClientActorContext ctx;

    private ActorSystem system;
    private TestProbe probe;
    private ActorRef actor;
    private File stateFile;

    @BeforeEach
    void beforeEach() throws Exception {
        //persistenceId() in AbstractClientActorBehavior is final and can't be mocked
        //use reflection to work around this
        final var context = AbstractClientActorBehavior.class.getDeclaredField("context");
        context.setAccessible(true);
        context.set(initialBehavior, ctx);
        final var persistenceId = AbstractClientActorContext.class.getDeclaredField("persistenceId");
        persistenceId.setAccessible(true);
        persistenceId.set(ctx, PERSISTENCE_ID);

        // delete state file if exists
        stateFile = ClientStateUtils.stateFile(FRONTEND_ID);
        stateFile.delete();

        // init actor system with persistence
        system = ActorSystem.apply("system1");
        final var storeRef = system.registerExtension(Persistence.lookup())
            .snapshotStoreFor(null, ConfigFactory.empty());
        probe = new TestProbe(system);
        storeRef.tell(probe.ref(), ActorRef.noSender());
        actor = system.actorOf(TestActor.props(FRONTEND_ID, initialBehavior));

        // recovery (migration) flow with no snapshot
        assertRecoveryFlow(null);
        assertCurrentGeneration(GEN_0);
    }

    @AfterEach
    void afterEach() {
        TestKit.shutdownActorSystem(system);
    }

    @Test
    void initialBehavior() {
        doReturn(initialBehavior).when(cmd).execute(any());
        actor.tell(cmd, ActorRef.noSender());
        verify(cmd, timeout(1000)).execute(initialBehavior);
    }

    @Test
    void commandStashing() {
        system.stop(actor);
        actor = system.actorOf(TestActor.props(FRONTEND_ID, initialBehavior));
        doReturn(initialBehavior).when(cmd).execute(any());
        // send messages before recovery is completed
        actor.tell(cmd, ActorRef.noSender());
        actor.tell(cmd, ActorRef.noSender());
        actor.tell(cmd, ActorRef.noSender());
        // complete recovery
        assertRecoveryFlow(null);
        // verify all pending messages are executed
        verify(cmd, timeout(1000).times(3)).execute(initialBehavior);
    }

    @Test
    void snapshotMigrate() {
        system.stop(actor);
        stateFile.delete();
        actor = system.actorOf(TestActor.props(FRONTEND_ID, initialBehavior));
        // takes snapshot generation 10, migrates and uses generation 11
        assertRecoveryFlow(snapshot(ClientIdentifier.create(FRONTEND_ID, GEN_10)));
        assertCurrentGeneration(GEN_11);
    }

    @Test
    void recoverFromTombstone() {
        system.stop(actor);
        stateFile.delete();
        actor = system.actorOf(TestActor.props(FRONTEND_ID, initialBehavior));
        probe.watch(actor);
        // takes snapshot with generation 10, adopts to generation 11, no snapshot re-save
        final var snapshot = snapshot(new PersistenceTombstone(ClientIdentifier.create(FRONTEND_ID, GEN_10)));
        assertRecoveryFlow(snapshot);
        // ensure the recovery flow is complete
        probe.expectNoMessage();
        assertCurrentGeneration(GEN_11);
    }

    @Test
    void frontendIdMismatch() {
        system.stop(actor);
        actor = system.actorOf(TestActor.props(FRONTEND_ID, initialBehavior));
        probe.watch(actor);
        // snapshot with frontend id mismatch
        final var snapshot = snapshot(ClientIdentifier.create(OTHER_FRONTEND_ID, GEN_10));
        assertRecoveryFlow(snapshot);
        //actor should be stopped
        probe.expectTerminated(actor, TIMEOUT);
        // snapshot data ignored because frontendId mismatches
        // initial generation = 0, after restart generation = 1
        assertCurrentGeneration(GEN_1);
    }

    @Test
    void tombstoneFrontendIdMismatch() {
        system.stop(actor);
        actor = system.actorOf(TestActor.props(FRONTEND_ID, initialBehavior));
        probe.watch(actor);
        // snapshot with frontend id mismatch
        final var snapshot = snapshot(new PersistenceTombstone(ClientIdentifier.create(OTHER_FRONTEND_ID, GEN_10)));
        // snapshot data ignored because frontendId mismatches
        assertRecoveryFlow(snapshot);
        //actor should be stopped
        probe.expectTerminated(actor, TIMEOUT);
        // initial generation = 0, after restart generation = 1
        assertCurrentGeneration(GEN_1);
    }

    private void assertRecoveryFlow(final SelectedSnapshot snapshot) {
        // actor explicitly requests for clientId snapshot
        probe.expectMsgClass(MockedSnapshotStore.LoadRequest.class);
        probe.reply(Optional.ofNullable(snapshot));

        if (snapshot == null) {
            // no snapshot
            return;
        }
        // snapshot exists
        if (snapshot.snapshot() instanceof ClientIdentifier clientId && FRONTEND_ID.equals(clientId.getFrontendId())) {
            // current clientId expected to be updated and saved as tombstone
            final var saveRequest = probe.expectMsgClass(MockedSnapshotStore.SaveRequest.class);
            assertNotNull(saveRequest);
            assertNotNull(saveRequest.getMetadata());
            assertEquals(PERSISTENCE_ID, saveRequest.getMetadata().persistenceId());

            // tombstone expected to use incremented (after migration) generation value
            final var tombstone = assertInstanceOf(PersistenceTombstone.class, saveRequest.getSnapshot());
            assertEquals(FRONTEND_ID, tombstone.clientId().getFrontendId());
            assertEquals(clientId.getGeneration() + 1, tombstone.clientId().getGeneration());
            probe.reply(Void.TYPE);

            // original snapshot expected to be deleted
            probe.expectMsgClass(MockedSnapshotStore.DeleteByCriteriaRequest.class);
            probe.reply(Void.TYPE);
        }
    }

    void assertCurrentGeneration(final long expectedGeneration) {
        final var clientId = ClientStateUtils.loadClientIdentifier(stateFile, FRONTEND_ID);
        assertNotNull(clientId);
        assertEquals(FRONTEND_ID, clientId.getFrontendId());
        assertEquals(expectedGeneration, clientId.getGeneration());
    }

    private static SelectedSnapshot snapshot(final Object snapshot) {
        return new SelectedSnapshot(SnapshotMetadata.apply(PERSISTENCE_ID, 1), snapshot);
    }

    private static class TestActor extends AbstractClientActor {
        private final ClientActorBehavior<?> initialBehavior;
        private final ClientActorConfig mockConfig = AccessClientUtil.newMockClientActorConfig();

        private static Props props(final FrontendIdentifier frontendId, final ClientActorBehavior<?> initialBehavior) {
            return Props.create(TestActor.class, () -> new TestActor(frontendId, initialBehavior));
        }

        TestActor(final FrontendIdentifier frontendId, final ClientActorBehavior<?> initialBehavior) {
            super(frontendId);
            this.initialBehavior = initialBehavior;
        }

        @Override
        protected ClientActorBehavior<?> initialBehavior(final ClientActorContext context) {
            return initialBehavior;
        }

        @Override
        protected ClientActorConfig getClientActorConfig() {
            return mockConfig;
        }
    }
}
