/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.datastore;

import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.CheckedFuture;
import com.google.common.util.concurrent.ListenableFuture;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.opendaylight.controller.md.sal.dom.spi.AbstractRegistrationTree;
import org.opendaylight.controller.md.sal.dom.spi.RegistrationTreeNode;
import org.opendaylight.controller.md.sal.dom.spi.RegistrationTreeSnapshot;
import org.opendaylight.mdsal.common.api.DataValidationFailedException;
import org.opendaylight.mdsal.common.api.PostCanCommitStep;
import org.opendaylight.mdsal.dom.api.DOMDataTreeCommitCohort;
import org.opendaylight.mdsal.dom.api.DOMDataTreeCommitCohortRegistration;
import org.opendaylight.mdsal.dom.api.DOMDataTreeCommitCohortRegistry;
import org.opendaylight.mdsal.dom.api.DOMDataTreeIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.PathArgument;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeCandidateNode;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeCandidateTip;
import org.opendaylight.yangtools.yang.data.api.schema.tree.ModificationType;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class ShardDataTreeCohortRegistry extends AbstractRegistrationTree<CohortRegistration<?>>
        implements DOMDataTreeCommitCohortRegistry {

    private static final Logger LOG = LoggerFactory.getLogger(ShardDataTreeCohortRegistry.class);

    @Override
    public <T extends DOMDataTreeCommitCohort> DOMDataTreeCommitCohortRegistration<T> registerCommitCohort(
            DOMDataTreeIdentifier path, T cohort) {
        takeLock();
        try {
            final RegistrationTreeNode<CohortRegistration<?>> node =
                    findNodeFor(path.getRootIdentifier().getPathArguments());
            CohortRegistration<T> reg = new CohortRegistration<T>(cohort, path) {

                @Override
                protected void removeRegistration() {
                    ShardDataTreeCohortRegistry.this.removeRegistration(node, this);
                }
            };
            addRegistration(node, reg);
            return reg;
        } finally {
            releaseLock();
        }
    }


    ListenableFuture<ResultCollection<PostCanCommitStep>> canCommit(Object txId, DataTreeCandidateTip candidate,
            SchemaContext schema) {
        try (RegistrationTreeSnapshot<CohortRegistration<?>> cohorts = takeSnapshot()) {
            return new CanCommitCohortBuilder(txId, candidate, schema).perform(cohorts.getRootNode());
        }
    }


    private static class CanCommitCohortBuilder {

        private final Object txId;
        private final DataTreeCandidateTip candidate;
        private final SchemaContext schema;
        private Collection<CheckedFuture<PostCanCommitStep, DataValidationFailedException>> postCanCommits =
                new ArrayList<>();

        CanCommitCohortBuilder(Object txId, DataTreeCandidateTip candidate, SchemaContext schema) {
            this.txId = txId;
            this.candidate = candidate;
            this.schema = schema;
        }

        private void lookupAndStartCanCommits(List<PathArgument> args, int offset,
                RegistrationTreeNode<CohortRegistration<?>> node) {

            if (args.size() != offset) {
                final PathArgument arg = args.get(offset);
                final RegistrationTreeNode<CohortRegistration<?>> exactChild = node.getExactChild(arg);
                if (exactChild != null) {
                    lookupAndStartCanCommits(args, offset + 1, exactChild);
                }
                for (RegistrationTreeNode<CohortRegistration<?>> c : node.getInexactChildren(arg)) {
                    lookupAndStartCanCommits(args, offset + 1, c);
                }
            } else {
                startCanCommits(candidate.getRootPath(), node, candidate.getRootNode());
            }
        }

        private void startCanCommits(YangInstanceIdentifier path,
 RegistrationTreeNode<CohortRegistration<?>> regNode,
                DataTreeCandidateNode candNode) {
            if (candNode.getModificationType() == ModificationType.UNMODIFIED) {
                LOG.debug("Skipping unmodified candidate {}", path);
                return;
            }
            final Collection<CohortRegistration<?>> regs = regNode.getRegistrations();
            if (!regs.isEmpty()) {
                startCanCommits(regs, path, candNode);
            }

            for (DataTreeCandidateNode candChild : candNode.getChildNodes()) {
                if (candChild.getModificationType() != ModificationType.UNMODIFIED) {
                    final RegistrationTreeNode<CohortRegistration<?>> regChild =
                            regNode.getExactChild(candChild.getIdentifier());
                    if (regChild != null) {
                        startCanCommits(path.node(candChild.getIdentifier()), regChild, candChild);
                    }

                    for (RegistrationTreeNode<CohortRegistration<?>> rc : regNode
                            .getInexactChildren(candChild.getIdentifier())) {
                        startCanCommits(path.node(candChild.getIdentifier()), rc, candChild);
                    }
                }
            }
        }

        private void startCanCommits(Collection<CohortRegistration<?>> regs, YangInstanceIdentifier path,
                DataTreeCandidateNode candNode) {
            for (CohortRegistration<?> reg : regs) {
                LOG.debug("Starting Invoking canCommit() on {} for transaction {}", reg.getInstance(), txId);
                CheckedFuture<PostCanCommitStep, DataValidationFailedException> nextStep =
                        reg.canCommit(txId, path, candNode, schema);
                if (!PostCanCommitStep.NOOP_SUCCESS_FUTURE.equals(nextStep)) {
                    // record can commit.
                    postCanCommits.add(nextStep);
                }
            }
        }

        public ListenableFuture<ResultCollection<PostCanCommitStep>> perform(
                RegistrationTreeNode<CohortRegistration<?>> rootNode) {
            final List<PathArgument> toLookup = ImmutableList.copyOf(candidate.getRootPath().getPathArguments());
            lookupAndStartCanCommits(toLookup, 0, rootNode);
            return ResultCollection.fromFutures(postCanCommits);
        }
    }

}
