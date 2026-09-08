/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore;

import static java.util.Objects.requireNonNull;

import com.google.common.collect.ArrayListMultimap;
import java.time.Duration;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.CompletionStage;
import java.util.stream.Collectors;
import org.apache.pekko.actor.ActorRef;
import org.apache.pekko.actor.PoisonPill;
import org.apache.pekko.actor.Status;
import org.apache.pekko.pattern.Patterns;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.opendaylight.controller.cluster.access.concepts.TransactionIdentifier;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.mdsal.dom.api.DOMDataTreeCandidate;
import org.opendaylight.mdsal.dom.api.DOMDataTreeIdentifier;
import org.opendaylight.mdsal.dom.spi.AbstractRegistrationTree;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.PathArgument;
import org.opendaylight.yangtools.yang.data.tree.api.DataTreeCandidate;
import org.opendaylight.yangtools.yang.data.tree.api.DataTreeCandidateNode;
import org.opendaylight.yangtools.yang.data.tree.api.ModificationType;
import org.opendaylight.yangtools.yang.model.api.EffectiveModelContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Registry of user commit cohorts, which is responsible for handling registration and calculation
 * of affected cohorts based on {@link DataTreeCandidate}. This class is NOT thread-safe.
 */
final class DataTreeCohortActorRegistry extends AbstractRegistrationTree<ActorRef> {
    sealed interface Command {
        // nothing else
    }

    @NonNullByDefault
    private record RegisterActor(
            ActorRef replyTo,
            DOMDataTreeIdentifier subtree,
            ActorRef cohortActor) implements Command {
        RegisterActor {
            requireNonNull(replyTo);
            requireNonNull(subtree);
            requireNonNull(cohortActor);
        }
    }

    @NonNullByDefault
    private record UnregisterActor(ActorRef cohortActor) implements Command {
        UnregisterActor {
            requireNonNull(cohortActor);
        }
    }

    private static final Logger LOG = LoggerFactory.getLogger(DataTreeCohortActorRegistry.class);
    // FIXME: hard-coded
    private static final Duration REGISTER_ASK_TIMEOUT = Duration.ofSeconds(5);

    private final HashMap<ActorRef, Node<ActorRef>> cohortToNode = new HashMap<>();

    List<ActorRef> getCohortActors() {
        return List.copyOf(cohortToNode.keySet());
    }

    List<DataTreeCohortActor.CanCommit> createCanCommitMessages(final TransactionIdentifier txId,
            final DataTreeCandidate candidate, final EffectiveModelContext schema) {
        try (var cohorts = takeSnapshot()) {
            return new CanCommitMessageBuilder(txId, candidate, schema).perform(cohorts.getRootNode());
        }
    }

    void process(final @NonNull Command message) {
        switch (message) {
            case RegisterActor command -> registerActor(command);
            case UnregisterActor command -> removeActor(command);
        }
    }

    @NonNullByDefault
    static CompletionStage<?> registerActor(final ActorRef registryActor, final DOMDataTreeIdentifier subtree,
            final ActorRef cohortActor) {
        // TODO: can we make the timeout part just a retry?
        return Patterns.askWithReplyTo(cohortActor, replyTo -> new RegisterActor(replyTo, subtree, cohortActor),
            REGISTER_ASK_TIMEOUT);
    }

    @SuppressWarnings("checkstyle:IllegalCatch")
    private void registerActor(final RegisterActor command) {
        takeLock();
        try {
            final var cohortActor = command.cohortActor;
            final var node = findNodeFor(command.subtree.path().getPathArguments());
            addRegistration(node, cohortActor);
            cohortToNode.put(cohortActor, node);
        } catch (Exception e) {
            command.replyTo.tell(new Status.Failure(e), ActorRef.noSender());
            return;
        } finally {
            releaseLock();
        }
        command.replyTo.tell(new Status.Success(null), ActorRef.noSender());
    }

    @NonNullByDefault
    static void unregisterActor(final ActorRef registryActor, final ActorRef cohortActor) {
        registryActor.tell(new UnregisterActor(cohortActor), ActorRef.noSender());
    }

    private void removeActor(final UnregisterActor command) {
        final var cohortActor = command.cohortActor;
        final var node = cohortToNode.get(cohortActor);
        if (node != null) {
            removeRegistration(node, cohortActor);
            cohortToNode.remove(cohortActor);
        }
        cohortActor.tell(PoisonPill.getInstance(), cohortActor);
    }

    private static final class CanCommitMessageBuilder {
        private final ArrayListMultimap<ActorRef, DOMDataTreeCandidate> actorToCandidates = ArrayListMultimap.create();
        private final @NonNull TransactionIdentifier txId;
        private final @NonNull DataTreeCandidate candidate;
        private final EffectiveModelContext schema;

        CanCommitMessageBuilder(final TransactionIdentifier txId, final DataTreeCandidate candidate,
                final EffectiveModelContext schema) {
            this.txId = requireNonNull(txId);
            this.candidate = requireNonNull(candidate);
            this.schema = schema;
        }

        private void lookupAndCreateCanCommits(final List<PathArgument> args, final int offset,
                final Node<ActorRef> node) {

            if (args.size() != offset) {
                final PathArgument arg = args.get(offset);
                final var exactChild = node.getExactChild(arg);
                if (exactChild != null) {
                    lookupAndCreateCanCommits(args, offset + 1, exactChild);
                }
                for (var inexact : node.getInexactChildren(arg)) {
                    lookupAndCreateCanCommits(args, offset + 1, inexact);
                }
            } else {
                lookupAndCreateCanCommits(candidate.getRootPath(), node, candidate.getRootNode());
            }
        }

        private void lookupAndCreateCanCommits(final YangInstanceIdentifier path, final Node<ActorRef> regNode,
                final DataTreeCandidateNode candNode) {
            if (candNode.modificationType() == ModificationType.UNMODIFIED) {
                LOG.debug("Skipping unmodified candidate {}", path);
                return;
            }
            final var regs = regNode.getRegistrations();
            if (!regs.isEmpty()) {
                createCanCommits(regs, path, candNode);
            }

            for (var candChild : candNode.childNodes()) {
                if (candChild.modificationType() != ModificationType.UNMODIFIED) {
                    final var regChild = regNode.getExactChild(candChild.name());
                    if (regChild != null) {
                        lookupAndCreateCanCommits(path.node(candChild.name()), regChild, candChild);
                    }

                    for (var rc : regNode.getInexactChildren(candChild.name())) {
                        lookupAndCreateCanCommits(path.node(candChild.name()), rc, candChild);
                    }
                }
            }
        }

        private void createCanCommits(final Collection<ActorRef> regs, final YangInstanceIdentifier path,
                final DataTreeCandidateNode node) {
            final DOMDataTreeCandidate domCandidate = DOMDataTreeCandidateTO.create(treeIdentifier(path), node);
            for (final ActorRef reg : regs) {
                actorToCandidates.put(reg, domCandidate);
            }
        }

        private static DOMDataTreeIdentifier treeIdentifier(final YangInstanceIdentifier path) {
            return DOMDataTreeIdentifier.of(LogicalDatastoreType.CONFIGURATION, path);
        }

        List<DataTreeCohortActor.CanCommit> perform(final Node<ActorRef> rootNode) {
            final var toLookup = candidate.getRootPath().getPathArguments();
            lookupAndCreateCanCommits(toLookup, 0, rootNode);

            return actorToCandidates.asMap().entrySet().stream()
                .map(entry -> new DataTreeCohortActor.CanCommit(txId, entry.getValue(), schema, entry.getKey()))
                .collect(Collectors.toUnmodifiableList());
        }
    }
}
