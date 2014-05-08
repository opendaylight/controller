/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.datastore.modification;

import java.util.List;

/**
 * CompositeModification contains a list of modifications that need to be applied to the DOMStore
 * <p>
 * A CompositeModification gets stored in the transaction log for a Shard. During recovery when the transaction log
 * is being replayed a DOMStoreWriteTransaction could be created and a CompositeModification could be applied to it.
 * </p>
 */
public interface CompositeModification extends Modification {
    /**
     * Get a list of Modifications contained by this Composite
     * @return
     */
    List<Modification> getModifications();
}
