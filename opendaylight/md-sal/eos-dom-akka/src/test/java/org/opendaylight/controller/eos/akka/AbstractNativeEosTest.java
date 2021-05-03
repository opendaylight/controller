/*
 * Copyright (c) 2021 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.eos.akka;

import static org.awaitility.Awaitility.await;
import static org.junit.Assert.assertEquals;

import akka.actor.ActorSystem;
import akka.actor.Address;
import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.Adapter;
import akka.actor.typed.javadsl.AskPattern;
import akka.actor.typed.javadsl.Behaviors;
import akka.cluster.ddata.LWWRegister;
import akka.cluster.ddata.LWWRegisterKey;
import akka.cluster.ddata.typed.javadsl.DistributedData;
import akka.cluster.ddata.typed.javadsl.Replicator;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import org.opendaylight.controller.eos.akka.bootstrap.EOSMain;
import org.opendaylight.controller.eos.akka.bootstrap.command.BootstrapCommand;
import org.opendaylight.controller.eos.akka.bootstrap.command.GetRunningContext;
import org.opendaylight.controller.eos.akka.bootstrap.command.RunningContext;
import org.opendaylight.controller.eos.akka.owner.supervisor.command.ActivateDataCenter;
import org.opendaylight.controller.eos.akka.owner.supervisor.command.DeactivateDataCenter;
import org.opendaylight.controller.eos.akka.owner.supervisor.command.MemberReachableEvent;
import org.opendaylight.controller.eos.akka.owner.supervisor.command.MemberUnreachableEvent;
import org.opendaylight.controller.eos.akka.owner.supervisor.command.OwnerSupervisorCommand;
import org.opendaylight.controller.eos.akka.owner.supervisor.command.OwnerSupervisorReply;
import org.opendaylight.controller.eos.akka.registry.candidate.command.CandidateRegistryCommand;
import org.opendaylight.controller.eos.akka.registry.candidate.command.RegisterCandidate;
import org.opendaylight.controller.eos.akka.registry.candidate.command.UnregisterCandidate;
import org.opendaylight.controller.eos.akka.registry.listener.type.command.RegisterListener;
import org.opendaylight.controller.eos.akka.registry.listener.type.command.TypeListenerRegistryCommand;
import org.opendaylight.mdsal.eos.dom.api.DOMEntity;
import org.opendaylight.mdsal.eos.dom.api.DOMEntityOwnershipChange;
import org.opendaylight.mdsal.eos.dom.api.DOMEntityOwnershipListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractNativeEosTest {

    public static final DOMEntity ENTITY_1 = new DOMEntity("test-type", "entity-1");
    public static final DOMEntity ENTITY_2 = new DOMEntity("test-type-2", "entity-2");

    protected static final String DEFAULT_DATACENTER = "dc-default";

    protected static final List<String> TWO_NODE_SEED_NODES =
            List.of("akka://ClusterSystem@127.0.0.1:2550",
                    "akka://ClusterSystem@127.0.0.1:2551");

    protected static final List<String> THREE_NODE_SEED_NODES =
            List.of("akka://ClusterSystem@127.0.0.1:2550",
                    "akka://ClusterSystem@127.0.0.1:2551",
                    "akka://ClusterSystem@127.0.0.1:2552");

    protected static final List<String> DATACENTER_SEED_NODES =
            List.of("akka://ClusterSystem@127.0.0.1:2550",
                    "akka://ClusterSystem@127.0.0.1:2551",
                    "akka://ClusterSystem@127.0.0.1:2552",
                    "akka://ClusterSystem@127.0.0.1:2553");

    private static final String REMOTE_PROTOCOL = "akka";
    private static final String PORT_PARAM = "akka.remote.artery.canonical.port";
    private static final String ROLE_PARAM = "akka.cluster.roles";
    private static final String SEED_NODES_PARAM = "akka.cluster.seed-nodes";
    private static final String DATA_CENTER_PARAM = "akka.cluster.multi-data-center.self-data-center";


    protected static ClusterNode startupRemote(final int port, final List<String> roles)
            throws ExecutionException, InterruptedException {
        return startup(port, roles, THREE_NODE_SEED_NODES);
    }

    protected static ClusterNode startupRemote(final int port, final List<String> roles, final List<String> seedNodes)
            throws ExecutionException, InterruptedException {
        return startup(port, roles, seedNodes);
    }

    protected static ClusterNode startup(final int port, final List<String> roles)
            throws ExecutionException, InterruptedException {
        return startup(port, roles, List.of());
    }

    protected static ClusterNode startup(final int port, final List<String> roles, final List<String> seedNodes)
            throws ExecutionException, InterruptedException {

        return startup(port, roles, seedNodes, AbstractNativeEosTest::rootBehavior);
    }

    protected static ClusterNode startup(final int port, final List<String> roles, final List<String> seedNodes,
                                         final Supplier<Behavior<BootstrapCommand>> bootstrap)
            throws ExecutionException, InterruptedException {
        // Override the configuration
        final Map<String, Object> overrides = new HashMap<>(4);
        overrides.put(PORT_PARAM, port);
        overrides.put(ROLE_PARAM, roles);
        if (!seedNodes.isEmpty()) {
            overrides.put(SEED_NODES_PARAM, seedNodes);
        }

        final Config config = ConfigFactory.parseMap(overrides).withFallback(ConfigFactory.load());

        // Create a classic Akka system since thats what we will have in osgi
        final akka.actor.ActorSystem system = akka.actor.ActorSystem.create("ClusterSystem", config);
        final ActorRef<BootstrapCommand> eosBootstrap =
                Adapter.spawn(system, bootstrap.get(), "EOSBootstrap");

        final CompletionStage<RunningContext> ask = AskPattern.ask(eosBootstrap,
                GetRunningContext::new,
                Duration.ofSeconds(5),
                Adapter.toTyped(system.scheduler()));
        final RunningContext runningContext = ask.toCompletableFuture().get();

        return new ClusterNode(port, roles, system, eosBootstrap, runningContext.getListenerRegistry(),
                runningContext.getCandidateRegistry(), runningContext.getOwnerSupervisor());
    }

    protected static ClusterNode startupWithDatacenter(final int port, final List<String> roles,
                                                       final List<String> seedNodes, final String dataCenter)
            throws ExecutionException, InterruptedException {
        final Map<String, Object> overrides = new HashMap<>();
        overrides.put(PORT_PARAM, port);
        overrides.put(ROLE_PARAM, roles);
        if (!seedNodes.isEmpty()) {
            overrides.put(SEED_NODES_PARAM, seedNodes);
        }
        overrides.put(DATA_CENTER_PARAM, dataCenter);

        final Config config = ConfigFactory.parseMap(overrides)
                .withFallback(ConfigFactory.load());

        // Create a classic Akka system since thats what we will have in osgi
        final akka.actor.ActorSystem system = akka.actor.ActorSystem.create("ClusterSystem", config);
        final ActorRef<BootstrapCommand> eosBootstrap =
                Adapter.spawn(system, EOSMain.create(), "EOSBootstrap");

        final CompletionStage<RunningContext> ask = AskPattern.ask(eosBootstrap,
                GetRunningContext::new,
                Duration.ofSeconds(5),
                Adapter.toTyped(system.scheduler()));
        final RunningContext runningContext = ask.toCompletableFuture().get();

        return new ClusterNode(port, roles, system, eosBootstrap, runningContext.getListenerRegistry(),
                runningContext.getCandidateRegistry(), runningContext.getOwnerSupervisor());
    }

    private static Behavior<BootstrapCommand> rootBehavior() {
        return Behaviors.setup(context -> EOSMain.create());
    }

    protected static void registerCandidates(final ClusterNode node, final DOMEntity entity, final String... members) {
        final ActorRef<CandidateRegistryCommand> candidateRegistry = node.getCandidateRegistry();
        registerCandidates(candidateRegistry, entity, members);
    }

    protected static void registerCandidates(final ActorRef<CandidateRegistryCommand> candidateRegistry,
                                             final DOMEntity entity, final String... members) {
        for (final String member : members) {
            candidateRegistry.tell(new RegisterCandidate(entity, member));
        }
    }

    protected static void unregisterCandidates(final ClusterNode node, final DOMEntity entity,
                                               final String... members) {
        final ActorRef<CandidateRegistryCommand> candidateRegistry = node.getCandidateRegistry();
        for (final String member : members) {
            candidateRegistry.tell(new UnregisterCandidate(entity, member));
        }
    }

    protected static  MockEntityOwnershipListener registerListener(final ClusterNode node, final DOMEntity entity) {
        final ActorRef<TypeListenerRegistryCommand> listenerRegistry = node.getListenerRegistry();
        final MockEntityOwnershipListener listener = new MockEntityOwnershipListener(node.getRoles().get(0));
        listenerRegistry.tell(new RegisterListener(entity.getType(), listener));

        return listener;
    }

    protected static void reachableMember(final ClusterNode node, final String... role) {
        reachableMember(node.getOwnerSupervisor(), role);
    }

    protected static void reachableMember(final ActorRef<OwnerSupervisorCommand> ownerSupervisor,
                                          final String... role) {
        ownerSupervisor.tell(new MemberReachableEvent(
                new Address(REMOTE_PROTOCOL, "ClusterSystem@127.0.0.1:2550"), Set.of(role)));
    }

    protected static void unreachableMember(final ClusterNode node, final String... role) {
        unreachableMember(node.getOwnerSupervisor(), role);
    }

    protected static void unreachableMember(final ActorRef<OwnerSupervisorCommand> ownerSupervisor,
                                            final String... role) {
        ownerSupervisor.tell(new MemberUnreachableEvent(
                new Address(REMOTE_PROTOCOL, "ClusterSystem@127.0.0.1:2550"), Set.of(role)));
    }

    protected static void waitUntillOwnerPresent(final ClusterNode clusterNode, final DOMEntity entity) {
        await().until(() -> {
            final DistributedData distributedData = DistributedData.get(clusterNode.getActorSystem());
            final CompletionStage<Replicator.GetResponse<LWWRegister<String>>> ask =
                    AskPattern.ask(distributedData.replicator(),
                            replyTo -> new Replicator.Get<>(
                                    new LWWRegisterKey<>(entity.toString()), Replicator.readLocal(), replyTo),
                            Duration.ofSeconds(5),
                            clusterNode.getActorSystem().scheduler());

            final Replicator.GetResponse<LWWRegister<String>> response =
                    ask.toCompletableFuture().get(5, TimeUnit.SECONDS);

            if (response instanceof Replicator.GetSuccess) {
                final String owner = ((Replicator.GetSuccess<LWWRegister<String>>) response).dataValue().getValue();
                return !owner.isEmpty();
            }

            return false;
        });
    }

    protected static CompletableFuture<OwnerSupervisorReply> activateDatacenter(final ClusterNode clusterNode) {
        final CompletionStage<OwnerSupervisorReply> ask =
                AskPattern.ask(clusterNode.getOwnerSupervisor(),
                        ActivateDataCenter::new,
                        Duration.ofSeconds(20),
                        clusterNode.actorSystem.scheduler());
        return ask.toCompletableFuture();
    }

    protected static CompletableFuture<OwnerSupervisorReply> deactivateDatacenter(final ClusterNode clusterNode) {
        final CompletionStage<OwnerSupervisorReply> ask =
                AskPattern.ask(clusterNode.getOwnerSupervisor(),
                        DeactivateDataCenter::new,
                        Duration.ofSeconds(20),
                        clusterNode.actorSystem.scheduler());
        return ask.toCompletableFuture();
    }

    protected static void verifyListenerState(final MockEntityOwnershipListener listener, final DOMEntity entity,
                                              final boolean hasOwner, final boolean isOwner, final boolean wasOwner) {
        await().until(() -> !listener.getChanges().isEmpty());

        await().untilAsserted(() -> {
            final List<DOMEntityOwnershipChange> changes = listener.getChanges();
            final DOMEntityOwnershipChange domEntityOwnershipChange = listener.getChanges().get(changes.size() - 1);
            assertEquals(entity, domEntityOwnershipChange.getEntity());

            assertEquals(hasOwner, domEntityOwnershipChange.getState().hasOwner());
            assertEquals(isOwner, domEntityOwnershipChange.getState().isOwner());
            assertEquals(wasOwner, domEntityOwnershipChange.getState().wasOwner());
        });
    }

    protected static void verifyNoNotifications(final MockEntityOwnershipListener listener) {
        await().pollDelay(2, TimeUnit.SECONDS).until(() -> listener.getChanges().isEmpty());
    }

    protected static final class ClusterNode {
        private final int port;
        private final List<String> roles;
        private final akka.actor.typed.ActorSystem<Void> actorSystem;
        private final ActorRef<BootstrapCommand> eosBootstrap;
        private final ActorRef<TypeListenerRegistryCommand> listenerRegistry;
        private final ActorRef<CandidateRegistryCommand> candidateRegistry;
        private final ActorRef<OwnerSupervisorCommand> ownerSupervisor;

        private ClusterNode(final int port,
                            final List<String> roles,
                            final ActorSystem actorSystem,
                            final ActorRef<BootstrapCommand> eosBootstrap,
                            final ActorRef<TypeListenerRegistryCommand> listenerRegistry,
                            final ActorRef<CandidateRegistryCommand> candidateRegistry,
                            final ActorRef<OwnerSupervisorCommand> ownerSupervisor) {
            this.port = port;
            this.roles = roles;
            this.actorSystem = Adapter.toTyped(actorSystem);
            this.eosBootstrap = eosBootstrap;
            this.listenerRegistry = listenerRegistry;
            this.candidateRegistry = candidateRegistry;
            this.ownerSupervisor = ownerSupervisor;
        }

        public int getPort() {
            return port;
        }

        public akka.actor.typed.ActorSystem<Void> getActorSystem() {
            return actorSystem;
        }

        public ActorRef<BootstrapCommand> getEosBootstrap() {
            return eosBootstrap;
        }

        public ActorRef<TypeListenerRegistryCommand> getListenerRegistry() {
            return listenerRegistry;
        }

        public ActorRef<CandidateRegistryCommand> getCandidateRegistry() {
            return candidateRegistry;
        }

        public ActorRef<OwnerSupervisorCommand> getOwnerSupervisor() {
            return ownerSupervisor;
        }

        public List<String> getRoles() {
            return roles;
        }
    }

    protected static final class MockEntityOwnershipListener implements DOMEntityOwnershipListener {

        private final Logger log;

        private final List<DOMEntityOwnershipChange> changes = new ArrayList<>();
        private final String member;

        public MockEntityOwnershipListener(final String member) {
            log = LoggerFactory.getLogger("EOS-listener-" + member);
            this.member = member;
        }

        @Override
        public void ownershipChanged(final DOMEntityOwnershipChange ownershipChange) {
            log.info("{} Received ownershipCHanged: {}", member, ownershipChange);
            log.info("{} changes: {}", member, changes.size());
            changes.add(ownershipChange);
        }

        public List<DOMEntityOwnershipChange> getChanges() {
            return changes;
        }
    }
}
