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
    public void delete(final PathArgument child) {
        getModifications().addModification(new DeleteModification(next(child)));
    }

    @Override
    public void merge(final PathArgument child, final NormalizedNode<?, ?> data) {
        getModifications().addModification(new MergeModification(next(child), data));
    }

    @Override
    public void write(final PathArgument child, final NormalizedNode<?, ?> data) {
        getModifications().addModification(new WriteModification(next(child), data));
    }
}
