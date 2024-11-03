/*
 * Copyright (c) 2017 Pantheon Technologies s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.access.client;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

import com.typesafe.config.ConfigFactory;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import org.apache.pekko.actor.ActorRef;
import org.apache.pekko.actor.ActorSystem;
import org.apache.pekko.actor.Props;
import org.apache.pekko.persistence.Persistence;
import org.apache.pekko.persistence.SelectedSnapshot;
import org.apache.pekko.persistence.SnapshotMetadata;
import org.apache.pekko.testkit.TestProbe;
import org.apache.pekko.testkit.javadsl.TestKit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opendaylight.controller.cluster.access.client.MockedSnapshotStore.DeleteByCriteriaRequest;
import org.opendaylight.controller.cluster.access.client.MockedSnapshotStore.DeleteByMetadataRequest;
import org.opendaylight.controller.cluster.access.client.MockedSnapshotStore.LoadRequest;
import org.opendaylight.controller.cluster.access.client.MockedSnapshotStore.SaveRequest;
import org.opendaylight.controller.cluster.access.concepts.ClientIdentifier;
import org.opendaylight.controller.cluster.access.concepts.FrontendIdentifier;
import org.opendaylight.controller.cluster.access.concepts.FrontendType;
import org.opendaylight.controller.cluster.access.concepts.MemberName;
import scala.concurrent.duration.FiniteDuration;

@ExtendWith(MockitoExtension.class)
class ActorBehaviorTest {
    private static final String MEMBER_1_FRONTEND_TYPE_1 = "member-1-frontend-type-1";
    private static final FiniteDuration TIMEOUT = FiniteDuration.create(5, TimeUnit.SECONDS);

    private final FrontendIdentifier id =
        FrontendIdentifier.create(MemberName.forName("member-1"), FrontendType.forName("type-1"));

    @TempDir
    private Path statePath;
    @Mock
    private InternalCommand<BackendInfo> cmd;
    @Mock(answer = Answers.CALLS_REAL_METHODS)
    private ClientActorBehavior<BackendInfo> initialBehavior;
    @Mock
    private AbstractClientActorContext ctx;

    private ActorSystem system;
    private TestProbe probe;
    private ActorRef mockedActor;

    @BeforeEach
    void beforeEach() throws Exception {
        //persistenceId() in AbstractClientActorBehavior is final and can't be mocked
        //use reflection to work around this
        final var context = AbstractClientActorBehavior.class.getDeclaredField("context");
        context.setAccessible(true);
        context.set(initialBehavior, ctx);
        final var persistenceId = AbstractClientActorContext.class.getDeclaredField("persistenceId");
        persistenceId.setAccessible(true);
        persistenceId.set(ctx, MEMBER_1_FRONTEND_TYPE_1);

        system = ActorSystem.apply("system1");
        final var storeRef = ((Persistence) system.registerExtension(Persistence.lookup()))
            .snapshotStoreFor(null, ConfigFactory.empty());
        probe = new TestProbe(system);
        storeRef.tell(probe.ref(), ActorRef.noSender());

        mockedActor = system.actorOf(MockedActor.props(statePath, id, initialBehavior));
    }

    @AfterEach
    void afterEach() throws Exception {
        TestKit.shutdownActorSystem(system);
    }

    @Test
    void testInitialBehavior() {
        recoverInitial();

        doReturn(initialBehavior).when(cmd).execute(any());
        mockedActor.tell(cmd, ActorRef.noSender());
        verify(cmd, timeout(1000)).execute(initialBehavior);
    }

    @Test
    void testCommandStashing() {
        recoverInitial();

        system.stop(mockedActor);
        mockedActor = system.actorOf(MockedActor.props(statePath, id, initialBehavior));
        doReturn(initialBehavior).when(cmd).execute(any());
        //send messages before recovery is completed
        mockedActor.tell(cmd, ActorRef.noSender());
        mockedActor.tell(cmd, ActorRef.noSender());
        mockedActor.tell(cmd, ActorRef.noSender());
        //complete recovery
        assertTombstone(1, handleRecovery(null));
        verifyStateFile(1);

        verify(cmd, timeout(1000).times(3)).execute(initialBehavior);
    }

    @Test
    void testRecoveryAfterRestart() {
        final var saveRequest = recoverInitial();

        system.stop(mockedActor);
        mockedActor = system.actorOf(MockedActor.props(statePath, id, initialBehavior));

        probe.expectMsgClass(LoadRequest.class);
        //offer snapshot
        probe.reply(Optional.ofNullable(new SelectedSnapshot(saveRequest.getMetadata(), saveRequest.getSnapshot())));

        // state should be updated
        await().atMost(Duration.ofSeconds(5)).untilAsserted(() -> verifyStateFile(1));

        // there should be no further save
        probe.expectNoMessage();
    }

    @Test
    void testRecoveryAfterRestartFrontendIdMismatch() {
        final var saveRequest = recoverInitial();

        system.stop(mockedActor);
        //start actor again
        mockedActor = system.actorOf(MockedActor.props(statePath, id, initialBehavior));
        probe.expectMsgClass(LoadRequest.class);
        //offer snapshot with incorrect client id
        final var metadata = saveRequest.getMetadata();
        final var anotherFrontend = FrontendIdentifier.create(MemberName.forName("another"),
                FrontendType.forName("type-2"));
        final var incorrectClientId = ClientIdentifier.create(anotherFrontend, 0);
        probe.watch(mockedActor);
        probe.reply(Optional.of(new SelectedSnapshot(metadata, incorrectClientId)));
        //actor should be stopped
        probe.expectTerminated(mockedActor, TIMEOUT);
        verifyStateFile(0);
    }

    @Test
    void testRecoveryAfterRestartSaveSnapshotFail() {
        recoverInitial();

        system.stop(mockedActor);
        mockedActor = system.actorOf(MockedActor.props(statePath, id, initialBehavior));
        probe.watch(mockedActor);
        probe.expectMsgClass(LoadRequest.class);
        probe.reply(Optional.empty());

        assertTombstone(1);

        probe.reply(new RuntimeException("save failed"));
        probe.expectMsgClass(DeleteByMetadataRequest.class);
        probe.expectTerminated(mockedActor, TIMEOUT);
        verifyStateFile(1);
    }

    @Test
    void testRecoveryAfterRestartDeleteSnapshotsFail() {
        recoverInitial();

        system.stop(mockedActor);
        mockedActor = system.actorOf(MockedActor.props(statePath, id, initialBehavior));
        probe.watch(mockedActor);
        probe.expectMsgClass(LoadRequest.class);
        probe.reply(Optional.empty());

        assertTombstone(1);

        probe.reply(Void.TYPE);
        probe.expectMsgClass(DeleteByCriteriaRequest.class);
        probe.reply(new RuntimeException("delete failed"));
        //actor shouldn't terminate
        probe.expectNoMessage();
        verifyStateFile(1);
    }

    @Test
    void testMigration() {
        final var saveRequest = handleRecovery(
            new SelectedSnapshot(new SnapshotMetadata(MEMBER_1_FRONTEND_TYPE_1, 0, 0), ClientIdentifier.create(id, 5)));
        assertTombstone(6, saveRequest);
        verifyStateFile(6);
    }

    private SaveRequest recoverInitial() {
        //handle initial actor recovery
        final var saveRequest = handleRecovery(null);
        assertTombstone(0, saveRequest);
        verifyStateFile(0);
        return saveRequest;
    }

    private SaveRequest handleRecovery(final SelectedSnapshot savedState) {
        probe.expectMsgClass(LoadRequest.class);
        //offer snapshot
        probe.reply(Optional.ofNullable(savedState));
        final var nextSaveRequest = probe.expectMsgClass(SaveRequest.class);
        probe.reply(Void.TYPE);
        //check old snapshots deleted
        probe.expectMsgClass(DeleteByCriteriaRequest.class);
        probe.reply(Void.TYPE);
        return nextSaveRequest;
    }

    private void assertTombstone(final long expectedGeneration) {
        assertTombstone(expectedGeneration, probe.expectMsgClass(SaveRequest.class));
    }

    private void assertTombstone(final long expectedGeneration, final SaveRequest save) {
        final var clientId = assertInstanceOf(PersistenceTombstone.class, save.getSnapshot()).clientId();
        assertEquals(id, clientId.getFrontendId());
        assertEquals(expectedGeneration, clientId.getGeneration());
    }

    private void verifyStateFile(final long expectedGeneration) {
        final List<String> lines;
        try {
            lines = Files.readAllLines(statePath.resolve("odl.cluster.client").resolve("member-1")
                .resolve("type-1.properties"));
        } catch (IOException e) {
            throw new AssertionError(e);
        }

        assertEquals(5, lines.size());
        assertEquals("#Critical persistent state. Do not touch unless you know what you are doing!", lines.get(0));
        assertEquals("client-type=type-1", lines.get(2));
        assertEquals("generation=" + expectedGeneration, lines.get(3));
        assertEquals("member-name=member-1", lines.get(4));
    }

    private static class MockedActor extends AbstractClientActor {
        private final ClientActorBehavior<?> initialBehavior;
        private final ClientActorConfig mockConfig = AccessClientUtil.newMockClientActorConfig();

        // FIXME: Passing initial behavior here is a mocking pain, as evidenced via above reflection dance. Refactor
        //        tests so we can allocate it in initialBehavior() below, where we get a fully-functioning
        //        ClientActorContext
        static Props props(final Path statePath, final FrontendIdentifier frontendId,
                final ClientActorBehavior<?> initialBehavior) {
            return Props.create(MockedActor.class, () -> new MockedActor(statePath, frontendId, initialBehavior));
        }

        MockedActor(final Path statePath, final FrontendIdentifier frontendId,
                final ClientActorBehavior<?> initialBehavior) {
            super(statePath, frontendId);
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
