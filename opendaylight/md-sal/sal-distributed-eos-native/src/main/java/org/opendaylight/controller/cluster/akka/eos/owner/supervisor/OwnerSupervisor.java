/*
 * Copyright (c) 2021 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.akka.eos.owner.supervisor;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;
import akka.cluster.ClusterEvent;
import akka.cluster.Member;
import akka.cluster.ddata.LWWRegister;
import akka.cluster.ddata.LWWRegisterKey;
import akka.cluster.ddata.ORMap;
import akka.cluster.ddata.ORSet;
import akka.cluster.ddata.SelfUniqueAddress;
import akka.cluster.ddata.typed.javadsl.DistributedData;
import akka.cluster.ddata.typed.javadsl.Replicator;
import akka.cluster.ddata.typed.javadsl.ReplicatorMessageAdapter;
import akka.cluster.typed.Cluster;
import akka.cluster.typed.Subscribe;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.opendaylight.controller.cluster.akka.eos.owner.supervisor.command.CandidatesChanged;
import org.opendaylight.controller.cluster.akka.eos.owner.supervisor.command.MemberDownEvent;
import org.opendaylight.controller.cluster.akka.eos.owner.supervisor.command.MemberReachableEvent;
import org.opendaylight.controller.cluster.akka.eos.owner.supervisor.command.MemberUnreachableEvent;
import org.opendaylight.controller.cluster.akka.eos.owner.supervisor.command.MemberUpEvent;
import org.opendaylight.controller.cluster.akka.eos.owner.supervisor.command.OwnerChanged;
import org.opendaylight.controller.cluster.akka.eos.owner.supervisor.command.OwnerSupervisorCommand;
import org.opendaylight.controller.cluster.akka.eos.registry.candidate.CandidateRegistry;
import org.opendaylight.mdsal.eos.dom.api.DOMEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.collection.JavaConverters;

/**
 * Responsible for tracking candidates and assigning ownership of entities. This behavior is subscribed to the candidate
 * registry in distributed-data and picks entity owners based on the current cluster state and registered candidates.
 * On cluster up/down etc. events the owners are reassigned if possible.
 */
public final class OwnerSupervisor extends AbstractBehavior<OwnerSupervisorCommand> {

    private static final Logger LOG = LoggerFactory.getLogger(OwnerSupervisor.class);

    private final ReplicatorMessageAdapter<OwnerSupervisorCommand, LWWRegister<String>> ownerReplicator;

    private final Cluster cluster;
    private final SelfUniqueAddress node;

    private final Set<String> activeMembers;

    // currently registered candidates
    private final Map<DOMEntity, Set<String>> currentCandidates;
    // current owners
    private final Map<DOMEntity, String> currentOwners;
    // reverse lookup of owner to entity
    private final Multimap<String, DOMEntity> ownerToEntity = HashMultimap.create();

    private OwnerSupervisor(final ActorContext<OwnerSupervisorCommand> context,
                            final ReplicatorMessageAdapter<OwnerSupervisorCommand,
                                    ORMap<DOMEntity, ORSet<String>>> candidateReplicator,
                            final ReplicatorMessageAdapter<OwnerSupervisorCommand,
                                    LWWRegister<String>> ownerReplicator,
                            final Map<DOMEntity, Set<String>> currentCandidates,
                            final Map<DOMEntity, String> currentOwners) {
        super(context);
        this.cluster = Cluster.get(context.getSystem());
        this.ownerReplicator = ownerReplicator;

        this.node = DistributedData.get(context.getSystem()).selfUniqueAddress();
        this.activeMembers = getActiveMembers(cluster);

        this.currentCandidates = currentCandidates;
        this.currentOwners = currentOwners;

        for (final Map.Entry<DOMEntity, String> entry : currentOwners.entrySet()) {
            ownerToEntity.put(entry.getValue(), entry.getKey());
        }

        // check whether we have any unreachable/missing owners
        reassignUnreachableOwners();
        assignMissingOwners();

        final ActorRef<ClusterEvent.MemberEvent> memberEventAdapter =
                context.messageAdapter(ClusterEvent.MemberEvent.class, event -> {
                    if (event instanceof ClusterEvent.MemberUp) {
                        return new MemberUpEvent(event.member().address(), event.member().getRoles());
                    } else {
                        return new MemberDownEvent(event.member().address(), event.member().getRoles());
                    }
                });
        cluster.subscriptions().tell(Subscribe.create(memberEventAdapter, ClusterEvent.MemberEvent.class));

        final ActorRef<ClusterEvent.ReachabilityEvent> reachabilityEventAdapter =
                context.messageAdapter(ClusterEvent.ReachabilityEvent.class, event -> {
                    if (event instanceof ClusterEvent.ReachableMember) {
                        return new MemberReachableEvent(event.member().address(), event.member().getRoles());
                    } else {
                        return new MemberUnreachableEvent(event.member().address(), event.member().getRoles());
                    }
                });
        cluster.subscriptions().tell(Subscribe.create(reachabilityEventAdapter, ClusterEvent.ReachabilityEvent.class));

        candidateReplicator.subscribe(CandidateRegistry.KEY, CandidatesChanged::new);

        LOG.debug("Owner Supervisor started");
    }

    public static Behavior<OwnerSupervisorCommand> create(final Map<DOMEntity, Set<String>> currentCandidates,
                                                          final Map<DOMEntity, String> currentOwners) {
        return Behaviors.setup(ctx -> {
                final ActorRef<Replicator.Command> replicator = DistributedData.get(ctx.getSystem()).replicator();

                final ReplicatorMessageAdapter<OwnerSupervisorCommand,
                        ORMap<DOMEntity, ORSet<String>>> candidateReplicator =
                                new ReplicatorMessageAdapter<>(ctx, replicator, Duration.ofSeconds(5));

                final ReplicatorMessageAdapter<OwnerSupervisorCommand, LWWRegister<String>> ownerReplicator =
                        new ReplicatorMessageAdapter<>(ctx, replicator, Duration.ofSeconds(5));

                return new OwnerSupervisor(ctx, candidateReplicator, ownerReplicator,
                        currentCandidates, currentOwners);
            }
        );
    }

    @Override
    public Receive<OwnerSupervisorCommand> createReceive() {
        return newReceiveBuilder()
                .onMessage(CandidatesChanged.class, this::onCandidatesChanged)
                .onMessage(MemberUpEvent.class, this::onPeerUp)
                .onMessage(MemberDownEvent.class, this::onPeerDown)
                .onMessage(MemberReachableEvent.class, this::onPeerReachable)
                .onMessage(MemberUnreachableEvent.class, this::onPeerUnreachable)
                .build();
    }

    private void reassignUnreachableOwners() {
        final Set<String> ownersToReassign = new HashSet<>();
        for (final String owner : ownerToEntity.keys()) {
            if (!activeMembers.contains(owner)) {
                ownersToReassign.add(owner);
            }
        }

        for (final String owner : ownersToReassign) {
            reassignCandidatesFor(owner, ImmutableList.copyOf(ownerToEntity.get(owner)));
        }
    }

    private void assignMissingOwners() {
        for (final Map.Entry<DOMEntity, Set<String>> entry : currentCandidates.entrySet()) {
            if (!currentOwners.containsKey(entry.getKey())) {
                assignOwnerFor(entry.getKey());
            }
        }
    }

    private Behavior<OwnerSupervisorCommand> onCandidatesChanged(final CandidatesChanged message) {
        LOG.debug("onCandidatesChanged {}", message.getResponse());
        if (message.getResponse() instanceof Replicator.Changed) {
            final Replicator.Changed<ORMap<DOMEntity, ORSet<String>>> changed =
                    (Replicator.Changed<ORMap<DOMEntity, ORSet<String>>>) message.getResponse();
            processCandidateChanges(changed.get(CandidateRegistry.KEY));
        }
        return this;
    }

    private void processCandidateChanges(final ORMap<DOMEntity, ORSet<String>> candidates) {
        final Map<DOMEntity, ORSet<String>> entries = candidates.getEntries();
        for (final Map.Entry<DOMEntity, ORSet<String>> entry : entries.entrySet()) {
            processCandidatesFor(entry.getKey(), entry.getValue());
        }
    }

    private void processCandidatesFor(final DOMEntity entity, final ORSet<String> receivedCandidates) {
        LOG.debug("Processing candidates for : {}, new value: {}", entity, receivedCandidates.elements());

        final Set<String> candidates = JavaConverters.asJava(receivedCandidates.elements());
        // only insert candidates if there are any to insert, otherwise we would generate unnecessary notification with
        // no owner
        if (!currentCandidates.containsKey(entity) && !candidates.isEmpty()) {
            LOG.debug("Candidates missing for entity: {} adding all candidates", entity);
            currentCandidates.put(entity, new HashSet<>(candidates));

            LOG.debug("Current state for {} : {}", entity, currentCandidates.get(entity).toString());
            assignOwnerFor(entity);

            return;
        }

        final Set<String> currentlyPresent = currentCandidates.getOrDefault(entity, Collections.emptySet());
        final Set<String> difference = ImmutableSet.copyOf(Sets.symmetricDifference(currentlyPresent, candidates));

        LOG.debug("currently present candidates: {}", currentlyPresent);
        LOG.debug("difference: {}", difference);

        final List<String> ownersToReassign = new ArrayList<>();

        // first add/remove candidates from entities
        for (final String toCheck : difference) {
            if (!currentlyPresent.contains(toCheck)) {
                // add new candidate
                LOG.debug("Adding new candidate for entity: {} : {}", entity, toCheck);
                currentCandidates.get(entity).add(toCheck);

                if (!currentOwners.containsKey(entity)) {
                    // might as well assign right away when we don't have an owner
                    assignOwnerFor(entity);
                }

                LOG.debug("Current state for entity: {} : {}", entity, currentCandidates.get(entity).toString());
                continue;
            }

            if (!candidates.contains(toCheck)) {
                // remove candidate
                LOG.debug("Removing candidate from entity: {} - {}", entity, toCheck);
                currentCandidates.get(entity).remove(toCheck);
                if (ownerToEntity.containsKey(toCheck)) {
                    ownersToReassign.add(toCheck);
                }
            }
        }

        // then reassign those that need new owners
        for (final String toReassign : ownersToReassign) {
            reassignCandidatesFor(toReassign, ImmutableList.copyOf(ownerToEntity.get(toReassign)));
        }

        if (currentCandidates.get(entity) == null) {
            LOG.debug("Last candidate removed for {}", entity);
        } else {
            LOG.debug("Current state for entity: {} : {}", entity, currentCandidates.get(entity).toString());
        }
    }

    private void reassignCandidatesFor(final String oldOwner, final Collection<DOMEntity> entities) {
        LOG.debug("Reassigning owners for {}", entities);
        for (final DOMEntity entity : entities) {

            // only reassign owner for those entities that lost this candidate or is not reachable
            if (!activeMembers.contains(oldOwner)
                    || !currentCandidates.getOrDefault(entity, Collections.emptySet()).contains(oldOwner)) {
                ownerToEntity.remove(oldOwner, entity);
                assignOwnerFor(entity);
            }
        }
    }

    private void assignOwnerFor(final DOMEntity entity) {
        final Set<String> candidatesForEntity = currentCandidates.get(entity);
        if (candidatesForEntity.isEmpty()) {
            LOG.debug("No candidates present for entity: {}", entity);
            removeOwner(entity);
            return;
        }

        String pickedCandidate = null;
        for (final String candidate : candidatesForEntity) {
            if (activeMembers.contains(candidate)) {
                pickedCandidate = candidate;
                break;
            }
        }
        if (pickedCandidate == null) {
            LOG.debug("No candidate is reachable for {}, activeMembers: {}, currentCandidates: {}",
                    entity, activeMembers, currentCandidates.get(entity));
            // no candidate is reachable so only remove owner if necessary
            removeOwner(entity);
            return;
        }
        ownerToEntity.put(pickedCandidate, entity);

        LOG.debug("Entity {} new owner: {}", entity, pickedCandidate);
        currentOwners.put(entity, pickedCandidate);
        writeNewOwner(entity, pickedCandidate);
    }

    private void removeOwner(final DOMEntity entity) {
        if (currentOwners.containsKey(entity)) {
            // assign empty owner to dd, as we cannot delete data for a key since that would prevent
            // writes for the same key
            currentOwners.remove(entity);

            writeNewOwner(entity, "");
        }
    }

    private void writeNewOwner(final DOMEntity entity, final String candidate) {
        ownerReplicator.askUpdate(
                askReplyTo -> new Replicator.Update<>(
                        new LWWRegisterKey<>(entity.toString()),
                        new LWWRegister<>(node.uniqueAddress(), candidate, System.currentTimeMillis()),
                        Replicator.writeLocal(),
                        askReplyTo,
                        register -> register.withValue(node, candidate)),
                OwnerChanged::new);
    }

    private Behavior<OwnerSupervisorCommand> onPeerUp(final MemberUpEvent event) {
        LOG.debug("Received MemberUp : {}", event);

        handleReachableEvent(event.getRoles());
        return this;
    }

    private Behavior<OwnerSupervisorCommand> onPeerReachable(final MemberReachableEvent event) {
        LOG.debug("Received MemberReachable : {}", event);

        handleReachableEvent(event.getRoles());
        return this;
    }

    private void handleReachableEvent(final Set<String> roles) {
        activeMembers.add(extractRole(roles));
        assignMissingOwners();
    }

    private Behavior<OwnerSupervisorCommand> onPeerDown(final MemberDownEvent event) {
        LOG.debug("Received MemberDown : {}", event);

        handleUnreachableEvent(event.getRoles());
        return this;
    }

    private Behavior<OwnerSupervisorCommand> onPeerUnreachable(final MemberUnreachableEvent event) {
        LOG.debug("Received MemberUnreachable : {}", event);

        handleUnreachableEvent(event.getRoles());
        return this;
    }

    private void handleUnreachableEvent(final Set<String> roles) {
        activeMembers.remove(extractRole(roles));
        reassignUnreachableOwners();
    }

    private static Set<String> getActiveMembers(final Cluster cluster) {
        final Set<String> activeMembers = new HashSet<>();
        cluster.state().getMembers().forEach(member -> activeMembers.add(extractRole(member)));
        activeMembers.removeAll(cluster.state().getUnreachable().stream()
                .map(OwnerSupervisor::extractRole).collect(Collectors.toSet()));

        return activeMembers;
    }

    private static String extractRole(final Member member) {
        return extractRole(member.getRoles());
    }

    private static String extractRole(final Set<String> roles) {
        final Set<String> filtered = roles.stream().filter(role -> !role.contains("dc")).collect(Collectors.toSet());
        return filtered.iterator().next();
    }
}
