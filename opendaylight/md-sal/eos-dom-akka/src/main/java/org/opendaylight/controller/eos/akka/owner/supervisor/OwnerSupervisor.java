/*
 * Copyright (c) 2021 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.eos.akka.owner.supervisor;

import static com.google.common.base.Verify.verifyNotNull;
import static java.util.Objects.requireNonNull;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiPredicate;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import org.apache.pekko.actor.typed.Behavior;
import org.apache.pekko.actor.typed.javadsl.ActorContext;
import org.apache.pekko.actor.typed.javadsl.Behaviors;
import org.apache.pekko.actor.typed.javadsl.Receive;
import org.apache.pekko.cluster.ClusterEvent;
import org.apache.pekko.cluster.ClusterEvent.MemberEvent;
import org.apache.pekko.cluster.ClusterEvent.ReachabilityEvent;
import org.apache.pekko.cluster.Member;
import org.apache.pekko.cluster.ddata.LWWRegister;
import org.apache.pekko.cluster.ddata.LWWRegisterKey;
import org.apache.pekko.cluster.ddata.ORMap;
import org.apache.pekko.cluster.ddata.ORSet;
import org.apache.pekko.cluster.ddata.SelfUniqueAddress;
import org.apache.pekko.cluster.ddata.typed.javadsl.DistributedData;
import org.apache.pekko.cluster.ddata.typed.javadsl.Replicator;
import org.apache.pekko.cluster.ddata.typed.javadsl.ReplicatorMessageAdapter;
import org.apache.pekko.cluster.typed.Cluster;
import org.apache.pekko.cluster.typed.Subscribe;
import org.apache.pekko.pattern.StatusReply;
import org.opendaylight.controller.eos.akka.owner.supervisor.command.AbstractEntityRequest;
import org.opendaylight.controller.eos.akka.owner.supervisor.command.CandidatesChanged;
import org.opendaylight.controller.eos.akka.owner.supervisor.command.ClearCandidates;
import org.opendaylight.controller.eos.akka.owner.supervisor.command.ClearCandidatesForMember;
import org.opendaylight.controller.eos.akka.owner.supervisor.command.DataCenterDeactivated;
import org.opendaylight.controller.eos.akka.owner.supervisor.command.DeactivateDataCenter;
import org.opendaylight.controller.eos.akka.owner.supervisor.command.GetEntitiesBackendReply;
import org.opendaylight.controller.eos.akka.owner.supervisor.command.GetEntitiesBackendRequest;
import org.opendaylight.controller.eos.akka.owner.supervisor.command.GetEntityBackendReply;
import org.opendaylight.controller.eos.akka.owner.supervisor.command.GetEntityBackendRequest;
import org.opendaylight.controller.eos.akka.owner.supervisor.command.GetEntityOwnerBackendReply;
import org.opendaylight.controller.eos.akka.owner.supervisor.command.GetEntityOwnerBackendRequest;
import org.opendaylight.controller.eos.akka.owner.supervisor.command.MemberDownEvent;
import org.opendaylight.controller.eos.akka.owner.supervisor.command.MemberReachableEvent;
import org.opendaylight.controller.eos.akka.owner.supervisor.command.MemberUnreachableEvent;
import org.opendaylight.controller.eos.akka.owner.supervisor.command.MemberUpEvent;
import org.opendaylight.controller.eos.akka.owner.supervisor.command.OwnerChanged;
import org.opendaylight.controller.eos.akka.owner.supervisor.command.OwnerSupervisorCommand;
import org.opendaylight.controller.eos.akka.registry.candidate.CandidateRegistry;
import org.opendaylight.mdsal.eos.dom.api.DOMEntity;
import org.opendaylight.yangtools.binding.data.codec.api.BindingInstanceIdentifierCodec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.jdk.javaapi.CollectionConverters;

/**
 * Responsible for tracking candidates and assigning ownership of entities. This behavior is subscribed to the candidate
 * registry in distributed-data and picks entity owners based on the current cluster state and registered candidates.
 * On cluster up/down etc. events the owners are reassigned if possible.
 */
public final class OwnerSupervisor extends AbstractSupervisor {

    private static final Logger LOG = LoggerFactory.getLogger(OwnerSupervisor.class);
    private static final String DATACENTER_PREFIX = "dc-";

    private final ReplicatorMessageAdapter<OwnerSupervisorCommand, LWWRegister<String>> ownerReplicator;

    // Our own clock implementation so we do not have to rely on synchronized clocks. This basically functions as an
    // increasing counter which is fine for our needs as we only ever have a single writer since t supervisor is
    // running in a cluster-singleton
    private static final LWWRegister.Clock<String> CLOCK = (currentTimestamp, value) -> currentTimestamp + 1;

    private final Cluster cluster;
    private final SelfUniqueAddress node;
    private final String dataCenter;

    private final Set<String> activeMembers;

    // currently registered candidates
    private final Map<DOMEntity, Set<String>> currentCandidates;
    // current owners
    private final Map<DOMEntity, String> currentOwners;
    // reverse lookup of owner to entity
    private final HashMultimap<String, DOMEntity> ownerToEntity = HashMultimap.create();

    // only reassign owner for those entities that lost this candidate or is not reachable
    private final BiPredicate<DOMEntity, String> reassignPredicate = (entity, candidate) ->
            !isActiveCandidate(candidate) || !isCandidateFor(entity, candidate);

    private final BindingInstanceIdentifierCodec iidCodec;

    private OwnerSupervisor(final ActorContext<OwnerSupervisorCommand> context,
                            final Map<DOMEntity, Set<String>> currentCandidates,
                            final Map<DOMEntity, String> currentOwners,
                            final BindingInstanceIdentifierCodec iidCodec) {
        super(context);
        this.iidCodec = requireNonNull(iidCodec);

        final var distributedData = DistributedData.get(context.getSystem());
        final var replicator = distributedData.replicator();

        cluster = Cluster.get(context.getSystem());
        ownerReplicator = new ReplicatorMessageAdapter<>(context, replicator, Duration.ofSeconds(5));
        dataCenter = extractDatacenterRole(cluster.selfMember());

        node = distributedData.selfUniqueAddress();
        activeMembers = getActiveMembers();

        this.currentCandidates = currentCandidates;
        this.currentOwners = currentOwners;

        for (var entry : currentOwners.entrySet()) {
            ownerToEntity.put(entry.getValue(), entry.getKey());
        }

        // check whether we have any unreachable/missing owners
        reassignUnreachableOwners();
        assignMissingOwners();

        cluster.subscriptions().tell(Subscribe.create(context.messageAdapter(MemberEvent.class, event -> {
            if (event instanceof ClusterEvent.MemberUp) {
                return new MemberUpEvent(event.member().address(), event.member().getRoles());
            } else {
                return new MemberDownEvent(event.member().address(), event.member().getRoles());
            }
        }), MemberEvent.class));

        cluster.subscriptions().tell(Subscribe.create(context.messageAdapter(ReachabilityEvent.class, event -> {
            if (event instanceof ClusterEvent.ReachableMember) {
                return new MemberReachableEvent(event.member().address(), event.member().getRoles());
            } else {
                return new MemberUnreachableEvent(event.member().address(), event.member().getRoles());
            }
        }), ReachabilityEvent.class));

        candidateReplicator.subscribe(CandidateRegistry.KEY, CandidatesChanged::new);

        LOG.debug("Owner Supervisor started");
    }

    public static Behavior<OwnerSupervisorCommand> create(final Map<DOMEntity, Set<String>> currentCandidates,
            final Map<DOMEntity, String> currentOwners, final BindingInstanceIdentifierCodec iidCodec) {
        return Behaviors.setup(ctx -> new OwnerSupervisor(ctx, currentCandidates, currentOwners, iidCodec));
    }

    @Override
    public Receive<OwnerSupervisorCommand> createReceive() {
        return newReceiveBuilder()
                .onMessage(CandidatesChanged.class, this::onCandidatesChanged)
                .onMessage(DeactivateDataCenter.class, this::onDeactivateDatacenter)
                .onMessage(OwnerChanged.class, this::onOwnerChanged)
                .onMessage(MemberUpEvent.class, this::onPeerUp)
                .onMessage(MemberDownEvent.class, this::onPeerDown)
                .onMessage(MemberReachableEvent.class, this::onPeerReachable)
                .onMessage(MemberUnreachableEvent.class, this::onPeerUnreachable)
                .onMessage(GetEntitiesBackendRequest.class, this::onGetEntities)
                .onMessage(GetEntityBackendRequest.class, this::onGetEntity)
                .onMessage(GetEntityOwnerBackendRequest.class, this::onGetEntityOwner)
                .onMessage(ClearCandidatesForMember.class, this::onClearCandidatesForMember)
                .onMessage(ClearCandidates.class, this::finishClearCandidates)
                .build();
    }

    private Behavior<OwnerSupervisorCommand> onDeactivateDatacenter(final DeactivateDataCenter command) {
        LOG.debug("Deactivating Owner Supervisor on {}", cluster.selfMember());
        command.getReplyTo().tell(DataCenterDeactivated.INSTANCE);
        return IdleSupervisor.create(iidCodec);
    }

    private Behavior<OwnerSupervisorCommand> onOwnerChanged(final OwnerChanged command) {
        LOG.debug("Owner has changed for {}", command.getResponse().key());
        return this;
    }

    private void reassignUnreachableOwners() {
        final var ownersToReassign = new HashSet<String>();
        for (var owner : ownerToEntity.keys()) {
            if (!isActiveCandidate(owner)) {
                ownersToReassign.add(owner);
            }
        }

        for (var owner : ownersToReassign) {
            reassignCandidatesFor(owner, ImmutableList.copyOf(ownerToEntity.get(owner)), reassignPredicate);
        }
    }

    private void assignMissingOwners() {
        for (var entity : currentCandidates.keySet()) {
            if (!currentOwners.containsKey(entity)) {
                assignOwnerFor(entity);
            }
        }
    }

    private Behavior<OwnerSupervisorCommand> onCandidatesChanged(final CandidatesChanged message) {
        LOG.debug("onCandidatesChanged {}", message.getResponse());
        if (message.getResponse() instanceof Replicator.Changed<ORMap<DOMEntity, ORSet<String>>> changed) {
            processCandidateChanges(changed.get(CandidateRegistry.KEY));
        }
        return this;
    }

    private void processCandidateChanges(final ORMap<DOMEntity, ORSet<String>> candidates) {
        for (var entry : candidates.getEntries().entrySet()) {
            processCandidatesFor(entry.getKey(), entry.getValue());
        }
    }

    private void processCandidatesFor(final DOMEntity entity, final ORSet<String> receivedCandidates) {
        LOG.debug("Processing candidates for : {}, new value: {}", entity, receivedCandidates.elements());

        final var candidates = CollectionConverters.asJava(receivedCandidates.elements());
        // only insert candidates if there are any to insert, otherwise we would generate unnecessary notification with
        // no owner
        if (!currentCandidates.containsKey(entity) && !candidates.isEmpty()) {
            LOG.debug("Candidates missing for entity: {} adding all candidates", entity);
            currentCandidates.put(entity, new HashSet<>(candidates));

            LOG.debug("Current state for {} : {}", entity, currentCandidates.get(entity).toString());
            assignOwnerFor(entity);

            return;
        }

        final var currentlyPresent = currentCandidates.getOrDefault(entity, Set.of());
        final var difference = ImmutableSet.copyOf(Sets.symmetricDifference(currentlyPresent, candidates));

        LOG.debug("currently present candidates: {}", currentlyPresent);
        LOG.debug("difference: {}", difference);

        final List<String> ownersToReassign = new ArrayList<>();

        // first add/remove candidates from entities
        for (var toCheck : difference) {
            if (!currentlyPresent.contains(toCheck)) {
                // add new candidate
                LOG.debug("Adding new candidate for entity: {} : {}", entity, toCheck);
                currentCandidates.get(entity).add(toCheck);

                final var currentOwner = currentOwners.get(entity);
                if (currentOwner == null || !activeMembers.contains(currentOwner)) {
                    // might as well assign right away when we don't have an owner or its unreachable
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
        for (var toReassign : ownersToReassign) {
            reassignCandidatesFor(toReassign, ImmutableList.copyOf(ownerToEntity.get(toReassign)), reassignPredicate);
        }

        if (currentCandidates.get(entity) == null) {
            LOG.debug("Last candidate removed for {}", entity);
        } else {
            LOG.debug("Current state for entity: {} : {}", entity, currentCandidates.get(entity).toString());
        }
    }

    private void reassignCandidatesFor(final String oldOwner, final Collection<DOMEntity> entities,
                                       final BiPredicate<DOMEntity, String> predicate) {
        LOG.debug("Reassigning owners for {}", entities);
        for (var entity : entities) {
            if (predicate.test(entity, oldOwner)) {
                if (!isActiveCandidate(oldOwner) && isCandidateFor(entity, oldOwner) && hasSingleCandidate(entity)) {
                    // only skip new owner assignment, only if unreachable, still is a candidate and is the ONLY
                    // candidate
                    LOG.debug("{} is the only candidate for {}. Skipping reassignment.", oldOwner, entity);
                    continue;
                }
                ownerToEntity.remove(oldOwner, entity);
                assignOwnerFor(entity);
            }
        }
    }

    private boolean isActiveCandidate(final String candidate) {
        return activeMembers.contains(candidate);
    }

    private boolean isCandidateFor(final DOMEntity entity, final String candidate) {
        return currentCandidates.getOrDefault(entity, Set.of()).contains(candidate);
    }

    private boolean hasSingleCandidate(final DOMEntity entity) {
        return currentCandidates.getOrDefault(entity, Set.of()).size() == 1;
    }

    private void assignOwnerFor(final DOMEntity entity) {
        final var candidatesForEntity = currentCandidates.get(entity);
        if (candidatesForEntity.isEmpty()) {
            LOG.debug("No candidates present for entity: {}", entity);
            removeOwner(entity);
            return;
        }

        String pickedCandidate = null;
        for (var candidate : candidatesForEntity) {
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
                        new LWWRegister<>(node.uniqueAddress(), candidate, 0),
                        Replicator.writeLocal(),
                        askReplyTo,
                        register -> register.withValue(node, candidate, CLOCK)),
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

    private Behavior<OwnerSupervisorCommand> onGetEntities(final GetEntitiesBackendRequest request) {
        request.getReplyTo().tell(StatusReply.success(new GetEntitiesBackendReply(currentOwners, currentCandidates)));
        return this;
    }

    private Behavior<OwnerSupervisorCommand> onGetEntity(final GetEntityBackendRequest request) {
        final var entity = extractEntity(request);
        request.getReplyTo().tell(StatusReply.success(
                new GetEntityBackendReply(currentOwners.get(entity), currentCandidates.get(entity))));
        return this;
    }

    private Behavior<OwnerSupervisorCommand> onGetEntityOwner(final GetEntityOwnerBackendRequest request) {
        request.getReplyTo().tell(
                StatusReply.success(new GetEntityOwnerBackendReply(currentOwners.get(extractEntity(request)))));
        return this;
    }

    private void handleReachableEvent(final Set<String> roles) {
        if (roles.contains(dataCenter)) {
            activeMembers.add(extractRole(roles));
            assignMissingOwners();
        } else {
            LOG.debug("Received reachable event from a foreign datacenter, Ignoring... Roles: {}", roles);
        }
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
        if (roles.contains(dataCenter)) {
            activeMembers.remove(extractRole(roles));
            reassignUnreachableOwners();
        } else {
            LOG.debug("Received unreachable event from a foreign datacenter, Ignoring... Roles: {}", roles);
        }
    }

    private Set<String> getActiveMembers() {
        final var clusterState = cluster.state();
        final var unreachableRoles = clusterState.getUnreachable().stream()
            .map(OwnerSupervisor::extractRole)
            .collect(Collectors.toSet());

        return StreamSupport.stream(clusterState.getMembers().spliterator(), false)
            // We are evaluating the set of roles for each member
            .map(Member::getRoles)
            // Filter out any members which do not share our dataCenter
            .filter(roles -> roles.contains(dataCenter))
            // Find first legal role
            .map(OwnerSupervisor::extractRole)
            // filter out unreachable roles
            .filter(role -> !unreachableRoles.contains(role))
            .collect(Collectors.toSet());
    }

    private DOMEntity extractEntity(final AbstractEntityRequest<?> request) {
        final var name = request.getName();
        final var iid = name.getInstanceIdentifier();
        if (iid != null) {
            return new DOMEntity(request.getType().getValue(), iidCodec.fromBinding(iid));
        }
        final var str = verifyNotNull(name.getString(), "Unhandled entity name %s", name);
        return new DOMEntity(request.getType().getValue(), str);
    }

    private static String extractRole(final Member member) {
        return extractRole(member.getRoles());
    }

    private static String extractRole(final Set<String> roles) {
        return roles.stream().filter(role -> !role.startsWith(DATACENTER_PREFIX))
                .findFirst().orElseThrow(() -> new IllegalArgumentException("No valid role found."));
    }

    private static String extractDatacenterRole(final Member member) {
        return member.getRoles().stream().filter(role -> role.startsWith(DATACENTER_PREFIX))
                .findFirst().orElseThrow(() -> new IllegalArgumentException("No valid role found."));
    }

    @Override
    Logger getLogger() {
        return LOG;
    }
}
