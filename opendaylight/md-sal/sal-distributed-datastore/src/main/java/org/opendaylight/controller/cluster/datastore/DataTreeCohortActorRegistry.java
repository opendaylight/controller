/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.datastore;

import akka.actor.ActorRef;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.concurrent.NotThreadSafe;
import org.opendaylight.controller.cluster.datastore.DataTreeCohortActor.CanCommit;
import org.opendaylight.controller.md.sal.dom.spi.AbstractRegistrationTree;
import org.opendaylight.controller.md.sal.dom.spi.RegistrationTreeNode;
import org.opendaylight.controller.md.sal.dom.spi.RegistrationTreeSnapshot;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.mdsal.dom.api.DOMDataTreeCandidate;
import org.opendaylight.mdsal.dom.api.DOMDataTreeIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.PathArgument;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeCandidateNode;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeCandidateTip;
import org.opendaylight.yangtools.yang.data.api.schema.tree.ModificationType;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@NotThreadSafe
class DataTreeCohortActorRegistry extends AbstractRegistrationTree<ActorRef> {

    private static final Logger LOG = LoggerFactory.getLogger(DataTreeCohortActorRegistry.class);

    private final Map<ActorRef, RegistrationTreeNode<ActorRef>> cohortToNode = new HashMap<>();


    void addCommitCohort(YangInstanceIdentifier path, ActorRef cohort) {
        takeLock();
        try {
            final RegistrationTreeNode<ActorRef> node = findNodeFor(path.getPathArguments());
            addRegistration(node, cohort);
        } finally {
            releaseLock();
        }
    }

    void removeCommitCohort(ActorRef cohort) {
        RegistrationTreeNode<ActorRef> node = cohortToNode.get(cohort);
        if (node != null) {
            removeRegistration(node, cohort);
        }
    }


    Collection<DataTreeCohortActor.CanCommit> createCanCommitMessages(String txId, DataTreeCandidateTip candidate,
            SchemaContext schema) {
        try (RegistrationTreeSnapshot<ActorRef> cohorts = takeSnapshot()) {
            return new CanCommitMessageBuilder(txId, candidate, schema).perform(cohorts.getRootNode());
        }
    }


    private static class CanCommitMessageBuilder {

        private final String txId;
        private final DataTreeCandidateTip candidate;
        private final SchemaContext schema;
        private Collection<DataTreeCohortActor.CanCommit> messages =
                new ArrayList<>();

        CanCommitMessageBuilder(String txId, DataTreeCandidateTip candidate, SchemaContext schema) {
            this.txId = Preconditions.checkNotNull(txId);
            this.candidate = Preconditions.checkNotNull(candidate);
            this.schema = schema;
        }

        private void lookupAndStartCanCommits(List<PathArgument> args, int offset,
                RegistrationTreeNode<ActorRef> node) {

            if (args.size() != offset) {
                final PathArgument arg = args.get(offset);
                final RegistrationTreeNode<ActorRef> exactChild = node.getExactChild(arg);
                if (exactChild != null) {
                    lookupAndStartCanCommits(args, offset + 1, exactChild);
                }
                for (RegistrationTreeNode<ActorRef> c : node.getInexactChildren(arg)) {
                    lookupAndStartCanCommits(args, offset + 1, c);
                }
            } else {
                startCanCommits(candidate.getRootPath(), node, candidate.getRootNode());
            }
        }

        private void startCanCommits(YangInstanceIdentifier path,
 RegistrationTreeNode<ActorRef> regNode,
                DataTreeCandidateNode candNode) {
            if (candNode.getModificationType() == ModificationType.UNMODIFIED) {
                LOG.debug("Skipping unmodified candidate {}", path);
                return;
            }
            final Collection<ActorRef> regs = regNode.getRegistrations();
            if (!regs.isEmpty()) {
                startCanCommits(regs, path, candNode);
            }

            for (DataTreeCandidateNode candChild : candNode.getChildNodes()) {
                if (candChild.getModificationType() != ModificationType.UNMODIFIED) {
                    final RegistrationTreeNode<ActorRef> regChild =
                            regNode.getExactChild(candChild.getIdentifier());
                    if (regChild != null) {
                        startCanCommits(path.node(candChild.getIdentifier()), regChild, candChild);
                    }

                    for (RegistrationTreeNode<ActorRef> rc : regNode
                            .getInexactChildren(candChild.getIdentifier())) {
                        startCanCommits(path.node(candChild.getIdentifier()), rc, candChild);
                    }
                }
            }
        }

        private void startCanCommits(Collection<ActorRef> regs, YangInstanceIdentifier path,
                DataTreeCandidateNode node) {
            DOMDataTreeCandidate candidate = DOMDataTreeCandidateTO.create(treeIdentifier(path), node);
            for (ActorRef reg : regs) {
                CanCommit message = new DataTreeCohortActor.CanCommit(txId, candidate, reg);
                messages.add(message);
            }
        }

        private static DOMDataTreeIdentifier treeIdentifier(YangInstanceIdentifier path) {
            return new DOMDataTreeIdentifier(LogicalDatastoreType.CONFIGURATION, path);
        }

        public Collection<DataTreeCohortActor.CanCommit> perform(RegistrationTreeNode<ActorRef> rootNode) {
            final List<PathArgument> toLookup = ImmutableList.copyOf(candidate.getRootPath().getPathArguments());
            lookupAndStartCanCommits(toLookup, 0, rootNode);
            return ImmutableList.copyOf(messages);
        }
    }

}
