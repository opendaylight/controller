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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.apache.pekko.actor.ActorRef;
import org.apache.pekko.actor.ActorSystem;
import org.apache.pekko.actor.Props;
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
    private ClientActorContext ctx;

    private ActorSystem system;
    private TestProbe probe;
    private ActorRef mockedActor;

    @BeforeEach
    void beforeEach() throws Exception {
        //persistenceId() in AbstractClientActorBehavior is final and can't be mocked
        //use reflection to work around this
        final var context = ClientActorBehavior.class.getDeclaredField("context");
        context.setAccessible(true);
        context.set(initialBehavior, ctx);
        final var persistenceId = ClientActorContext.class.getDeclaredField("persistenceId");
        persistenceId.setAccessible(true);
        persistenceId.set(ctx, MEMBER_1_FRONTEND_TYPE_1);

        system = ActorSystem.apply("system1");
        probe = new TestProbe(system);

        mockedActor = system.actorOf(MockedActor.props(statePath, id, initialBehavior));

        await().atMost(Duration.ofSeconds(5)).untilAsserted(() -> verifyStateFile(0));
    }

    @AfterEach
    void afterEach() throws Exception {
        TestKit.shutdownActorSystem(system);
    }

    @Test
    void testInitialBehavior() {
        doReturn(initialBehavior).when(cmd).execute(any());
        mockedActor.tell(cmd, ActorRef.noSender());
        verify(cmd, timeout(1000)).execute(initialBehavior);
    }

    @Test
    void testRecoveryAfterRestart() {
        system.stop(mockedActor);
        mockedActor = system.actorOf(MockedActor.props(statePath, id, initialBehavior));

        // state should be updated
        await().atMost(Duration.ofSeconds(5)).untilAsserted(() -> verifyStateFile(1));

        // there should be no further save
        probe.expectNoMessage();
    }

    @Test
    void testRecoveryAfterRestartFrontendIdMismatch() throws Exception {
        system.stop(mockedActor);

        Files.write(propsFile(), List.of("client-type=type-2", "generation=0", "member-name=another"));

        //start actor again
        mockedActor = system.actorOf(MockedActor.props(statePath, id, initialBehavior));
        probe.watch(mockedActor);
        //actor should be stopped
        probe.expectTerminated(mockedActor, TIMEOUT);
    }

    private void verifyStateFile(final long expectedGeneration) {
        final List<String> lines;
        try {
            lines = Files.readAllLines(propsFile());
        } catch (IOException e) {
            throw new AssertionError(e);
        }

        assertEquals(5, lines.size());
        assertEquals("#Critical persistent state. Do not touch unless you know what you are doing!", lines.get(0));
        assertEquals("client-type=type-1", lines.get(2));
        assertEquals("generation=" + expectedGeneration, lines.get(3));
        assertEquals("member-name=member-1", lines.get(4));
    }

    private Path propsFile() {
        return statePath.resolve("odl.cluster.client").resolve("member-1").resolve("type-1.properties");
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
