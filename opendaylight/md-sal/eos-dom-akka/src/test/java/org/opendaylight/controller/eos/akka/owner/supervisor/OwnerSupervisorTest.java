/*
 * Copyright (c) 2021 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.eos.akka.owner.supervisor;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import org.apache.pekko.actor.testkit.typed.javadsl.ActorTestKit;
import org.apache.pekko.actor.typed.ActorRef;
import org.apache.pekko.actor.typed.Behavior;
import org.apache.pekko.actor.typed.javadsl.AbstractBehavior;
import org.apache.pekko.actor.typed.javadsl.ActorContext;
import org.apache.pekko.actor.typed.javadsl.Behaviors;
import org.apache.pekko.actor.typed.javadsl.Receive;
import org.apache.pekko.cluster.typed.Cluster;
import org.apache.pekko.cluster.typed.ClusterSingleton;
import org.apache.pekko.cluster.typed.SingletonActor;
import org.junit.Test;
import org.opendaylight.controller.eos.akka.AbstractNativeEosTest;
import org.opendaylight.controller.eos.akka.bootstrap.command.BootstrapCommand;
import org.opendaylight.controller.eos.akka.bootstrap.command.GetRunningContext;
import org.opendaylight.controller.eos.akka.bootstrap.command.RunningContext;
import org.opendaylight.controller.eos.akka.owner.checker.OwnerStateChecker;
import org.opendaylight.controller.eos.akka.owner.checker.command.StateCheckerCommand;
import org.opendaylight.controller.eos.akka.owner.supervisor.command.InitialCandidateSync;
import org.opendaylight.controller.eos.akka.owner.supervisor.command.OwnerSupervisorCommand;
import org.opendaylight.controller.eos.akka.registry.candidate.CandidateRegistry;
import org.opendaylight.controller.eos.akka.registry.candidate.command.CandidateRegistryCommand;
import org.opendaylight.controller.eos.akka.registry.listener.type.EntityTypeListenerRegistry;
import org.opendaylight.controller.eos.akka.registry.listener.type.command.TypeListenerRegistryCommand;
import org.opendaylight.mdsal.eos.dom.api.DOMEntity;

public class OwnerSupervisorTest extends AbstractNativeEosTest {

    @Test
    public void testCandidatePickingWhenUnreachableCandidates() throws Exception {

        final ClusterNode node = startup(2550, Collections.singletonList("member-1"));
        try {
            reachableMember(node, "member-2", DEFAULT_DATACENTER);
            reachableMember(node, "member-3", DEFAULT_DATACENTER);
            registerCandidates(node, ENTITY_1, "member-1", "member-2", "member-3");

            final MockEntityOwnershipListener listener = registerListener(node, ENTITY_1);
            verifyListenerState(listener, ENTITY_1,true, true, false);

            unreachableMember(node, "member-1", DEFAULT_DATACENTER);
            verifyListenerState(listener, ENTITY_1, true, false, true);

            unreachableMember(node, "member-2", DEFAULT_DATACENTER);
            verifyListenerState(listener, ENTITY_1, true, false, false);

            unreachableMember(node, "member-3", DEFAULT_DATACENTER);
            verifyListenerState(listener, ENTITY_1, false, false, false);

            reachableMember(node, "member-2", DEFAULT_DATACENTER);
            verifyListenerState(listener, ENTITY_1, true, false, false);

            // no notification here as member-2 is already the owner
            reachableMember(node, "member-1", DEFAULT_DATACENTER);

            unreachableMember(node, "member-2", DEFAULT_DATACENTER);
            verifyListenerState(listener, ENTITY_1,true, true, false);
        } finally {
            ActorTestKit.shutdown(node.getActorSystem());
        }
    }

    @Test
    public void testSupervisorInitWithMissingOwners() throws Exception {
        final Map<DOMEntity, Set<String>> candidates = new HashMap<>();
        candidates.put(ENTITY_1, Set.of("member-1"));
        candidates.put(ENTITY_2, Set.of("member-2"));

        final ClusterNode node = startup(2550, Collections.singletonList("member-1"), Collections.emptyList(),
                () -> mockedBootstrap(candidates, new HashMap<>()));

        try {
            waitUntillOwnerPresent(node, ENTITY_1);

            // also do a proper register so the listener from the type lister actor are spawned
            registerCandidates(node, ENTITY_1, "member-1");
            registerCandidates(node, ENTITY_2, "member-2");

            final MockEntityOwnershipListener listener1 = registerListener(node, ENTITY_1);
            final MockEntityOwnershipListener listener2 = registerListener(node, ENTITY_2);

            // first entity should have correctly assigned owner as its reachable
            verifyListenerState(listener1, ENTITY_1, true, true, false);
            // this one could not be assigned during init as we dont have member-2 thats reachable
            verifyListenerState(listener2, ENTITY_2, false, false, false);

            reachableMember(node, "member-2", DEFAULT_DATACENTER);
            verifyListenerState(listener2, ENTITY_2, true, false, false);
        } finally {
            ActorTestKit.shutdown(node.getActorSystem());
        }
    }

    private static Behavior<BootstrapCommand> mockedBootstrap(final Map<DOMEntity, Set<String>> currentCandidates,
                                                              final Map<DOMEntity, String> currentOwners) {
        return Behaviors.setup(context -> MockBootstrap.create(currentCandidates, currentOwners));
    }

    /**
     * Initial behavior that skips initial sync and instead initializes OwnerSupervisor with provided values.
     */
    private static final class MockSyncer extends AbstractBehavior<OwnerSupervisorCommand> {

        private final Map<DOMEntity, Set<String>> currentCandidates;
        private final Map<DOMEntity, String> currentOwners;

        private MockSyncer(final ActorContext<OwnerSupervisorCommand> context,
                           final Map<DOMEntity, Set<String>> currentCandidates,
                           final Map<DOMEntity, String> currentOwners) {
            super(context);
            this.currentCandidates = currentCandidates;
            this.currentOwners = currentOwners;

            context.getSelf().tell(new InitialCandidateSync(null));
        }

        public static Behavior<OwnerSupervisorCommand> create(final Map<DOMEntity, Set<String>> currentCandidates,
                                                              final Map<DOMEntity, String> currentOwners) {
            return Behaviors.setup(ctx -> new MockSyncer(ctx, currentCandidates, currentOwners));
        }

        @Override
        public Receive<OwnerSupervisorCommand> createReceive() {
            return newReceiveBuilder()
                    .onMessage(InitialCandidateSync.class, this::switchToSupervisor)
                    .build();
        }

        private Behavior<OwnerSupervisorCommand> switchToSupervisor(final InitialCandidateSync message) {
            return OwnerSupervisor.create(currentCandidates, currentOwners, CODEC_CONTEXT.getInstanceIdentifierCodec());
        }
    }

    /**
     * Bootstrap with OwnerSyncer replaced with the testing syncer behavior.
     */
    private static final class MockBootstrap extends AbstractBehavior<BootstrapCommand> {

        private final ActorRef<TypeListenerRegistryCommand> listenerRegistry;
        private final ActorRef<CandidateRegistryCommand> candidateRegistry;
        private final ActorRef<StateCheckerCommand> ownerStateChecker;
        private final ActorRef<OwnerSupervisorCommand> ownerSupervisor;

        private MockBootstrap(final ActorContext<BootstrapCommand> context,
                              final Map<DOMEntity, Set<String>> currentCandidates,
                              final Map<DOMEntity, String> currentOwners) {
            super(context);

            final Cluster cluster = Cluster.get(context.getSystem());
            final String role = cluster.selfMember().getRoles().iterator().next();

            listenerRegistry = context.spawn(EntityTypeListenerRegistry.create(role), "ListenerRegistry");
            candidateRegistry = context.spawn(CandidateRegistry.create(), "CandidateRegistry");

            final ClusterSingleton clusterSingleton = ClusterSingleton.get(context.getSystem());
            // start the initial sync behavior that switches to the regular one after syncing
            ownerSupervisor = clusterSingleton.init(SingletonActor.of(
                    MockSyncer.create(currentCandidates, currentOwners), "OwnerSupervisor"));

            ownerStateChecker = context.spawn(OwnerStateChecker.create(role, ownerSupervisor, null),
                    "OwnerStateChecker");
        }

        public static Behavior<BootstrapCommand> create(final Map<DOMEntity, Set<String>> currentCandidates,
                                                        final Map<DOMEntity, String> currentOwners) {
            return Behaviors.setup(ctx -> new MockBootstrap(ctx, currentCandidates, currentOwners));
        }

        @Override
        public Receive<BootstrapCommand> createReceive() {
            return newReceiveBuilder()
                    .onMessage(GetRunningContext.class, this::onGetRunningContext)
                    .build();
        }

        private Behavior<BootstrapCommand> onGetRunningContext(final GetRunningContext request) {
            request.getReplyTo().tell(
                    new RunningContext(listenerRegistry, candidateRegistry,ownerStateChecker, ownerSupervisor));
            return this;
        }
    }

}
