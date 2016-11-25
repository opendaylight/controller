/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.datastore.modification;

import java.io.Externalizable;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreWriteTransaction;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeModification;

/**
 * Represents a modification to the data store.
 *
 * <p>
 * Simple modifications can be of type,
 * <ul>
 * <li> {@link org.opendaylight.controller.cluster.datastore.modification.WriteModification}
 * <li> {@link org.opendaylight.controller.cluster.datastore.modification.MergeModification}
 * <li> {@link org.opendaylight.controller.cluster.datastore.modification.DeleteModification}
 * </ul>
 *
 * <p>
 * Modifications can in turn be lumped into a single
 * {@link org.opendaylight.controller.cluster.datastore.modification.CompositeModification}
 * which can then be applied to a write transaction.
 */
public interface Modification extends Externalizable {

    byte COMPOSITE = 1;
    byte WRITE = 2;
    byte MERGE = 3;
    byte DELETE = 4;

    /**
     * Apply the modification to the specified transaction.
     *
     * @param transaction the transaction
     */
    void apply(DOMStoreWriteTransaction transaction);

    /**
     * Apply the modification to the specified transaction.
     *
     * @param transaction the transaction
     */
    void apply(DataTreeModification transaction);

    byte getType();
}
