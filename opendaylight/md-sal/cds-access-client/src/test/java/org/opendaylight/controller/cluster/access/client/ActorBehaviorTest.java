/*
 * Copyright (c) 2017 Pantheon Technologies s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.access.client;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.PoisonPill;
import akka.actor.Props;
import akka.persistence.SelectedSnapshot;
import akka.persistence.SnapshotMetadata;
import akka.testkit.TestProbe;
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
import scala.concurrent.Await;
import scala.concurrent.duration.Duration;

public class ActorBehaviorTest {

    private static final String MEMBER_1_FRONTEND_TYPE_1 = "member-1-frontend-type-1";

    private ActorSystem system;
    private TestProbe probe;
    private ActorRef mockedActor;
    private ClientActorBehavior initialBehavior;
    private MockedSnapshotStore.SaveRequest saveRequest;

    @Before
    public void setUp() throws Exception {
        initialBehavior = createInitialBehaviorMock();
        system = ActorSystem.apply("system1");
        probe = new TestProbe(system, "probe");
        final MemberName name = MemberName.forName("member-1");
        final FrontendIdentifier id = FrontendIdentifier.create(name, FrontendType.forName("type-1"));
        mockedActor = system.actorOf(MockedActor.props(id, initialBehavior));
        //handle initial actor recovery
        probe.expectMsgClass(MockedSnapshotStore.LoadRequest.class);
        probe.reply(Optional.empty());
        saveRequest = probe.expectMsgClass(MockedSnapshotStore.SaveRequest.class);
        probe.reply(Void.TYPE);
        probe.expectMsgClass(MockedSnapshotStore.DeleteByCriteriaRequest.class);
        probe.reply(Void.TYPE);
    }

    @Test
    public void testInitialBehavior() throws Exception {
        final String msg = "AAAA";
        when(initialBehavior.onCommand(msg)).thenReturn(initialBehavior);
        mockedActor.tell(msg, probe.ref());
        verify(initialBehavior, timeout(1000)).onReceiveCommand(msg);
    }

    @Test
    public void testRecoveryAfterRestart() throws Exception {
        when(initialBehavior.onCommand("fail")).thenThrow(new RuntimeException("fail"));
        mockedActor.tell("fail", probe.ref());
        probe.expectMsgClass(MockedSnapshotStore.LoadRequest.class);
        //offer snapshot
        probe.reply(Optional.of(new SelectedSnapshot(saveRequest.getMetadata(), saveRequest.getSnapshot())));
        final MockedSnapshotStore.SaveRequest nextSaveRequest =
                probe.expectMsgClass(MockedSnapshotStore.SaveRequest.class);
        Assert.assertEquals(MEMBER_1_FRONTEND_TYPE_1, nextSaveRequest.getMetadata().persistenceId());
        probe.reply(Void.TYPE);
        //check old snapshots deleted
        probe.expectMsgClass(MockedSnapshotStore.DeleteByCriteriaRequest.class);
        probe.reply(Void.TYPE);
    }

    @Test
    public void testRecoveryAfterRestartTypeMismatch() throws Exception {
        probe.watch(mockedActor);
        when(initialBehavior.onCommand("fail")).thenThrow(new RuntimeException("fail"));
        mockedActor.tell("fail", probe.ref());
        probe.expectMsgClass(MockedSnapshotStore.LoadRequest.class);
        //offer snapshot with incorrect client id
        final SnapshotMetadata metadata = saveRequest.getMetadata();
        final FrontendIdentifier anotherFrontend = FrontendIdentifier.create(MemberName.forName("another"),
                FrontendType.forName("type-2"));
        final ClientIdentifier incorrectClientId = ClientIdentifier.create(anotherFrontend, 0);
        probe.reply(Optional.of(new SelectedSnapshot(metadata, incorrectClientId)));
        //actor should be stopped
        probe.expectTerminated(mockedActor, Duration.apply(5, TimeUnit.SECONDS));
    }

    @After
    public void tearDown() throws Exception {
        probe.ref().tell(PoisonPill.getInstance(), ActorRef.noSender());
        Await.result(system.terminate(), Duration.apply(10, TimeUnit.SECONDS));
    }

    private ClientActorBehavior createInitialBehaviorMock() throws NoSuchFieldException, IllegalAccessException {
        final ClientActorBehavior initialBehavior = mock(ClientActorBehavior.class);
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

    private static class MockedActor extends AbstractClientActor {

        private final ClientActorBehavior initialBehavior;

        private static Props props(final FrontendIdentifier frontendId, final ClientActorBehavior initialBehavior) {
            return Props.create(MockedActor.class, () -> new MockedActor(frontendId, initialBehavior));
        }

        private MockedActor(final FrontendIdentifier frontendId, final ClientActorBehavior initialBehavior) {
            super(frontendId);
            this.initialBehavior = initialBehavior;
        }

        @Override
        protected ClientActorBehavior<?> initialBehavior(final ClientActorContext context) {
            return initialBehavior;
        }

    }

}