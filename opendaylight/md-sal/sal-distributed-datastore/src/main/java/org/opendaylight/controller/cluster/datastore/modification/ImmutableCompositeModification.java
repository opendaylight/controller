/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.datastore.modification;

import org.opendaylight.controller.protobuff.messages.persistent.PersistentMessages;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreWriteTransaction;

import java.util.List;

public class ImmutableCompositeModification implements CompositeModification {

    private final CompositeModification modification;

    public ImmutableCompositeModification(CompositeModification modification) {
        this.modification = modification;
    }

    @Override
    public List<Modification> getModifications() {
        return modification.getModifications();
    }

    @Override
    public void apply(DOMStoreWriteTransaction transaction) {
        modification.apply(transaction);
    }

    @Override public Object toSerializable() {

        PersistentMessages.CompositeModification.Builder builder =
            PersistentMessages.CompositeModification.newBuilder();

        for (Modification m : modification.getModifications()) {
            builder.addModification(
                (PersistentMessages.Modification) m.toSerializable());
        }

        return builder.build();
    }
}
