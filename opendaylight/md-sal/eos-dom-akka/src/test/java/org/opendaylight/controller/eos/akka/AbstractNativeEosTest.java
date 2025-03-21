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
import org.apache.pekko.actor.ActorSystem;
import org.apache.pekko.actor.Address;
import org.apache.pekko.actor.typed.ActorRef;
import org.apache.pekko.actor.typed.Behavior;
import org.apache.pekko.actor.typed.javadsl.Adapter;
import org.apache.pekko.actor.typed.javadsl.AskPattern;
import org.apache.pekko.actor.typed.javadsl.Behaviors;
import org.apache.pekko.cluster.ddata.LWWRegister;
import org.apache.pekko.cluster.ddata.LWWRegisterKey;
import org.apache.pekko.cluster.ddata.ORMap;
import org.apache.pekko.cluster.ddata.ORSet;
import org.apache.pekko.cluster.ddata.typed.javadsl.DistributedData;
import org.apache.pekko.cluster.ddata.typed.javadsl.Replicator;
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
import org.opendaylight.controller.eos.akka.registry.candidate.CandidateRegistry;
import org.opendaylight.controller.eos.akka.registry.candidate.command.CandidateRegistryCommand;
import org.opendaylight.controller.eos.akka.registry.candidate.command.RegisterCandidate;
import org.opendaylight.controller.eos.akka.registry.candidate.command.UnregisterCandidate;
import org.opendaylight.controller.eos.akka.registry.listener.type.command.EntityOwnerChanged;
import org.opendaylight.controller.eos.akka.registry.listener.type.command.RegisterListener;
import org.opendaylight.controller.eos.akka.registry.listener.type.command.TypeListenerRegistryCommand;
import org.opendaylight.mdsal.eos.common.api.EntityOwnershipStateChange;
import org.opendaylight.mdsal.eos.dom.api.DOMEntity;
import org.opendaylight.mdsal.eos.dom.api.DOMEntityOwnershipListener;
import org.opendaylight.yangtools.binding.data.codec.impl.di.DefaultBindingDOMCodecFactory;
import org.opendaylight.yangtools.binding.data.codec.spi.BindingDOMCodecServices;
import org.opendaylight.yangtools.binding.runtime.spi.BindingRuntimeHelpers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractNativeEosTest {

    public static final DOMEntity ENTITY_1 = new DOMEntity("test-type", "entity-1");
    public static final DOMEntity ENTITY_2 = new DOMEntity("test-type-2", "entity-2");

    protected static final String DEFAULT_DATACENTER = "dc-default";

    protected static final List<String> TWO_NODE_SEED_NODES =
            List.of("pekko://ClusterSystem@127.0.0.1:2550",
                    "pekko://ClusterSystem@127.0.0.1:2551");

    protected static final List<String> THREE_NODE_SEED_NODES =
            List.of("pekko://ClusterSystem@127.0.0.1:2550",
                    "pekko://ClusterSystem@127.0.0.1:2551",
                    "pekko://ClusterSystem@127.0.0.1:2552");

    protected static final List<String> DATACENTER_SEED_NODES =
            List.of("pekko://ClusterSystem@127.0.0.1:2550",
                    "pekko://ClusterSystem@127.0.0.1:2551",
                    "pekko://ClusterSystem@127.0.0.1:2552",
                    "pekko://ClusterSystem@127.0.0.1:2553");

    protected static final BindingDOMCodecServices CODEC_CONTEXT =
        new DefaultBindingDOMCodecFactory().createBindingDOMCodec(BindingRuntimeHelpers.createRuntimeContext());

    private static final String REMOTE_PROTOCOL = "pekko";
    private static final String PORT_PARAM = "pekko.remote.artery.canonical.port";
    private static final String ROLE_PARAM = "pekko.cluster.roles";
    private static final String SEED_NODES_PARAM = "pekko.cluster.seed-nodes";
    private static final String DATA_CENTER_PARAM = "pekko.cluster.multi-data-center.self-data-center";

    protected static MockNativeEntityOwnershipService startupNativeService(final int port, final List<String> roles,
                                                                           final List<String> seedNodes)
            throws ExecutionException, InterruptedException {
        final Map<String, Object> overrides = new HashMap<>();
        overrides.put(PORT_PARAM, port);
        overrides.put(ROLE_PARAM, roles);
        if (!seedNodes.isEmpty()) {
            overrides.put(SEED_NODES_PARAM, seedNodes);
        }

        final Config config = ConfigFactory.parseMap(overrides)
                .withFallback(ConfigFactory.load());

        // Create a classic Pekko system since thats what we will have in osgi
        final var system = org.apache.pekko.actor.ActorSystem.create("ClusterSystem", config);

        return new MockNativeEntityOwnershipService(system);
    }

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

        // Create a classic Pekko system since thats what we will have in osgi
        final var system = org.apache.pekko.actor.ActorSystem.create("ClusterSystem", config);
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
        final org.apache.pekko.actor.ActorSystem system = startupActorSystem(port, roles, seedNodes, dataCenter);
        final ActorRef<BootstrapCommand> eosBootstrap =
                Adapter.spawn(system, EOSMain.create(CODEC_CONTEXT.getInstanceIdentifierCodec()), "EOSBootstrap");

        final CompletionStage<RunningContext> ask = AskPattern.ask(eosBootstrap,
                GetRunningContext::new,
                Duration.ofSeconds(5),
                Adapter.toTyped(system.scheduler()));
        final RunningContext runningContext = ask.toCompletableFuture().get();

        return new ClusterNode(port, roles, system, eosBootstrap, runningContext.getListenerRegistry(),
                runningContext.getCandidateRegistry(), runningContext.getOwnerSupervisor());
    }

    protected static org.apache.pekko.actor.ActorSystem startupActorSystem(final int port, final List<String> roles,
                                                               final List<String> seedNodes) {
        final Map<String, Object> overrides = new HashMap<>();
        overrides.put(PORT_PARAM, port);
        overrides.put(ROLE_PARAM, roles);
        if (!seedNodes.isEmpty()) {
            overrides.put(SEED_NODES_PARAM, seedNodes);
        }

        final Config config = ConfigFactory.parseMap(overrides)
                .withFallback(ConfigFactory.load());

        // Create a classic Pekko system since thats what we will have in osgi
        return org.apache.pekko.actor.ActorSystem.create("ClusterSystem", config);
    }

    protected static org.apache.pekko.actor.ActorSystem startupActorSystem(final int port, final List<String> roles,
                                                               final List<String> seedNodes, final String dataCenter) {
        final Map<String, Object> overrides = new HashMap<>();
        overrides.put(PORT_PARAM, port);
        overrides.put(ROLE_PARAM, roles);
        if (!seedNodes.isEmpty()) {
            overrides.put(SEED_NODES_PARAM, seedNodes);
        }
        overrides.put(DATA_CENTER_PARAM, dataCenter);

        final Config config = ConfigFactory.parseMap(overrides)
                .withFallback(ConfigFactory.load());

        // Create a classic Pekko system since thats what we will have in osgi
        return org.apache.pekko.actor.ActorSystem.create("ClusterSystem", config);
    }

    private static Behavior<BootstrapCommand> rootBehavior() {
        return Behaviors.setup(context -> EOSMain.create(CODEC_CONTEXT.getInstanceIdentifierCodec()));
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
        await().atMost(Duration.ofSeconds(15)).until(() -> {
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

    protected static void waitUntillCandidatePresent(final ClusterNode clusterNode, final DOMEntity entity,
                                                     final String candidate) {
        await().atMost(Duration.ofSeconds(15)).until(() -> {
            final DistributedData distributedData = DistributedData.get(clusterNode.getActorSystem());

            final CompletionStage<Replicator.GetResponse<ORMap<DOMEntity, ORSet<String>>>> ask =
                    AskPattern.ask(distributedData.replicator(),
                            replyTo -> new Replicator.Get<>(
                                    CandidateRegistry.KEY, Replicator.readLocal(), replyTo),
                            Duration.ofSeconds(5),
                            clusterNode.getActorSystem().scheduler());

            final Replicator.GetResponse<ORMap<DOMEntity, ORSet<String>>> response =
                    ask.toCompletableFuture().get(5, TimeUnit.SECONDS);

            if (response instanceof Replicator.GetSuccess) {
                final Map<DOMEntity, ORSet<String>> entries =
                        ((Replicator.GetSuccess<ORMap<DOMEntity, ORSet<String>>>) response).dataValue().getEntries();

                return entries.get(entity).contains(candidate);

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

        await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
            final var changes = listener.getChanges();
            final var domEntityOwnershipChange = listener.getChanges().get(changes.size() - 1);
            assertEquals(entity, domEntityOwnershipChange.entity());

            assertEquals(hasOwner, domEntityOwnershipChange.change().hasOwner());
            assertEquals(isOwner, domEntityOwnershipChange.change().isOwner());
            assertEquals(wasOwner, domEntityOwnershipChange.change().wasOwner());
        });
    }

    protected static void verifyNoNotifications(final MockEntityOwnershipListener listener) {
        verifyNoNotifications(listener, 2);
    }

    protected static void verifyNoNotifications(final MockEntityOwnershipListener listener, final long delaySeconds) {
        await().pollDelay(delaySeconds, TimeUnit.SECONDS).until(() -> listener.getChanges().isEmpty());
    }

    protected static void verifyNoAdditionalNotifications(
            final MockEntityOwnershipListener listener, final long delaySeconds) {
        listener.resetListener();
        verifyNoNotifications(listener, delaySeconds);
    }

    protected static final class ClusterNode {
        private final int port;
        private final List<String> roles;
        private final org.apache.pekko.actor.typed.ActorSystem<Void> actorSystem;
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

        public org.apache.pekko.actor.typed.ActorSystem<Void> getActorSystem() {
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
        private final List<EntityOwnerChanged> changes = new ArrayList<>();
        private final String member;
        private final Logger log;

        public MockEntityOwnershipListener(final String member) {
            log = LoggerFactory.getLogger("EOS-listener-" + member);
            this.member = member;
        }

        @Override
        public void ownershipChanged(final DOMEntity entity, final EntityOwnershipStateChange change,
                final boolean inJeopardy) {
            final var changed = new EntityOwnerChanged(entity, change, inJeopardy);
            log.info("{} Received ownershipCHanged: {}", member, changed);
            log.info("{} changes: {}", member, changes.size());
            changes.add(changed);
        }

        public List<EntityOwnerChanged> getChanges() {
            return changes;
        }

        public void resetListener() {
            changes.clear();
        }
    }

    protected static final class MockNativeEntityOwnershipService extends AkkaEntityOwnershipService {
        private final ActorSystem classicActorSystem;

        protected MockNativeEntityOwnershipService(final ActorSystem classicActorSystem)
                throws ExecutionException, InterruptedException {
            super(classicActorSystem, CODEC_CONTEXT);
            this.classicActorSystem = classicActorSystem;
        }

        protected void reachableMember(final String... role) {
            AbstractNativeEosTest.reachableMember(ownerSupervisor, role);
        }

        public void unreachableMember(final String... role) {
            AbstractNativeEosTest.unreachableMember(ownerSupervisor, role);
        }

        public ActorSystem getActorSystem() {
            return classicActorSystem;
        }
    }
}
