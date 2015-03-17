/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.messages;

import com.google.common.base.Preconditions;
import java.util.Collection;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeCandidate;

/**
 * A message about a DataTree having been changed. The message is not
 * serializable on purpose. For delegating the change across cluster nodes,
 * this needs to be intercepted by a local agent and forwarded as
 * a {@link DataTreeDelta}.
 */
public final class DataTreeChanged {
    private final Collection<DataTreeCandidate> changes;

    public DataTreeChanged(final Collection<DataTreeCandidate> changes) {
        this.changes = Preconditions.checkNotNull(changes);
    }

    /**
     * Return the data changes.
     *
     * @return Change events
     */
    public Collection<DataTreeCandidate> getChanges() {
        return changes;
    }
}
