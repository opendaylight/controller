/*
 * Copyright (c) 2017 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.clustering.it.provider;

import com.google.common.base.Verify;
import com.google.common.util.concurrent.FluentFuture;
import java.util.Collection;
import java.util.Optional;
import org.opendaylight.mdsal.common.api.DataValidationFailedException;
import org.opendaylight.mdsal.common.api.PostCanCommitStep;
import org.opendaylight.mdsal.dom.api.DOMDataTreeCandidate;
import org.opendaylight.mdsal.dom.api.DOMDataTreeCommitCohort;
import org.opendaylight.mdsal.dom.api.DOMDataTreeIdentifier;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.sal.clustering.it.car.rev140818.Cars;
import org.opendaylight.yangtools.util.concurrent.FluentFutures;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.PathArgument;
import org.opendaylight.yangtools.yang.data.api.schema.DataContainerChild;
import org.opendaylight.yangtools.yang.data.api.schema.DataContainerNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeCandidateNode;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Example implementation of a DOMDataTreeCommitCohort that validates car entry data.
 *
 * @author Thomas Pantelis
 */
public class CarEntryDataTreeCommitCohort implements DOMDataTreeCommitCohort {
    private static final Logger LOG = LoggerFactory.getLogger(CarEntryDataTreeCommitCohort.class);

    private static final QName YEAR_QNAME = QName.create(Cars.QNAME, "year").intern();
    private static final NodeIdentifier YEAR_NODE_ID = new NodeIdentifier(YEAR_QNAME);

    @Override
    public FluentFuture<PostCanCommitStep> canCommit(Object txId, SchemaContext ctx,
            Collection<DOMDataTreeCandidate> candidates) {

        for (DOMDataTreeCandidate candidate : candidates) {
            // Simple data validation - verify the year, if present, is >= 1990

            final DataTreeCandidateNode rootNode = candidate.getRootNode();
            final Optional<NormalizedNode> dataAfter = rootNode.getDataAfter();

            LOG.info("In canCommit: modificationType: {}, dataBefore: {}, dataAfter: {}",
                    rootNode.getModificationType(), rootNode.getDataBefore(), dataAfter);

            // Note: we don't want to process DELETE modifications but we don't need to explicitly check the
            // ModificationType because dataAfter will not be present. Also dataAfter *should* always contain a
            // MapEntryNode but we verify anyway.
            if (dataAfter.isPresent()) {
                final NormalizedNode normalizedNode = dataAfter.get();
                Verify.verify(normalizedNode instanceof DataContainerNode,
                        "Expected type DataContainerNode, actual was %s", normalizedNode.getClass());
                DataContainerNode<?> entryNode = (DataContainerNode<?>) normalizedNode;
                final Optional<DataContainerChild<? extends PathArgument, ?>> possibleYear =
                        entryNode.getChild(YEAR_NODE_ID);
                if (possibleYear.isPresent()) {
                    final Number year = (Number) possibleYear.get().getValue();

                    LOG.info("year is {}", year);

                    if (!(year.longValue() >= 1990)) {
                        return FluentFutures.immediateFailedFluentFuture(new DataValidationFailedException(
                                DOMDataTreeIdentifier.class, candidate.getRootPath(),
                                String.format("Invalid year %d - year must be >= 1990", year)));
                    }
                }
            }
        }

        // Return the noop PostCanCommitStep as we're only validating input data and not participating in the
        // remaining 3PC stages (pre-commit and commit).
        return PostCanCommitStep.NOOP_SUCCESSFUL_FUTURE;
    }
}
