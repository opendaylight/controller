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
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.pekko.actor.ActorRef;
import org.apache.pekko.actor.PoisonPill;
import org.apache.pekko.actor.Status;
import org.eclipse.jdt.annotation.NonNull;
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
    private static final Logger LOG = LoggerFactory.getLogger(DataTreeCohortActorRegistry.class);

    private final HashMap<ActorRef, Node<ActorRef>> cohortToNode = new HashMap<>();

    List<ActorRef> getCohortActors() {
        return List.copyOf(cohortToNode.keySet());
    }

    @SuppressWarnings("checkstyle:IllegalCatch")
    void registerCohort(final ActorRef sender, final RegisterCohort cohort) {
        takeLock();
        try {
            final ActorRef cohortRef = cohort.getCohort();
            final Node<ActorRef> node = findNodeFor(cohort.getPath().path().getPathArguments());
            addRegistration(node, cohort.getCohort());
            cohortToNode.put(cohortRef, node);
        } catch (final Exception e) {
            sender.tell(new Status.Failure(e), ActorRef.noSender());
            return;
        } finally {
            releaseLock();
        }
        sender.tell(new Status.Success(null), ActorRef.noSender());
    }

    void removeCommitCohort(final ActorRef sender, final RemoveCohort message) {
        final ActorRef cohort = message.getCohort();
        final Node<ActorRef> node = cohortToNode.get(cohort);
        if (node != null) {
            removeRegistration(node, cohort);
            cohortToNode.remove(cohort);
        }
        sender.tell(new Status.Success(null), ActorRef.noSender());
        cohort.tell(PoisonPill.getInstance(), cohort);
    }

    List<DataTreeCohortActor.CanCommit> createCanCommitMessages(final TransactionIdentifier txId,
            final DataTreeCandidate candidate, final EffectiveModelContext schema) {
        try (var cohorts = takeSnapshot()) {
            return new CanCommitMessageBuilder(txId, candidate, schema).perform(cohorts.getRootNode());
        }
    }

    void process(final ActorRef sender, final @NonNull CohortRegistryCommand message) {
        switch (message) {
            case RegisterCohort register -> registerCohort(sender, register);
            case RemoveCohort remove -> removeCommitCohort(sender, remove);
        }
    }

    abstract static sealed class CohortRegistryCommand {
        private final ActorRef cohort;

        CohortRegistryCommand(final ActorRef cohort) {
            this.cohort = requireNonNull(cohort);
        }

        ActorRef getCohort() {
            return cohort;
        }
    }

    static final class RegisterCohort extends CohortRegistryCommand {
        private final DOMDataTreeIdentifier path;

        RegisterCohort(final DOMDataTreeIdentifier path, final ActorRef cohort) {
            super(cohort);
            this.path = path;
        }

        public DOMDataTreeIdentifier getPath() {
            return path;
        }
    }

    static final class RemoveCohort extends CohortRegistryCommand {
        RemoveCohort(final ActorRef cohort) {
            super(cohort);
        }
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
