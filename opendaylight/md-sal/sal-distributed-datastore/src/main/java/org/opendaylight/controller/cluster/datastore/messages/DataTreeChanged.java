/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.messages;

import static java.util.Objects.requireNonNull;

import java.util.List;
import org.opendaylight.yangtools.yang.data.tree.api.DataTreeCandidate;

/**
 * A message about a DataTree having been changed. The message is not
 * serializable on purpose. For delegating the change across cluster nodes,
 * this needs to be intercepted by a local agent and forwarded as reconstructed
 * candidate.
 */
public final class DataTreeChanged {
    private final List<DataTreeCandidate> changes;

    public DataTreeChanged(final List<DataTreeCandidate> changes) {
        this.changes = requireNonNull(changes);
    }

    /**
     * Return the data changes.
     *
     * @return Change events
     */
    public List<DataTreeCandidate> getChanges() {
        return changes;
    }
}
