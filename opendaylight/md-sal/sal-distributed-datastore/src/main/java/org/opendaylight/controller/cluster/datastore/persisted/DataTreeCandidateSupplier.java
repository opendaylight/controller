/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.persisted;

import com.google.common.annotations.Beta;
import java.io.IOException;
import java.util.Map.Entry;
import java.util.Optional;
import org.opendaylight.controller.cluster.access.concepts.TransactionIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeCandidate;

/**
 * Interim interface for consolidating DataTreeCandidatePayload and {@link CommitTransactionPayload}.
 *
 * @author Robert Varga
 */
@Beta
public interface DataTreeCandidateSupplier {
    Entry<Optional<TransactionIdentifier>, DataTreeCandidate> getCandidate() throws IOException;
}
