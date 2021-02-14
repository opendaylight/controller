/*
 * Copyright (c) 2015 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.utils;

import org.opendaylight.controller.cluster.datastore.messages.BatchedModifications;
import org.opendaylight.controller.cluster.datastore.modification.DeleteModification;
import org.opendaylight.controller.cluster.datastore.modification.MergeModification;
import org.opendaylight.controller.cluster.datastore.modification.WriteModification;
import org.opendaylight.controller.cluster.datastore.util.AbstractDataTreeModificationCursor;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.PathArgument;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;

/**
 * Base class for a DataTreeModificationCursor that publishes to BatchedModifications instance(s).
 *
 * @author Thomas Pantelis
 */
public abstract class AbstractBatchedModificationsCursor extends AbstractDataTreeModificationCursor {
    protected abstract BatchedModifications getModifications();

    @Override
    public final void delete(final PathArgument child) {
        getModifications().addModification(new DeleteModification(current().node(child)));
    }

    @Override
    public final void merge(final PathArgument child, final NormalizedNode data) {
        getModifications().addModification(new MergeModification(current().node(child), data));
    }

    @Override
    public final void write(final PathArgument child, final NormalizedNode data) {
        getModifications().addModification(new WriteModification(current().node(child), data));
    }
}
