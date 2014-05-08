/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.datastore.modification;

import org.opendaylight.controller.sal.core.spi.data.DOMStoreWriteTransaction;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * MutableCompositeModification is just a mutable version of a
 * CompositeModification {@link org.opendaylight.controller.cluster.datastore.modification.MutableCompositeModification#addModification(Modification)}
 */
public class MutableCompositeModification
    implements CompositeModification, Serializable {

    private static final long serialVersionUID = 1163377899140186790L;

    private final List<Modification> modifications = new ArrayList<>();

    @Override
    public void apply(DOMStoreWriteTransaction transaction) {
        for (Modification modification : modifications) {
            modification.apply(transaction);
        }
    }

    /**
     * Add a new Modification to the list of Modifications represented by this
     * composite
     *
     * @param modification
     */
    public void addModification(Modification modification) {
        modifications.add(modification);
    }

    public List<Modification> getModifications() {
        return Collections.unmodifiableList(modifications);
    }
}
