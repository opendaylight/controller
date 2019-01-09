/*
 * Copyright (c) 2017 Pantheon Technologies s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.access.client;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.persistence.Persistence;
import akka.persistence.SelectedSnapshot;
import akka.persistence.SnapshotMetadata;
import akka.testkit.TestProbe;
import akka.testkit.javadsl.TestKit;
import com.typesafe.config.ConfigFactory;
import java.lang.reflect.Field;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.controller.cluster.access.concepts.ClientIdentifier;
import org.opendaylight.controller.cluster.access.concepts.FrontendIdentifier;
import org.opendaylight.controller.cluster.access.concepts.FrontendType;
import org.opendaylight.controller.cluster.access.concepts.MemberName;
import scala.concurrent.duration.FiniteDuration;

public class ActorBehaviorTest {

    private static final String MEMBER_1_FRONTEND_TYPE_1 = "member-1-frontend-type-1";
    private static final FiniteDuration TIMEOUT = FiniteDuration.create(5, TimeUnit.SECONDS);

    private ActorSystem system;
    private TestProbe probe;
    private ClientActorBehavior<BackendInfo> initialBehavior;
    private MockedSnapshotStore.SaveRequest saveRequest;
    private FrontendIdentifier id;
    private ActorRef mockedActor;

    @Before
    public void setUp() throws Exception {
        initialBehavior = createInitialBehaviorMock();
        system = ActorSystem.apply("system1");
        final ActorRef storeRef = system.registerExtension(Persistence.lookup()).snapshotStoreFor(null,
            ConfigFactory.empty());
        probe = new TestProbe(system);
        storeRef.tell(probe.ref(), ActorRef.noSender());
        final MemberName name = MemberName.forName("member-1");
        id = FrontendIdentifier.create(name, FrontendType.forName("type-1"));
        mockedActor = system.actorOf(MockedActor.props(id, initialBehavior));
        //handle initial actor recovery
        saveRequest = handleRecovery(null);
    }

    @After
    public void tearDown() {
        TestKit.shutdownActorSystem(system);
    }

    @Test
    public void testInitialBehavior() {
        final InternalCommand<BackendInfo> cmd = mock(InternalCommand.class);
        when(cmd.execute(any())).thenReturn(initialBehavior);
        mockedActor.tell(cmd, ActorRef.noSender());
        verify(cmd, timeout(1000)).execute(initialBehavior);
    }

    @Test
    public void testCommandStashing() {
        system.stop(mockedActor);
        mockedActor = system.actorOf(MockedActor.props(id, initialBehavior));
        final InternalCommand<BackendInfo> cmd = mock(InternalCommand.class);
        when(cmd.execute(any())).thenReturn(initialBehavior);
        //send messages before recovery is completed
        mockedActor.tell(cmd, ActorRef.noSender());
        mockedActor.tell(cmd, ActorRef.noSender());
        mockedActor.tell(cmd, ActorRef.noSender());
        //complete recovery
        handleRecovery(null);
        verify(cmd, timeout(1000).times(3)).execute(initialBehavior);
    }

    @Test
    public void testRecoveryAfterRestart() {
        system.stop(mockedActor);
        mockedActor = system.actorOf(MockedActor.props(id, initialBehavior));
        final MockedSnapshotStore.SaveRequest newSaveRequest =
                handleRecovery(new SelectedSnapshot(saveRequest.getMetadata(), saveRequest.getSnapshot()));
        Assert.assertEquals(MEMBER_1_FRONTEND_TYPE_1, newSaveRequest.getMetadata().persistenceId());
    }

    @Test
    public void testRecoveryAfterRestartFrontendIdMismatch() {
        system.stop(mockedActor);
        //start actor again
        mockedActor = system.actorOf(MockedActor.props(id, initialBehavior));
        probe.expectMsgClass(MockedSnapshotStore.LoadRequest.class);
        //offer snapshot with incorrect client id
        final SnapshotMetadata metadata = saveRequest.getMetadata();
        final FrontendIdentifier anotherFrontend = FrontendIdentifier.create(MemberName.forName("another"),
                FrontendType.forName("type-2"));
        final ClientIdentifier incorrectClientId = ClientIdentifier.create(anotherFrontend, 0);
        probe.watch(mockedActor);
        probe.reply(Optional.of(new SelectedSnapshot(metadata, incorrectClientId)));
        //actor should be stopped
        probe.expectTerminated(mockedActor, TIMEOUT);
    }

    @Test
    public void testRecoveryAfterRestartSaveSnapshotFail() {
        system.stop(mockedActor);
        mockedActor = system.actorOf(MockedActor.props(id, initialBehavior));
        probe.watch(mockedActor);
        probe.expectMsgClass(MockedSnapshotStore.LoadRequest.class);
        probe.reply(Optional.empty());
        probe.expectMsgClass(MockedSnapshotStore.SaveRequest.class);
        probe.reply(new RuntimeException("save failed"));
        probe.expectMsgClass(MockedSnapshotStore.DeleteByMetadataRequest.class);
        probe.expectTerminated(mockedActor, TIMEOUT);
    }

    @Test
    public void testRecoveryAfterRestartDeleteSnapshotsFail() {
        system.stop(mockedActor);
        mockedActor = system.actorOf(MockedActor.props(id, initialBehavior));
        probe.watch(mockedActor);
        probe.expectMsgClass(MockedSnapshotStore.LoadRequest.class);
        probe.reply(Optional.empty());
        probe.expectMsgClass(MockedSnapshotStore.SaveRequest.class);
        probe.reply(Void.TYPE);
        probe.expectMsgClass(MockedSnapshotStore.DeleteByCriteriaRequest.class);
        probe.reply(new RuntimeException("delete failed"));
        //actor shouldn't terminate
        probe.expectNoMessage();
    }

    @SuppressWarnings("unchecked")
    private static ClientActorBehavior<BackendInfo> createInitialBehaviorMock() throws Exception {
        final ClientActorBehavior<BackendInfo> initialBehavior = mock(ClientActorBehavior.class);
        //persistenceId() in AbstractClientActorBehavior is final and can't be mocked
        //use reflection to work around this
        final Field context = AbstractClientActorBehavior.class.getDeclaredField("context");
        context.setAccessible(true);
        final AbstractClientActorContext ctx = mock(AbstractClientActorContext.class);
        context.set(initialBehavior, ctx);
        final Field persistenceId = AbstractClientActorContext.class.getDeclaredField("persistenceId");
        persistenceId.setAccessible(true);
        persistenceId.set(ctx, MEMBER_1_FRONTEND_TYPE_1);
        return initialBehavior;
    }

    private MockedSnapshotStore.SaveRequest handleRecovery(final SelectedSnapshot savedState) {
        probe.expectMsgClass(MockedSnapshotStore.LoadRequest.class);
        //offer snapshot
        probe.reply(Optional.ofNullable(savedState));
        final MockedSnapshotStore.SaveRequest nextSaveRequest =
                probe.expectMsgClass(MockedSnapshotStore.SaveRequest.class);
        probe.reply(Void.TYPE);
        //check old snapshots deleted
        probe.expectMsgClass(MockedSnapshotStore.DeleteByCriteriaRequest.class);
        probe.reply(Void.TYPE);
        return nextSaveRequest;
    }

    private static class MockedActor extends AbstractClientActor {

        private final ClientActorBehavior<?> initialBehavior;
        private final ClientActorConfig mockConfig = AccessClientUtil.newMockClientActorConfig();

        private static Props props(final FrontendIdentifier frontendId, final ClientActorBehavior<?> initialBehavior) {
            return Props.create(MockedActor.class, () -> new MockedActor(frontendId, initialBehavior));
        }

        MockedActor(final FrontendIdentifier frontendId, final ClientActorBehavior<?> initialBehavior) {
            super(frontendId, false);
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
