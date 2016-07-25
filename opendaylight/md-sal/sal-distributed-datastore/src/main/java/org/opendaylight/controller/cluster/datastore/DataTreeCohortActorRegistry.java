/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.datastore;

import akka.actor.ActorRef;
import akka.actor.PoisonPill;
import akka.actor.Status;
import akka.util.Timeout;
import com.google.common.base.Preconditions;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.concurrent.NotThreadSafe;
import org.opendaylight.controller.cluster.access.concepts.TransactionIdentifier;
import org.opendaylight.controller.cluster.datastore.DataTreeCohortActor.CanCommit;
import org.opendaylight.controller.md.sal.dom.spi.AbstractRegistrationTree;
import org.opendaylight.controller.md.sal.dom.spi.RegistrationTreeNode;
import org.opendaylight.controller.md.sal.dom.spi.RegistrationTreeSnapshot;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.mdsal.dom.api.DOMDataTreeCandidate;
import org.opendaylight.mdsal.dom.api.DOMDataTreeIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.PathArgument;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeCandidate;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeCandidateNode;
import org.opendaylight.yangtools.yang.data.api.schema.tree.ModificationType;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Registry of user commit cohorts, which is responsible for handling registration and calculation
 * of affected cohorts based on {@link DataTreeCandidate}.
 *
 */
@NotThreadSafe
class DataTreeCohortActorRegistry extends AbstractRegistrationTree<ActorRef> {

    private static final Logger LOG = LoggerFactory.getLogger(DataTreeCohortActorRegistry.class);

    private final Map<ActorRef, RegistrationTreeNode<ActorRef>> cohortToNode = new HashMap<>();


    void registerCohort(final ActorRef sender, final RegisterCohort cohort) {
        takeLock();
        try {
            final ActorRef cohortRef = cohort.getCohort();
            final RegistrationTreeNode<ActorRef> node =
                    findNodeFor(cohort.getPath().getRootIdentifier().getPathArguments());
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
        final RegistrationTreeNode<ActorRef> node = cohortToNode.get(cohort);
        if (node != null) {
            removeRegistration(node, cohort);
            cohortToNode.remove(cohort);
        }
        sender.tell(new Status.Success(null), ActorRef.noSender());
        cohort.tell(PoisonPill.getInstance(), cohort);
    }

    Collection<DataTreeCohortActor.CanCommit> createCanCommitMessages(final TransactionIdentifier txId,
            final DataTreeCandidate candidate, final SchemaContext schema) {
        try (RegistrationTreeSnapshot<ActorRef> cohorts = takeSnapshot()) {
            return new CanCommitMessageBuilder(txId, candidate, schema).perform(cohorts.getRootNode());
        }
    }

    void process(final ActorRef sender, final CohortRegistryCommand message) {
        if (message instanceof RegisterCohort) {
            registerCohort(sender, (RegisterCohort) message);
        } else if (message instanceof RemoveCohort) {
            removeCommitCohort(sender, (RemoveCohort) message);
        }
    }

    static abstract class CohortRegistryCommand {

        private final ActorRef cohort;

        CohortRegistryCommand(final ActorRef cohort) {
            this.cohort = Preconditions.checkNotNull(cohort);
        }

        ActorRef getCohort() {
            return cohort;
        }
    }

    static class RegisterCohort extends CohortRegistryCommand {

        private final DOMDataTreeIdentifier path;

        RegisterCohort(final DOMDataTreeIdentifier path, final ActorRef cohort) {
            super(cohort);
            this.path = path;

        }

        public DOMDataTreeIdentifier getPath() {
            return path;
        }

    }

    static class RemoveCohort extends CohortRegistryCommand {

        RemoveCohort(final ActorRef cohort) {
            super(cohort);
        }

    }

    private static class CanCommitMessageBuilder {

        private final TransactionIdentifier txId;
        private final DataTreeCandidate candidate;
        private final SchemaContext schema;
        private final Collection<DataTreeCohortActor.CanCommit> messages =
                new ArrayList<>();

        CanCommitMessageBuilder(final TransactionIdentifier txId, final DataTreeCandidate candidate, final SchemaContext schema) {
            this.txId = Preconditions.checkNotNull(txId);
            this.candidate = Preconditions.checkNotNull(candidate);
            this.schema = schema;
        }

        private void lookupAndCreateCanCommits(final List<PathArgument> args, final int offset,
                final RegistrationTreeNode<ActorRef> node) {

            if (args.size() != offset) {
                final PathArgument arg = args.get(offset);
                final RegistrationTreeNode<ActorRef> exactChild = node.getExactChild(arg);
                if (exactChild != null) {
                    lookupAndCreateCanCommits(args, offset + 1, exactChild);
                }
                for (final RegistrationTreeNode<ActorRef> c : node.getInexactChildren(arg)) {
                    lookupAndCreateCanCommits(args, offset + 1, c);
                }
            } else {
                lookupAndCreateCanCommits(candidate.getRootPath(), node, candidate.getRootNode());
            }
        }

        private void lookupAndCreateCanCommits(final YangInstanceIdentifier path, final RegistrationTreeNode<ActorRef> regNode,
                final DataTreeCandidateNode candNode) {
            if (candNode.getModificationType() == ModificationType.UNMODIFIED) {
                LOG.debug("Skipping unmodified candidate {}", path);
                return;
            }
            final Collection<ActorRef> regs = regNode.getRegistrations();
            if (!regs.isEmpty()) {
                createCanCommits(regs, path, candNode);
            }

            for (final DataTreeCandidateNode candChild : candNode.getChildNodes()) {
                if (candChild.getModificationType() != ModificationType.UNMODIFIED) {
                    final RegistrationTreeNode<ActorRef> regChild =
                            regNode.getExactChild(candChild.getIdentifier());
                    if (regChild != null) {
                        lookupAndCreateCanCommits(path.node(candChild.getIdentifier()), regChild, candChild);
                    }

                    for (final RegistrationTreeNode<ActorRef> rc : regNode
                            .getInexactChildren(candChild.getIdentifier())) {
                        lookupAndCreateCanCommits(path.node(candChild.getIdentifier()), rc, candChild);
                    }
                }
            }
        }

        private void createCanCommits(final Collection<ActorRef> regs, final YangInstanceIdentifier path,
                final DataTreeCandidateNode node) {
            final DOMDataTreeCandidate candidate = DOMDataTreeCandidateTO.create(treeIdentifier(path), node);
            for (final ActorRef reg : regs) {
                final CanCommit message = new DataTreeCohortActor.CanCommit(txId, candidate, schema, reg);
                messages.add(message);
            }
        }

        private static DOMDataTreeIdentifier treeIdentifier(final YangInstanceIdentifier path) {
            return new DOMDataTreeIdentifier(LogicalDatastoreType.CONFIGURATION, path);
        }

        private Collection<DataTreeCohortActor.CanCommit> perform(final RegistrationTreeNode<ActorRef> rootNode) {
            final List<PathArgument> toLookup = candidate.getRootPath().getPathArguments();
            lookupAndCreateCanCommits(toLookup, 0, rootNode);
            return messages;
        }
    }

    CompositeDataTreeCohort createCohort(final SchemaContext schemaContext, final TransactionIdentifier txId,
            final Timeout commitStepTimeout) {
        return new CompositeDataTreeCohort(this, txId, schemaContext, commitStepTimeout);
    }
}
