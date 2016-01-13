/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.datastore;

import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.CheckedFuture;
import org.opendaylight.mdsal.common.api.DataValidationFailedException;
import org.opendaylight.mdsal.common.api.PostCanCommitStep;
import org.opendaylight.mdsal.dom.api.DOMDataTreeCandidate;
import org.opendaylight.mdsal.dom.api.DOMDataTreeCommitCohort;
import org.opendaylight.mdsal.dom.api.DOMDataTreeCommitCohortRegistration;
import org.opendaylight.mdsal.dom.api.DOMDataTreeIdentifier;
import org.opendaylight.yangtools.concepts.AbstractObjectRegistration;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeCandidateNode;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;

abstract class CohortRegistration<T extends DOMDataTreeCommitCohort> extends AbstractObjectRegistration<T>
        implements DOMDataTreeCommitCohortRegistration<T> {

    private final DOMDataTreeIdentifier path;

    CohortRegistration(T instance, DOMDataTreeIdentifier path) {
        super(instance);
        this.path = Preconditions.checkNotNull(path);
    }

    CheckedFuture<PostCanCommitStep, DataValidationFailedException> canCommit(Object txId,
            YangInstanceIdentifier path, DataTreeCandidateNode candNode, SchemaContext schema) {
        return getInstance().canCommit(txId, toFrontendCandidate(path, candNode), schema);
    }

    private DOMDataTreeCandidate toFrontendCandidate(YangInstanceIdentifier candidatePath,
            DataTreeCandidateNode candidateNode) {
        return DOMDataTreeCandidateTO.create(new DOMDataTreeIdentifier(path.getDatastoreType(), candidatePath),
                candidateNode);
    }

    DOMDataTreeIdentifier getPath() {
        return path;
    }

}